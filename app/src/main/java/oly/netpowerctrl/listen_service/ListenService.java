package oly.netpowerctrl.listen_service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anel.AnelPlugin;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.data.onDataLoaded;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.devices.DeviceCollection;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.network.DeviceQuery;
import oly.netpowerctrl.network.onDeviceObserverFinishedResult;
import oly.netpowerctrl.network.onDeviceObserverResult;
import oly.netpowerctrl.utils.Logging;

/**
 * Look for and load plugins. After network change: Rescan for reachable devices.
 */
public class ListenService extends Service {
    public static final ServiceReadyObserver observersServiceReady = new ServiceReadyObserver();
    public static final ServiceRefreshQueryObserver observersStartStopRefresh = new ServiceRefreshQueryObserver();
    public static final ServiceModeChangedObserver observersServiceModeChanged = new ServiceModeChangedObserver();
    private static final String TAG = "NetpowerctrlService";
    private static final String PLUGIN_QUERY_ACTION = "oly.netpowerctrl.plugins.INetPwrCtrlPlugin";
    private static final String PAYLOAD_SERVICE_NAME = "SERVICE_NAME";
    public static String service_shutdown_reason = "";
    static int findDevicesRun = 0;
    ///////////////// Service start/stop listener /////////////////
    static private int mDiscoverServiceRefCount = 0;
    static private ListenService mDiscoverService;
    static private boolean mWaitForService;
    private final Runnable stopRunnable = new Runnable() {
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
    private final Handler stopServiceHandler = new Handler();

    private final BroadcastReceiver networkChangedListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            @SuppressWarnings("ConstantConditions")
            ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected()) {
                if (SharedPrefs.getInstance().logEnergySaveMode())
                    Logging.appendLog(ListenService.this, "Energiesparen aus: Netzwechsel erkannt");
                enterFullNetworkMode(true, false);
            } else {
                if (SharedPrefs.getInstance().logEnergySaveMode())
                    Logging.appendLog(ListenService.this, "Energiesparen an: Kein Netzwerk");

                enterNetworkReducedMode();
            }
        }
    };
    private final List<PluginInterface> plugins = new ArrayList<>();
    private onDataLoaded onDataLoadedListener = new onDataLoaded() {
        @Override
        public boolean onDataLoaded() {
            for (PluginInterface pluginInterface : plugins)
                updatePluginReferencesInDevices(pluginInterface);

            enterFullNetworkMode(true, false);

            return false;
        }
    };
    // Debug
    long startTime;
    /**
     * If the listen and send thread are shutdown because the devices destination networks are
     * not in range, this variable is set to true.
     */
    private boolean mNetworkReducedMode;
    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            if (SharedPrefs.getInstance().isPreferenceNameLogEnergySaveMode(s) && mNetworkReducedMode) {
                if (SharedPrefs.getInstance().logEnergySaveMode())
                    Logging.appendLog(ListenService.this, "Energiesparen abgeschaltet");
                enterFullNetworkMode(true, false);
            }
        }
    };
    private boolean isNetworkChangedListener = false;
    /**
     * Detect new devices and check reach-ability of configured devices.
     */
    private boolean isDetecting = false;

    public static boolean isWirelessLanConnected(Context context) {
        @SuppressWarnings("ConstantConditions")
        WifiManager cm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return cm.isWifiEnabled() && cm.getConnectionInfo() != null;
    }

    static public boolean isServiceReady() {
        return (mDiscoverService != null);
    }

    /**
     * Call this in onResume if you need any of the service functionality.
     *
     * @param refreshDevices   Refresh devices immediately or after the service
     *                         is ready if it is not started yet.
     * @param showNotification
     */
    public static void useService(Context context, boolean refreshDevices, boolean showNotification) {
        ++mDiscoverServiceRefCount;
        // Stop delayed stop-service
        // Service is not running anymore, restart it
        if (mDiscoverService == null) {
            if (mWaitForService)
                return;
            mWaitForService = true;
            // start service
            Intent intent = new Intent(context, ListenService.class);
            intent.putExtra("refreshDevices", refreshDevices);
            intent.putExtra("showNotification", showNotification);
            context.startService(intent);
        } else { // service already running. refresh devices?
            service_shutdown_reason = "";
            mDiscoverService.stopServiceHandler.removeCallbacks(mDiscoverService.stopRunnable);
            if (refreshDevices)
                mDiscoverService.findDevices(showNotification, null);
        }
    }

    public static boolean isServiceUsed() {
        return mDiscoverServiceRefCount > 0;
    }

    public static void stopUseService() {
        if (mDiscoverServiceRefCount > 0) {
            mDiscoverServiceRefCount--;
        }
        if (mDiscoverServiceRefCount == 0 && mDiscoverService != null) {
            service_shutdown_reason = "No use of service!";
            mDiscoverService.stopServiceHandler.postDelayed(mDiscoverService.stopRunnable, 2000);
        }
    }

    public static int getUsedCount() {
        return mDiscoverServiceRefCount;
    }

    public static ListenService getService() {
        return mDiscoverService;
    }

    public static void debug_toggle_network_reduced() {
        if (mDiscoverService == null) {
            Toast.makeText(App.instance, "Unexpected state: Listen Service not running", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mDiscoverService.isNetworkReducedMode())
            mDiscoverService.enterFullNetworkMode(true, true);
        else {
            AppData c = AppData.getInstance();
            for (Device d : c.deviceCollection.getItems()) {
                d.setStatusMessageAllConnections("Debug force off");
                c.updateDevice(d);
            }
            mDiscoverService.enterNetworkReducedMode();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (mDiscoverService != null)
            return super.onStartCommand(intent, flags, startId);

        if (mDiscoverServiceRefCount == 0)
            mDiscoverServiceRefCount = 1;
        mWaitForService = false;
        mDiscoverService = this;

        plugins.add(new AnelPlugin());

        // We may be in the situation that the service and plugins are ready before
        // all devices are loaded. Therefore create anel plugin and update plugin references after load.
        AppData.observersOnDataLoaded.register(onDataLoadedListener);

        // Listen to preferences changes
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        observersServiceReady.onServiceReady(ListenService.this);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if (isNetworkChangedListener) {
            isNetworkChangedListener = false;
            unregisterReceiver(networkChangedListener);
        }

        // Unregister from preferences changes
        try {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            sp.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        } catch (IllegalArgumentException ignored) {
        }

        // Logging
        if (SharedPrefs.getInstance().logEnergySaveMode())
            Logging.appendLog(this, "ENDE: Hintergrunddienste aus");

        enterNetworkReducedMode();

        observersServiceReady.onServiceFinished();

        // Clean up
        removePluginReferencesInDevices(null);
        for (PluginInterface pluginInterface : plugins)
            pluginInterface.onDestroy();
        plugins.clear();

        mDiscoverServiceRefCount = 0;
        mDiscoverService = null;
        mWaitForService = false;

    }

    ///////////////// Service start/stop /////////////////
    private void enterNetworkReducedMode() {
        Log.w(TAG, "findDevices:enterNetworkReducedMode " + String.valueOf((System.nanoTime() - startTime) / 1000000.0));
        mNetworkReducedMode = true;

        observersServiceModeChanged.onServiceModeChanged(true);

        for (PluginInterface pluginInterface : plugins)
            pluginInterface.enterNetworkReducedState(this);

        // Stop listening for network changes
        if (isNetworkChangedListener && !SharedPrefs.getInstance().isWakeUpFromEnergySaving()) {
            if (SharedPrefs.getInstance().logEnergySaveMode())
                Logging.appendLog(this, "Netzwerkwechsel nicht mehr überwacht. Manuelle Suche erforderlich.");
            isNetworkChangedListener = false;
            unregisterReceiver(networkChangedListener);
        }
    }

    public void enterFullNetworkMode(boolean refreshDevices, boolean showNotification) {
        mNetworkReducedMode = false;

        observersServiceModeChanged.onServiceModeChanged(false);

        if (SharedPrefs.getInstance().logEnergySaveMode())
            Logging.appendLog(this, "Hintergrunddienst gestartet");

        for (PluginInterface pluginInterface : plugins)
            pluginInterface.enterFullNetworkState(this, null);

        AppData.getInstance().deviceCollection.setHasChangedAll();

        if (!isNetworkChangedListener) {
            if (SharedPrefs.getInstance().logEnergySaveMode())
                Logging.appendLog(this, "Netzwerkwechsel überwacht");
            isNetworkChangedListener = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            registerReceiver(networkChangedListener, filter);
        }

        // refresh devices after service start
        if (refreshDevices)
            findDevices(showNotification, null);
    }

    private void extensionDiscovered(String serviceName, String localized_name, String packageName) {
        if (serviceName == null || serviceName.isEmpty() || packageName == null || packageName.isEmpty()) {
            if (SharedPrefs.getInstance().logExtensions())
                Logging.appendLog(this, localized_name + "failed");
            Log.e(TAG, localized_name + " failed");
            return;
        }

        Log.w(TAG, "Extension: " + serviceName + " " + localized_name + " " + packageName);

        /**
         * We received a message from a plugin, we already know: ignore
         */
        for (PluginInterface pi : plugins) {
            if (pi instanceof PluginRemote && ((PluginRemote) pi).serviceName.equals(serviceName)) {
                updatePluginReferencesInDevices(pi);
                return;
            }
        }

        PluginRemote plugin = PluginRemote.create(serviceName, localized_name, packageName);

        if (plugin == null) {
            return;
        }

        plugins.add(plugin);
        updatePluginReferencesInDevices(plugin);
    }

    private void updatePluginReferencesInDevices(PluginInterface plugin) {
        DeviceCollection deviceCollection = AppData.getInstance().deviceCollection;
        for (Device device : deviceCollection.getItems()) {
            if (device.pluginID.equals(plugin.getPluginID()) && device.getPluginInterface() != plugin) {
                device.setPluginInterface(plugin);
                device.setHasChanged();
                AppData.getInstance().updateExistingDevice(device);
            }
        }
    }

    public PluginInterface getPluginByID(String pluginID) {
        for (PluginInterface pluginInterface : plugins)
            if (pluginInterface.getPluginID().equals(pluginID))
                return pluginInterface;
        return null;
    }

    public void removeExtension(PluginRemote plugin) {
        plugins.remove(plugin);
        removePluginReferencesInDevices(plugin);
    }

    private void removePluginReferencesInDevices(PluginInterface plugin) {
        // Remove all references in Device objects.
        DeviceCollection deviceCollection = AppData.getInstance().deviceCollection;
        for (Device device : deviceCollection.getItems()) {
            if (plugin == null || device.getPluginInterface() == plugin) {
                device.setPluginInterface(null);
                device.setStatusMessageAllConnections(getString(R.string.error_plugin_not_installed));
                device.setHasChanged();
                AppData.getInstance().updateExistingDevice(device);
            }
        }
    }

    public void sendBroadcastQuery() {
        for (PluginInterface pluginInterface : plugins) {
            pluginInterface.requestData();
        }

        if (SharedPrefs.getInstance().getLoadExtensions()) {
            Intent i = new Intent(PLUGIN_QUERY_ACTION);
            i.addFlags(Intent.FLAG_FROM_BACKGROUND);
            i.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            i.addFlags(Intent.FLAG_DEBUG_LOG_RESOLUTION);
            i.putExtra(PAYLOAD_SERVICE_NAME, MainActivity.class.getCanonicalName());
            List<ResolveInfo> list = getPackageManager().queryIntentServices(i, 0);
            for (ResolveInfo resolveInfo : list) {
                extensionDiscovered(resolveInfo.serviceInfo.name, resolveInfo.loadLabel(getPackageManager()).toString(), resolveInfo.serviceInfo.packageName);
            }
        }
    }

    public void findDevices(final boolean showNotification, final onDeviceObserverFinishedResult callback) {
        final int currentRun = ++findDevicesRun;

        // The following mechanism allows only one update request within a
        // 1sec timeframe.
        if (isDetecting)
            return;
        isDetecting = true;

        startTime = System.nanoTime();
        Log.w(TAG, "findDevices:start " + String.valueOf((System.nanoTime() - startTime) / 1000000.0) + " " + String.valueOf(currentRun));

        App.getMainThreadHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                isDetecting = false;
            }
        }, 1000);

        if (mNetworkReducedMode) {
            if (SharedPrefs.getInstance().logEnergySaveMode())
                Logging.appendLog(this, "Energiesparen aus: Suche Geräte");

            enterFullNetworkMode(false, false);
        }

        observersStartStopRefresh.onRefreshStateChanged(true);

        // First try a broadcast
        AppData.getInstance().clearNewDevices();
        new DeviceQuery(this, new onDeviceObserverResult() {
            @Override
            public void onObserverDeviceUpdated(Device di) {
            }

            @Override
            public void onObserverJobFinished(List<Device> timeout_devices) {
                Log.w(TAG, "findDevices:job_finished " + String.valueOf((System.nanoTime() - startTime) / 1000000.0) + " " + String.valueOf(currentRun));
                AppData.observersDataQueryCompleted.onDataQueryFinished();
                if (callback != null)
                    callback.onObserverJobFinished(timeout_devices);

                observersStartStopRefresh.onRefreshStateChanged(false);

                if (showNotification) {
                    // Show notification 500ms later, to also aggregate new devices for the message
                    App.getMainThreadHandler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //noinspection ConstantConditions
                            Toast.makeText(ListenService.this,
                                    ListenService.this.getString(R.string.devices_refreshed,
                                            AppData.getInstance().getReachableConfiguredDevices(),
                                            AppData.getInstance().unconfiguredDeviceCollection.size()),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }, 500);
                }

                if (timeout_devices.size() == 0)
                    return;
                Log.w(TAG, "findDevices:timeout_devices " + String.valueOf((System.nanoTime() - startTime) / 1000000.0) + " " + String.valueOf(currentRun));

                // Do we need to go into network reduced mode?
                if (timeout_devices.size() == AppData.getInstance().countNetworkDevices()) {
                    if (SharedPrefs.getInstance().logEnergySaveMode())
                        Logging.appendLog(ListenService.this, "Energiesparen an: Keine Geräte gefunden");
                    enterNetworkReducedMode();
                }
            }
        });
    }

    public boolean isNetworkReducedMode() {
        return mNetworkReducedMode;
    }

    public String[] pluginIDs() {
        String[] ids = new String[plugins.size()];
        for (int i = 0; i < plugins.size(); ++i)
            ids[i] = plugins.get(i).getPluginID();
        return ids;
    }
}
