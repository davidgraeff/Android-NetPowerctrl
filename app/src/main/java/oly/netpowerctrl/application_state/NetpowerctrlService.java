package oly.netpowerctrl.application_state;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anel.AnelPlugin;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.network.DeviceObserverFinishedResult;
import oly.netpowerctrl.network.DeviceObserverResult;
import oly.netpowerctrl.network.DeviceQuery;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.Logging;
import oly.netpowerctrl.utils.ShowToast;

/**
 * Look for and load plugins. After network change: Rescan for reachable devices.
 */
public class NetpowerctrlService extends Service {
    private static final String TAG = "NetpowerctrlService";
    private static final String PLUGIN_RESPONSE_ACTION = "oly.netpowerctrl.plugins.PLUGIN_RESPONSE_ACTION";
    private static final String PLUGIN_QUERY_ACTION = "oly.netpowerctrl.plugins.action.QUERY_CONDITION";
    private static final String PAYLOAD_SERVICENAME = "SERVICENAME";
    private static final String PAYLOAD_PACKAGENAME = "PACKAGENAME";
    private static final String PAYLOAD_LOCALIZED_NAME = "LOCALIZED_NAME";
    private static final String RESULT_CODE = "RESULT_CODE";
    private static final int INITIAL_VALUES = 1337;
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
        }
    };
    private final BroadcastReceiver extensionsListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ignored, Intent i) {
            extensionDiscovered(i);
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
                enterFullNetworkMode(true, false);
            } else {
                if (SharedPrefs.logEnergySaveMode())
                    Logging.appendLog("Energiesparen an: Kein Netzwerk");
                if (SharedPrefs.notifyOnStop()) {
                    ShowToast.FromOtherThread(NetpowerctrlService.this, getString(R.string.network_unreachable));
                }
                enterNetworkReducedMode();
            }
        }
    };
    private final List<PluginInterface> plugins = new ArrayList<>();
    private boolean isExtensionsListener = false;
    /**
     * If the listen and send thread are shutdown because the devices destination networks are
     * not in range, this variable is set to true.
     */
    private boolean mNetworkReducedMode;
    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            if (SharedPrefs.isPreferenceNameLogEnergySaveMode(s) && mNetworkReducedMode) {
                if (SharedPrefs.logEnergySaveMode())
                    Logging.appendLog("Energiesparen abgeschaltet");
                if (SharedPrefs.notifyOnStop()) {
                    ShowToast.FromOtherThread(NetpowerctrlService.this, getString(R.string.network_restarted));
                }
                enterFullNetworkMode(true, false);
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
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mDiscoverService != null)
            return super.onStartCommand(intent, flags, startId);

        if (mDiscoverServiceRefCount == 0)
            mDiscoverServiceRefCount = 1;
        mWaitForService = false;
        mDiscoverService = this;

        // Add anel plugin
        plugins.add(new AnelPlugin());

        // Listen to preferences changes
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        Bundle b = (intent != null) ? intent.getExtras() : null;

        // Service start code
        if (b != null)
            mDiscoverService.enterFullNetworkMode(b.getBoolean("refreshDevices"), b.getBoolean("showNotification"));
        else
            mDiscoverService.enterFullNetworkMode(true, false);

        // Notify all observers that we are ready
        Iterator<ServiceReady> it = observersServiceReady.iterator();
        while (it.hasNext()) {
            // If onServiceReady return false: remove listener (one-time listener)
            if (!it.next().onServiceReady(mDiscoverService))
                it.remove();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if (isNetworkChangedListener) {
            isNetworkChangedListener = false;
            unregisterReceiver(networkChangedListener);
        }

        // Unregister extensions receiver
        if (isExtensionsListener) {
            isExtensionsListener = false;
            try {
                unregisterReceiver(extensionsListener);
            } catch (IllegalArgumentException ignored) {
            }
        }

        // Unregister from preferences changes
        try {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            sp.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        } catch (IllegalArgumentException ignored) {
        }

        // Logging
        if (SharedPrefs.logEnergySaveMode())
            Logging.appendLog("ENDE: Hintergrunddienste aus");
        if (SharedPrefs.notifyOnStop()) {
            ShowToast.FromOtherThread(NetpowerctrlApplication.instance, R.string.service_stopped);
        }

        enterNetworkReducedMode();

        // Notify rest of the app
        for (ServiceReady anObserversServiceReady : observersServiceReady) {
            anObserversServiceReady.onServiceFinished();
        }

        // Clean up
        plugins.clear();
        mDiscoverServiceRefCount = 0;
        mDiscoverService = null;
        mWaitForService = false;

    }

    ///////////////// Service start/stop /////////////////
    private void enterNetworkReducedMode() {
        mNetworkReducedMode = true;

        for (PluginInterface pluginInterface : plugins)
            pluginInterface.enterNetworkReducedState();

        // Stop listening for network changes
        if (isNetworkChangedListener && !SharedPrefs.isWakeUpFromEnergySaving()) {
            if (SharedPrefs.logEnergySaveMode())
                Logging.appendLog("Netzwerkwechsel nicht mehr überwacht. Manuelle Suche erforderlich.");
            isNetworkChangedListener = false;
            unregisterReceiver(networkChangedListener);
        }
    }

    public void enterFullNetworkMode(boolean refreshDevices, boolean showNotification) {
        mNetworkReducedMode = false;

        if (SharedPrefs.logEnergySaveMode())
            Logging.appendLog("Hintergrunddienst gestartet");

        for (PluginInterface pluginInterface : plugins)
            pluginInterface.enterFullNetworkState(null);

        if (!isNetworkChangedListener && SharedPrefs.isEnergySavingEnabled()) {
            if (SharedPrefs.logEnergySaveMode())
                Logging.appendLog("Netzwerkwechsel überwacht");
            isNetworkChangedListener = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            registerReceiver(networkChangedListener, filter);
        }

        // refresh devices after service start
        if (refreshDevices)
            findDevices(showNotification, null);
    }

    private void extensionDiscovered(Intent i) {
        final String serviceName = i.getStringExtra(PAYLOAD_SERVICENAME);
        final String localized_name = i.getStringExtra(PAYLOAD_LOCALIZED_NAME);
        final String packageName = i.getStringExtra(PAYLOAD_PACKAGENAME);

        if (i.getIntExtra(RESULT_CODE, -1) != INITIAL_VALUES) {
            if (SharedPrefs.logEnergySaveMode())
                Logging.appendLog(localized_name + "failed");
            Log.e(TAG, localized_name + "failed");
            return;
        }

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

    public void removeExtension(PluginRemote plugin) {
        plugins.remove(plugin);
    }

    public void sendBroadcastQuery() {
        for (PluginInterface pi : plugins) {
            pi.requestData();
        }

        if (SharedPrefs.getLoadExtensions()) {
            if (!isExtensionsListener) {
                isExtensionsListener = true;
                NetpowerctrlApplication.instance.registerReceiver(extensionsListener,
                        new IntentFilter(PLUGIN_RESPONSE_ACTION));
            }

            Intent i = new Intent(PLUGIN_QUERY_ACTION);
            i.putExtra(PAYLOAD_SERVICENAME, MainActivity.class.getCanonicalName());
            NetpowerctrlApplication.instance.sendBroadcast(i);
        }
    }

    public PluginInterface getPluginInterface(Device device) {
        for (PluginInterface pi : plugins) {
            if (pi.getPluginID().equals(device.pluginID)) {
                return pi;
            }
        }
        return null;
    }

    public void findDevices(final boolean showNotification, final DeviceObserverFinishedResult callback) {
        if (mNetworkReducedMode) {
            if (SharedPrefs.notifyOnStop())
                ShowToast.FromOtherThread(NetpowerctrlService.this, getString(R.string.network_restarted));
            if (SharedPrefs.logEnergySaveMode())
                Logging.appendLog("Energiesparen aus: Suche Geräte");
            // Restart all listener services and try again
            Handler h = new Handler();
            h.post(new Runnable() {
                @Override
                public void run() {
                    enterFullNetworkMode(true, showNotification);
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
            public void onDeviceUpdated(Device di) {
            }

            @Override
            public void onObserverJobFinished(List<Device> timeout_devices) {
                RuntimeDataController c = NetpowerctrlApplication.getDataController();
                c.notifyStateQueryFinished();
                if (callback != null)
                    callback.onObserverJobFinished(timeout_devices);

                notifyRefreshState(false);

                if (showNotification) {
                    // Show notification 500ms later, to also aggregate new devices for the message
                    NetpowerctrlApplication.getMainThreadHandler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //noinspection ConstantConditions
                            Toast.makeText(NetpowerctrlApplication.instance,
                                    NetpowerctrlApplication.instance.getString(R.string.devices_refreshed,
                                            NetpowerctrlApplication.getDataController().getReachableConfiguredDevices(),
                                            NetpowerctrlApplication.getDataController().newDevices.size()),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }, 500);
                }

                if (timeout_devices.size() == 0)
                    return;

                // Do we need to go into network reduced mode?
                if (timeout_devices.size() == c.countNetworkDevices(NetpowerctrlService.this)) {
                    if (SharedPrefs.logEnergySaveMode())
                        Logging.appendLog("Energiesparen an: Keine Geräte gefunden");
                    if (SharedPrefs.notifyOnStop()) {
                        ShowToast.FromOtherThread(NetpowerctrlService.this, getString(R.string.network_no_devices));
                    }
                    enterNetworkReducedMode();
                }
            }
        });
    }

    public boolean isNetworkReducedMode() {
        return mNetworkReducedMode;
    }
}
