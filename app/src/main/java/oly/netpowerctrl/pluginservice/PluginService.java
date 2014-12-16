package oly.netpowerctrl.pluginservice;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

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
    public static final PluginsReadyObserver observersPluginsReady = new PluginsReadyObserver();
    private static final String TAG = "NetpowerctrlService";
    private static final String PLUGIN_QUERY_ACTION = "oly.netpowerctrl.plugins.INetPwrCtrlPlugin";
    private static final String PAYLOAD_SERVICE_NAME = "SERVICE_NAME";
    private static final NetworkChangedBroadcastReceiver networkChangedListener = new NetworkChangedBroadcastReceiver();
    public static String service_shutdown_reason = "";
    ///////////////// Service start/stop listener /////////////////
    static private WeakHashMap<Object, Boolean> weakHashMap = new WeakHashMap<>();
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


    // Debug
    private boolean notificationAfterNextRefresh;
    private DataQueryFinishedMessageRunnable afterDataQueryFinishedHandler = new DataQueryFinishedMessageRunnable();

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
    public static void useService(WeakReference<Object> useReference) {
        weakHashMap.put(useReference, true);
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
        } else { // service already running. if service was going to go down, stop the shutdown task.
            service_shutdown_reason = "";
            mDiscoverService.stopServiceHandler.removeCallbacks(mDiscoverService.stopRunnable);
        }
    }

    public static boolean isServiceUsed() {
        return weakHashMap.size() > 0;
    }

    public static void stopUseService(Object useReference) {
        weakHashMap.remove(useReference);
        if (weakHashMap.size() == 0 && mDiscoverService != null) {
            service_shutdown_reason = "No use of service!";
            mDiscoverService.stopServiceHandler.postDelayed(mDiscoverService.stopRunnable, 2000);
        }
    }

    public static PluginService getService() {
        return mDiscoverService;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Called after an alarm is executed. If the service is not used anymore (weakHashMap empty),
     * then stop the service.
     */
    public void checkStopAfterAlarm() {
        if (weakHashMap.size() == 0) stopSelf();
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
    public int onStartCommand(@Nullable final Intent intent, final int flags, final int startId) {
        if (mDiscoverService != null) {
            TimerCollection.checkAndExecuteAlarm();
            return super.onStartCommand(intent, flags, startId);
        }

        boolean isAlarm = intent != null && intent.getBooleanExtra("isAlarm", false);

        if (isAlarm) {
            Logging.getInstance().logMain("START by alarm");
        } else if (weakHashMap.size() == 0) {
            Logging.getInstance().logMain("ILLEGAL START");
            stopSelf();
            Log.w(TAG, "cannot be started without useService");
            return START_NOT_STICKY;
        } else
            Logging.getInstance().logMain("START");

        mWaitForService = false;
        mDiscoverService = this;

        plugins.add(new AnelPlugin());

        AppData.observersDataQueryCompleted.register(this);
        AppData.observersOnDataLoaded.register(this);

        discoverExtensions();

        return super.onStartCommand(intent, flags, startId);
    }

    public void discoverExtensions() {
        if (SharedPrefs.getInstance().getLoadExtensions()) {
            Logging.getInstance().logExtensions("Suche neue Erweiterungen");
            Intent i = new Intent(PLUGIN_QUERY_ACTION);
            i.addFlags(Intent.FLAG_FROM_BACKGROUND);
            i.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            //i.addFlags(Intent.FLAG_DEBUG_LOG_RESOLUTION);
            i.putExtra(PAYLOAD_SERVICE_NAME, MainActivity.class.getCanonicalName());
            List<ResolveInfo> list = getPackageManager().queryIntentServices(i, 0);
            observersPluginsReady.setPluginCount(list.size());
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

        Logging.getInstance().logMain("ENDE");

        enterNetworkReducedMode();

        observersServiceReady.onServiceFinished();

        // Clean up
        removePluginReferencesInDevices(null);
        for (PluginInterface pluginInterface : plugins)
            pluginInterface.onDestroy();
        plugins.clear();

        weakHashMap.clear();
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
        if (SharedPrefs.getInstance().isMaximumEnergySaving()) {
            networkChangedListener.unregister(this);
            Logging.getInstance().logEnergy("Netzwerkwechsel nicht mehr überwacht!");
        }
    }

    /**
     * Do not use DeviceObserver here!
     */
    public void wakeupAllDevices() {
        observersServiceModeChanged.onServiceModeChanged(false);

        Logging.getInstance().logEnergy("Alle Geräte aufgeweckt");

        for (PluginInterface pluginInterface : plugins)
            pluginInterface.enterFullNetworkState(this, null);

        networkChangedListener.registerReceiver(this);
        if (networkChangedListener.isNetworkChangedListener)
            Logging.getInstance().logEnergy("Netzwerkwechsel überwacht");
    }

    private void extensionDiscovered(String serviceName, String localized_name, String packageName) {
        if (serviceName == null || serviceName.isEmpty() || packageName == null || packageName.isEmpty()) {
            observersPluginsReady.decreasePluginCount();
            Logging.getInstance().logExtensions(localized_name + " Fehler!");
            Log.e(TAG, localized_name + " failed");
            return;
        }

        /**
         * We received a message from a plugin, we already know: ignore
         */
        for (PluginInterface pi : plugins) {
            if (pi instanceof PluginRemote && ((PluginRemote) pi).serviceName.equals(serviceName)) {
                observersPluginsReady.decreasePluginCount();
                return;
            }
        }

        final PluginRemote plugin = new PluginRemote(this, serviceName, localized_name, packageName);
        Logging.getInstance().logExtensions("Hinzufügen: " + localized_name);
        plugins.add(plugin);
        plugin.enterFullNetworkState(PluginService.this, null);

        plugin.registerReadyObserver(new onPluginReady() {
            @Override
            public void onPluginReady(PluginRemote plugin) {
                Logging.getInstance().logExtensions("Aktiv: " + plugin.serviceName);
                if (AppData.observersOnDataLoaded.dataLoaded)
                    updatePluginReferencesInDevices(plugin, false);
                observersPluginsReady.decreasePluginCount();
            }

            @Override
            public void onPluginFailedToInit(PluginRemote plugin) {
                Logging.getInstance().logExtensions("Fehler: " + plugin.serviceName);
                if (AppData.observersOnDataLoaded.dataLoaded)
                    updatePluginReferencesInDevices(plugin, false);
                observersPluginsReady.decreasePluginCount();
            }

            @Override
            public void onPluginFinished(PluginRemote plugin) {
                removeExtension(plugin);
            }
        });
    }

    private boolean updatePluginReferencesInDevices(PluginInterface plugin, boolean updateChangesFlag) {
        boolean affectedDevices = false;
        DeviceCollection deviceCollection = AppData.getInstance().deviceCollection;
        for (Device device : deviceCollection.getItems()) {
            if (device.getPluginInterface() != plugin && device.pluginID.equals(plugin.getPluginID())) {
                affectedDevices = true;
                device.setPluginInterface(plugin);
                if (updateChangesFlag)
                    device.setChangesFlag(Device.CHANGE_CONNECTION_REACHABILITY);
                AppData.getInstance().updateExistingDevice(device);
            }
        }
        return affectedDevices;
    }

    private PluginInterface getPluginByID(String pluginID) {
        for (PluginInterface pluginInterface : plugins)
            if (pluginInterface.getPluginID().equals(pluginID))
                return pluginInterface;
        return null;
    }

    public void removeExtension(PluginRemote plugin) {
        Logging.getInstance().logExtensions("Entfernen: " + plugin.serviceName);
        plugins.remove(plugin);
        removePluginReferencesInDevices(plugin);
    }

    private void removePluginReferencesInDevices(PluginInterface plugin) {
        // Remove all references in Device objects.
        DeviceCollection deviceCollection = AppData.getInstance().deviceCollection;
        for (Device device : deviceCollection.getItems()) {
            if (plugin == null || device.getPluginInterface() == plugin) {
                device.setPluginInterface(null);
                device.setStatusMessageAllConnections(getString(R.string.error_plugin_removed));
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
            DataQueryFinishedMessageRunnable.show(afterDataQueryFinishedHandler);
        }

        TimerCollection.checkAndExecuteAlarm();

        return true;
    }

    @Override
    public boolean onDataLoaded() {
        for (PluginInterface pluginInterface : plugins)
            updatePluginReferencesInDevices(pluginInterface, false);

        observersPluginsReady.register(new onPluginsReady() {
            @Override
            public boolean onPluginsReady() {
                observersServiceReady.onServiceReady(PluginService.this);
                AppData.getInstance().refreshDeviceData(false);
                return false;
            }
        });

        return false;
    }
}
