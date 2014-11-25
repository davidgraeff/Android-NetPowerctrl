package oly.netpowerctrl.pluginservice;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
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
import oly.netpowerctrl.data.onDataQueryCompleted;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.devices.DeviceCollection;
import oly.netpowerctrl.devices.EditDeviceInterface;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.timer.TimerCollection;
import oly.netpowerctrl.utils.Logging;

/**
 * Look for and load plugins. After network change: Rescan for reachable devices.
 */
public class PluginService extends Service implements onDataQueryCompleted, onDataLoaded {
    public static final ServiceReadyObserver observersServiceReady = new ServiceReadyObserver();
    public static final ServiceModeChangedObserver observersServiceModeChanged = new ServiceModeChangedObserver();
    private static final String TAG = "NetpowerctrlService";
    private static final String PLUGIN_QUERY_ACTION = "oly.netpowerctrl.plugins.INetPwrCtrlPlugin";
    private static final String PAYLOAD_SERVICE_NAME = "SERVICE_NAME";
    private static final NetworkChangedBroadcastReceiver networkChangedListener = new NetworkChangedBroadcastReceiver();
    public static String service_shutdown_reason = "";
    ///////////////// Service start/stop listener /////////////////
    static private int mDiscoverServiceRefCount = 0;
    static private PluginService mDiscoverService;
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
    private final List<PluginInterface> plugins = new ArrayList<>();

