package oly.netpowerctrl.application_state;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.alarms.TimerController;
import oly.netpowerctrl.anel.AnelPlugin;
import oly.netpowerctrl.devices.DeviceCollection;
import oly.netpowerctrl.devices.DeviceInfo;
import oly.netpowerctrl.devices.DevicePort;
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.network.DeviceObserverFinishedResult;
import oly.netpowerctrl.network.DeviceObserverResult;
import oly.netpowerctrl.network.DeviceQuery;
import oly.netpowerctrl.network.UDPSending;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.Logging;
import oly.netpowerctrl.utils.ShowToast;

/**
 * Look for and load plugins. After network change: Rescan for reachable devices.
 */
public class NetpowerctrlService extends Service {
    private static final String LOGNAME = "Plugins";
    private static final String PLUGIN_RESPONSE_ACTION = "oly.netpowerctrl.plugins.PLUGIN_RESPONSE_ACTION";
    private static final String PLUGIN_QUERY_ACTION = "oly.netpowerctrl.plugins.action.QUERY_CONDITION";
    private static final String PAYLOAD_SERVICENAME = "SERVICENAME";
    private static final String PAYLOAD_PACKAGENAME = "PACKAGENAME";
    private static final String PAYLOAD_LOCALIZED_NAME = "LOCALIZED_NAME";
    private static final String RESULT_CODE = "RESULT_CODE";
    private static final int INITIAL_VALUES = 1337;
    private final BroadcastReceiver pluginBroadcastListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctxt, Intent i) {
            if (i.getIntExtra(RESULT_CODE, -1) == INITIAL_VALUES)
                createRemotePlugin(i.getStringExtra(PAYLOAD_SERVICENAME),
                        i.getStringExtra(PAYLOAD_LOCALIZED_NAME), i.getStringExtra(PAYLOAD_PACKAGENAME));
            else
                Log.w(LOGNAME, i.getStringExtra(PAYLOAD_LOCALIZED_NAME) + "failed");
        }
    };
    static private final ArrayList<ServiceReady> observersServiceReady = new ArrayList<>();
    static private final ArrayList<RefreshStartedStopped> observersStartStopRefresh = new ArrayList<>();
    private static final Handler stopServiceHandler = new Handler();
    ///////////////// Service start/stop listener /////////////////
    static private int mDiscoverServiceRefCount = 0;
    static private NetpowerctrlService mDiscoverService;
    static private boolean mWaitForService;

    private static final Runnable stopRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (mDiscoverService != null)
                    mDiscoverService.stopSelf();
                mWaitForService = false;
            } catch (IllegalArgumentException ignored) {
            }

            if (SharedPrefs.notifyOnStop()) {
                ShowToast.FromOtherThread(NetpowerctrlApplication.instance, R.string.service_stopped);
            }
        }
    };

    private final BroadcastReceiver networkChangedListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            @SuppressWarnings("ConstantConditions")
            ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected()) {
                if (SharedPrefs.notifyOnStop()) {
                    ShowToast.FromOtherThread(NetpowerctrlService.this, getString(R.string.network_restarted));
                }
                if (SharedPrefs.logEnergySaveMode())
                    Logging.appendLog("Energiesparen aus: Netzwechsel erkannt");
                start(true, false);
            } else {
                if (SharedPrefs.logEnergySaveMode())
                    Logging.appendLog("Energiesparen an: Kein Netzwerk");
                finish();
                if (SharedPrefs.notifyOnStop()) {
                    ShowToast.FromOtherThread(NetpowerctrlService.this, getString(R.string.network_unreachable));
                }
            }
        }
    };
    private final List<PluginInterface> plugins = new ArrayList<>();
    private boolean isBroadcastListener = false;
    private UDPSending udpSending;
    /**
     * If the listen and send thread are shutdown because the devices destination networks are
     * not in range, this variable is set to true.
     */
    private boolean isNetworkReducedMode;
    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            if (SharedPrefs.isPreferenceNameLogEnergySaveMode(s) && isNetworkReducedMode) {
                if (SharedPrefs.logEnergySaveMode())
                    Logging.appendLog("Energiesparen abgeschaltet");
                start(true, false);
                if (SharedPrefs.notifyOnStop()) {
                    ShowToast.FromOtherThread(NetpowerctrlService.this, getString(R.string.network_restarted));
                }
            }
        }
    };
    /**
     * Will be set if no one of the network DeviceInfos is reachable at the moment.
     */

    private boolean isNetworkChangedListener = false;
    /**
     * Detect new devices and check reach-ability of configured devices.
     */
    private boolean isDetecting = false;

    public static boolean isWirelessLanConnected() {
        @SuppressWarnings("ConstantConditions")
        WifiManager cm = (WifiManager) NetpowerctrlApplication.instance.getSystemService(Context.WIFI_SERVICE);
        return cm.isWifiEnabled() && cm.getConnectionInfo() != null;
    }

    static public boolean isServiceReady() {
        return (mDiscoverService != null);
    }

    @SuppressWarnings("unused")
    static public void registerServiceReadyObserver(ServiceReady o) {
        if (!observersServiceReady.contains(o)) {
            observersServiceReady.add(o);
            if (mDiscoverService != null)
                o.onServiceReady(mDiscoverService);
        }
    }

    @SuppressWarnings("unused")
    static public void unregisterServiceReadyObserver(ServiceReady o) {
        observersServiceReady.remove(o);
    }

    static private void notifyServiceReady(NetpowerctrlService service) {
        Iterator<ServiceReady> it = observersServiceReady.iterator();
        while (it.hasNext()) {
            // If onServiceReady return false: remove listener (one-time listener)
            if (!it.next().onServiceReady(service))
                it.remove();
        }
    }

    static private void notifyServiceFinished() {
        for (ServiceReady anObserversServiceReady : observersServiceReady) {
            anObserversServiceReady.onServiceFinished();
        }
    }

    @SuppressWarnings("unused")
    static public void registerRefreshStartedStopped(RefreshStartedStopped o) {
        if (!observersStartStopRefresh.contains(o)) {
            observersStartStopRefresh.add(o);
        }
    }

    @SuppressWarnings("unused")
    static public void unregisterRefreshStartedStopped(RefreshStartedStopped o) {
        observersStartStopRefresh.remove(o);
    }

    static private void notifyRefreshState(boolean isRefreshing) {
        for (RefreshStartedStopped anObserversStartStopRefresh : observersStartStopRefresh) {
            anObserversStartStopRefresh.onRefreshStateChanged(isRefreshing);
        }
    }

    /**
     * Call this in onResume if you need any of the service functionality.
     *
     * @param refreshDevices   Refresh devices immediately or after the service
     *                         is ready if it is not started yet.
     * @param showNotification
     */
    public static void useService(boolean refreshDevices, boolean showNotification) {
        ++mDiscoverServiceRefCount;
        // Stop delayed stop-service
        stopServiceHandler.removeCallbacks(stopRunnable);
        // Service is not running anymore, restart it
        if (mDiscoverService == null) {
            if (mWaitForService)
                return;
            mWaitForService = true;
            Context context = NetpowerctrlApplication.instance;
            // start service
            Intent intent = new Intent(context, NetpowerctrlService.class);
            intent.putExtra("refreshDevices", refreshDevices);
            intent.putExtra("showNotification", showNotification);
            context.startService(intent);
        } else if (refreshDevices) // service already running. refresh devices?
            mDiscoverService.findDevices(showNotification, null);
    }

    public static void stopUseService() {
        if (mDiscoverServiceRefCount > 0) {
            mDiscoverServiceRefCount--;
        }
        if (mDiscoverServiceRefCount == 0) {
            stopServiceHandler.postDelayed(stopRunnable, 2000);
        }
    }

    public static NetpowerctrlService getService() {
        return mDiscoverService;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (isNetworkChangedListener) {
            isNetworkChangedListener = false;
            unregisterReceiver(networkChangedListener);
        }
        if (SharedPrefs.logEnergySaveMode())
            Logging.appendLog("ENDE: Hintergrunddienste aus");
        finish();

        notifyServiceFinished();
        mDiscoverServiceRefCount = 0;
        mDiscoverService = null;
        mWaitForService = false;

        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mDiscoverService != null)
            return super.onStartCommand(intent, flags, startId);

        if (mDiscoverServiceRefCount == 0)
            mDiscoverServiceRefCount = 1;
        mWaitForService = false;
        mDiscoverService = this;

        Bundle b = intent.getExtras();

        // Service start code
        mDiscoverService.start(b.getBoolean("refreshDevices"), b.getBoolean("showNotification"));

        return super.onStartCommand(intent, flags, startId);
    }

    ///////////////// Service start/stop /////////////////
    private void finish() {
        // Clear all except the anel support
        PluginInterface first = plugins.get(0);
        plugins.clear();
        plugins.add(first);
        ((AnelPlugin) first).stopDiscoveryThreads(this);

        isNetworkReducedMode = true;
        // Stop send and listen threads
        boolean running = udpSending != null && udpSending.isRunning();
        if (running) {
            udpSending.interrupt();
            udpSending = null;
        }

        // Unregister plugin receiver
        if (isBroadcastListener) {
            isBroadcastListener = false;
            try {
                unregisterReceiver(pluginBroadcastListener);
            } catch (IllegalArgumentException ignored) {
            }
        }

        // Stop listening for network changes
        if (isNetworkChangedListener && !SharedPrefs.isWakeUpFromEnergySaving()) {
            if (SharedPrefs.logEnergySaveMode())
                Logging.appendLog("Netzwerkwechsel nicht mehr überwacht. Manuelle Suche erforderlich.");
            isNetworkChangedListener = false;
            unregisterReceiver(networkChangedListener);
        }
    }

    public void start(boolean refreshDevices, boolean showNotification) {
        if (SharedPrefs.logEnergySaveMode())
            Logging.appendLog("Hintergrunddienst gestartet");

        if (plugins.size() == 0) {
            plugins.add(new AnelPlugin());
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            sp.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        }

        if (!isNetworkChangedListener && SharedPrefs.isEnergySavingEnabled()) {
            if (SharedPrefs.logEnergySaveMode())
                Logging.appendLog("Netzwerkwechsel überwacht");
            isNetworkChangedListener = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            registerReceiver(networkChangedListener, filter);
        }

        // Start anel listener
        ((AnelPlugin) plugins.get(0)).startDiscoveryThreads(0);

        isNetworkReducedMode = false;

        // Start send thread
        if (udpSending == null)
            udpSending = new UDPSending(false);
        boolean alreadyRunning = udpSending.isRunning();
        if (!alreadyRunning) {
            udpSending.start();
        }

        // refresh devices after service start
        if (refreshDevices)
            findDevices(showNotification, null);

        // Notify all observers that we are ready
        notifyServiceReady(mDiscoverService);
    }

    private void createRemotePlugin(String serviceName,
                                    String localized_name, String packageName) {

        if (serviceName == null || serviceName.isEmpty() || packageName == null || packageName.isEmpty())
            return;

        /**
         * We received a message from a plugin, we already know: ignore
         */
        for (PluginInterface pi : plugins) {
            if (pi instanceof PluginRemote && ((PluginRemote) pi).serviceName.equals(serviceName)) {
                return;
            }
        }

        PluginRemote plugin = PluginRemote.create(serviceName, localized_name, packageName);

        if (plugin == null) {
            return;
        }

        plugins.add(plugin);
    }

    private void discover() {
        if (!SharedPrefs.getLoadExtensions())
            return;

        if (!isBroadcastListener) {
            isBroadcastListener = true;
            NetpowerctrlApplication.instance.registerReceiver(pluginBroadcastListener,
                    new IntentFilter(PLUGIN_RESPONSE_ACTION));
        }

        Intent i = new Intent(PLUGIN_QUERY_ACTION);
        i.putExtra(PAYLOAD_SERVICENAME, MainActivity.class.getCanonicalName());
        NetpowerctrlApplication.instance.sendBroadcast(i);
    }

    /**
     * Request alarms from plugins again.
     *
     * @param alarms Prefer entries of this alarm list to be fetched first
     */
    public boolean requestAllAlarms(HashSet<UUID> alarms, TimerController timerController) {
        if (isNetworkReducedMode)
            return false;

        List<DevicePort> alarm_ports = new ArrayList<>();

        DeviceCollection c = NetpowerctrlApplication.getDataController().deviceCollection;
        // Put all ports of all devices into the list alarm_ports.
        // If a port is referenced by the alarms hashSet, it will be put in front of the list
        // to refresh that port first.
        for (DeviceInfo di : c.devices) {
            // Request all alarms may be called before all plugins responded
            PluginInterface i = di.getPluginInterface(this);
            if (i == null || !di.enabled)
                continue;

            // Request alarms for every port
            di.lockDevicePorts();
            Iterator<DevicePort> it = di.getDevicePortIterator();
            while (it.hasNext()) {
                final DevicePort port = it.next();
                if (port.Disabled)
                    continue;

                if (alarms.contains(port.uuid))
                    alarm_ports.add(0, port); // add in front of all alarms
                else
                    alarm_ports.add(port);
            }
            di.releaseDevicePorts();
        }

        for (DevicePort port : alarm_ports) {
            PluginInterface i = port.device.getPluginInterface(this);
            i.requestAlarms(port, timerController);
        }

        return true;
    }

    public void remove(PluginRemote plugin) {
        plugins.remove(plugin);
    }

    public void sendBroadcastQuery() {
        for (PluginInterface pi : plugins) {
            pi.requestData();
        }

        if (!SharedPrefs.getLoadExtensions())
            return;

        discover();
    }

    public PluginInterface getPluginInterface(DeviceInfo deviceInfo) {
        for (PluginInterface pi : plugins) {
            if (pi.getPluginID().equals(deviceInfo.pluginID)) {
                return pi;
            }
        }
        return null;
    }

    public void findDevices(final boolean showNotification, final DeviceObserverFinishedResult callback) {
        if (isNetworkReducedMode) {
            if (SharedPrefs.notifyOnStop())
                ShowToast.FromOtherThread(NetpowerctrlService.this, getString(R.string.network_restarted));
            if (SharedPrefs.logEnergySaveMode())
                Logging.appendLog("Energiesparen aus: Suche Geräte");
            // Restart all listener services and try again
            Handler h = new Handler();
            h.post(new Runnable() {
                @Override
                public void run() {
                    start(true, showNotification);
                }
            });
            return;
        }
        // The following mechanism allows only one update request within a
        // 1sec timeframe.
        if (isDetecting)
            return;
        isDetecting = true;
        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                isDetecting = false;
            }
        }, 1000);

        notifyRefreshState(true);

        // First try a broadcast
        NetpowerctrlApplication.getDataController().clearNewDevices();
        new DeviceQuery(new DeviceObserverResult() {
            @Override
            public void onDeviceUpdated(DeviceInfo di) {
            }

            @Override
            public void onObserverJobFinished(List<DeviceInfo> timeout_devices) {
                RuntimeDataController c = NetpowerctrlApplication.getDataController();
                c.notifyStateQueryFinished();
                if (callback != null)
                    callback.onObserverJobFinished(timeout_devices);

                notifyRefreshState(false);

                if (showNotification) {
                    //noinspection ConstantConditions
                    Toast.makeText(NetpowerctrlApplication.instance,
                            NetpowerctrlApplication.instance.getString(R.string.devices_refreshed,
                                    NetpowerctrlApplication.getDataController().getReachableConfiguredDevices(),
                                    NetpowerctrlApplication.getDataController().newDevices.size()),
                            Toast.LENGTH_SHORT
                    ).show();
                }

                if (timeout_devices.size() == 0)
                    return;

                // Do we need to go into network reduced mode?
                if (timeout_devices.size() == c.countNetworkDevices(NetpowerctrlService.this)) {
                    if (SharedPrefs.logEnergySaveMode())
                        Logging.appendLog("Energiesparen an: Keine Geräte gefunden");
                    finish();
                    if (SharedPrefs.notifyOnStop()) {
                        ShowToast.FromOtherThread(NetpowerctrlService.this, getString(R.string.network_no_devices));
                    }
                }
            }
        });
    }

    public UDPSending getUDPSending() {
        return udpSending;
    }


    public class LocalBinder extends Binder {
        public NetpowerctrlService getService() {
            // Return this instance of LocalService so clients can call public methods
            return NetpowerctrlService.this;
        }
    }
}