    /**
     * If the listen and send thread are shutdown because the devices destination networks are
     * not in range, this variable is set to true.
     */

    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            if (SharedPrefs.getInstance().isPreferenceNameLogEnergySaveMode(s)) {
                if (SharedPrefs.getInstance().logEnergySaveMode())
                    Logging.appendLog(PluginService.this, "Energiesparen abgeschaltet");
                wakeupAllDevices();
            }
        }
    };
    // Debug
    private boolean notificationAfterNextRefresh;

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
     */
    public static void useService() {
        ++mDiscoverServiceRefCount;
        // Stop delayed stop-service
        // Service is not running anymore, restart it
        if (mDiscoverService == null) {
            if (mWaitForService)
                return;
            mWaitForService = true;
            Context context = App.instance;
            // start service
            Intent intent = new Intent(context, PluginService.class);
            context.startService(intent);
        } else { // service already running. refresh devices?
            service_shutdown_reason = "";
            mDiscoverService.stopServiceHandler.removeCallbacks(mDiscoverService.stopRunnable);
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

    public static PluginService getService() {
        return mDiscoverService;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Startup is like this:
     * 1) Create anel plugin
     * 2a) Load app data (devices, alarms etc)
     * 2b) Discover devices and extensions
     * 3) If all data loaded and all extensions for all loaded devices are loaded (or timeout):
     * 3.1) updatePluginReferencesInDevices
     * 3.2) wakeupAllDevices (initiates update for every loaded device, if not updated the last 5sec)
     */
    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        Log.w(TAG, "start service");

        if (mDiscoverService != null) {
            TimerCollection.checkAndExecuteAlarm();
            return super.onStartCommand(intent, flags, startId);
        }

        if (mDiscoverServiceRefCount == 0)
            mDiscoverServiceRefCount = 1;
        mWaitForService = false;
        mDiscoverService = this;

        plugins.add(new AnelPlugin());

        // Listen to preferences changes
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        AppData.observersDataQueryCompleted.register(this);
        AppData.observersOnDataLoaded.register(this);

        discoverExtensions();

        return super.onStartCommand(intent, flags, startId);
    }

    public void discoverExtensions() {
        if (SharedPrefs.getInstance().getLoadExtensions()) {
            Intent i = new Intent(PLUGIN_QUERY_ACTION);
            i.addFlags(Intent.FLAG_FROM_BACKGROUND);
            i.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            //i.addFlags(Intent.FLAG_DEBUG_LOG_RESOLUTION);
            i.putExtra(PAYLOAD_SERVICE_NAME, MainActivity.class.getCanonicalName());
            List<ResolveInfo> list = getPackageManager().queryIntentServices(i, 0);
            for (ResolveInfo resolveInfo : list) {
                extensionDiscovered(resolveInfo.serviceInfo.name, resolveInfo.loadLabel(getPackageManager()).toString(), resolveInfo.serviceInfo.packageName);
            }
        }
    }

    @Override
    public void onDestroy() {
        if (networkChangedListener.isNetworkChangedListener) {
            networkChangedListener.isNetworkChangedListener = false;
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
    void enterNetworkReducedMode() {
        observersServiceModeChanged.onServiceModeChanged(true);

        AppData.observersDataQueryCompleted.resetDataQueryCompleted();

        for (PluginInterface pluginInterface : plugins)
            if (pluginInterface.isNetworkPlugin())
                pluginInterface.enterNetworkReducedState(this);

        // Stop listening for network changes
        if (!SharedPrefs.getInstance().isWakeUpFromEnergySaving()) {
            networkChangedListener.unregister(this);
            if (SharedPrefs.getInstance().logEnergySaveMode())
                Logging.appendLog(this, "Netzwerkwechsel nicht mehr überwacht. Manuelle Suche erforderlich.");
        }
    }

    /**
     * Do not use DeviceObserver here!
     */
    public void wakeupAllDevices() {
        observersServiceModeChanged.onServiceModeChanged(false);

        if (SharedPrefs.getInstance().logEnergySaveMode())
            Logging.appendLog(this, "Alle Geräte aufgeweckt");

        for (PluginInterface pluginInterface : plugins)
            pluginInterface.enterFullNetworkState(this, null);

        networkChangedListener.registerReceiver(this);
        if (networkChangedListener.isNetworkChangedListener && SharedPrefs.getInstance().logEnergySaveMode())
            Logging.appendLog(this, "Netzwerkwechsel überwacht");
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
                return;
            }
        }

        final PluginRemote plugin = PluginRemote.create(serviceName, localized_name, packageName);

        if (plugin == null) {
            return;
        }

        plugin.registerReadyObserver(new onPluginReady() {
            @Override
            public void onPluginReady(PluginRemote plugin) {
                plugins.add(plugin);
                plugin.enterFullNetworkState(PluginService.this, null);
                if (AppData.observersOnDataLoaded.dataLoaded)
                    updatePluginReferencesInDevices(plugin, false);
            }

            @Override
            public void onFinished(PluginRemote plugin) {
                removeExtension(plugin);
            }
        });
    }

    private void updatePluginReferencesInDevices(PluginInterface plugin, boolean updateChangesFlag) {
        DeviceCollection deviceCollection = AppData.getInstance().deviceCollection;
        for (Device device : deviceCollection.getItems()) {
            if (device.getPluginInterface() != plugin && device.pluginID.equals(plugin.getPluginID())) {
                device.setPluginInterface(plugin);
                if (updateChangesFlag)
                    device.setChangesFlag(Device.CHANGE_CONNECTION_REACHABILITY);
                AppData.getInstance().updateExistingDevice(device);
            }
        }
    }

    private PluginInterface getPluginByID(String pluginID) {
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
                device.setChangesFlag(Device.CHANGE_CONNECTION_REACHABILITY);
                AppData.getInstance().updateExistingDevice(device);
            }
        }
    }

    /**
     * This does nothing if a plugin is already awake.
     * Wake up a plugin, but will send it to sleep again if the given device didn't get updated within 3s
     *
     * @param device
     * @return
     */
    public boolean wakeupPlugin(Device device) {
        PluginInterface pluginInterface = (PluginInterface) device.getPluginInterface();
        if (pluginInterface != null) {
            if (pluginInterface.isNetworkReducedState())
                pluginInterface.enterFullNetworkState(this, device);
            return true;
        } else {
            return false;
        }
    }

    public String[] pluginIDs() {
        String[] ids = new String[plugins.size()];
        for (int i = 0; i < plugins.size(); ++i)
            ids[i] = plugins.get(i).getPluginID();
        return ids;
    }

    public EditDeviceInterface openEditDevice(Device device) {
        return getPluginByID(device.pluginID).openEditDevice(device);
    }

    public PluginInterface getPlugin(int selected) {
        return plugins.get(selected);
    }

    public PluginInterface getPlugin(String plugin_id) {
        if (plugin_id == null)
            return null;
        for (PluginInterface plugin : plugins)
            if (plugin.getPluginID().equals(plugin_id))
                return plugin;
        return null;
    }

    public void requestDataAll() {
        for (PluginInterface pluginInterface : plugins) {
            pluginInterface.requestData();
        }
    }

    public void showNotificationForNextRefresh(boolean notificationAfterNextRefresh) {
        this.notificationAfterNextRefresh = notificationAfterNextRefresh;
    }

    @Override
    public boolean onDataQueryFinished(boolean networkDevicesNotReachable) {
        if (notificationAfterNextRefresh) {
            notificationAfterNextRefresh = false;
            // Show notification 500ms later, to also aggregate new devices for the message
            App.getMainThreadHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    //noinspection ConstantConditions
                    Toast.makeText(App.instance,
                            App.instance.getString(R.string.devices_refreshed,
                                    AppData.getInstance().getReachableConfiguredDevices(),
                                    AppData.getInstance().unconfiguredDeviceCollection.size()),
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }, 500);
        }

        TimerCollection.checkAndExecuteAlarm();

        return true;
    }

    @Override
    public boolean onDataLoaded() {
        for (PluginInterface pluginInterface : plugins)
            updatePluginReferencesInDevices(pluginInterface, false);

        observersServiceReady.onServiceReady(PluginService.this);
        AppData.getInstance().refreshDeviceData(false);

        return false;
    }
}
