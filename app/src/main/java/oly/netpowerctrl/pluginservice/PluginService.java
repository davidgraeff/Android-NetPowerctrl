package oly.netpowerctrl.pluginservice;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
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
import oly.netpowerctrl.data.WakeUpDeviceInterface;
import oly.netpowerctrl.data.onDataLoaded;
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
public class PluginService extends Service implements onDataLoaded, WakeUpDeviceInterface, onPluginsReady {
    public static final ServiceReadyObserver observersServiceReady = new ServiceReadyObserver();
    public static final ServiceModeChangedObserver observersServiceModeChanged = new ServiceModeChangedObserver();
    private static final String TAG = "NetpowerctrlService";
    private static final String PLUGIN_QUERY_ACTION = "oly.netpowerctrl.plugins.INetPwrCtrlPlugin";
    private static final String PAYLOAD_SERVICE_NAME = "SERVICE_NAME";
    private static final NetworkChangedBroadcastReceiver networkChangedListener = new NetworkChangedBroadcastReceiver();
    public static String service_shutdown_reason = "";
    ///////////////// Service start/stop listener /////////////////
    static private WeakHashMap<Object, Boolean> weakHashMap = new WeakHashMap<>();
    static private PluginService mDiscoverService;
    private final Runnable stopRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (mDiscoverService != null)
                    mDiscoverService.stopSelf();
            } catch (IllegalArgumentException ignored) {
            }
        }
    };
    public final PluginsReadyObserver observersPluginsReady = new PluginsReadyObserver(this);
    private final Handler stopServiceHandler = new Handler();
    private final List<AbstractBasePlugin> plugins = new ArrayList<>();
    private long mStartedByAlarmTime = -1;
    private AppData appData = new AppData(this);
    /**
     * If the listen and send thread are shutdown because the devices destination networks are
     * not in range, this variable is set to true.
     */
    public static boolean isWirelessLanConnected(Context context) {
        @SuppressWarnings("ConstantConditions")
        WifiManager cm = (WifiManager) context.getSystemService(android.content.Context.WIFI_SERVICE);
        return cm.isWifiEnabled() && cm.getConnectionInfo() != null;
    }

    static public boolean isServiceReady() {
        return (mDiscoverService != null);
    }

    /**
     * Call this in onResume if you need any of the service functionality.
     */
    public static void useService(WeakReference<Object> useReference) {
        boolean emptyUseMap = weakHashMap.isEmpty();
        weakHashMap.put(useReference, true);
        if (emptyUseMap || mDiscoverService == null) {
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
        if (weakHashMap.size() == 0) {
            service_shutdown_reason = "StopAfterAlarm";
            stopSelf();
        }
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
        // Determine why the service has been started (Alarm or a service user)
        boolean startedByAlarm = intent != null && intent.getBooleanExtra("isAlarm", false);
        mStartedByAlarmTime = -1;

        // If the service is already running, just check the alarms
        if (mDiscoverService != null) {
            // Only check alarms if the service is fully initialised, otherwise alarms will be
            // checked later.
            if (startedByAlarm && observersPluginsReady.isLoaded())
                appData.timerCollection.checkAlarm(System.currentTimeMillis());
            return super.onStartCommand(intent, flags, startId);
        }

        if (startedByAlarm) {
            mStartedByAlarmTime = System.currentTimeMillis();
            Logging.getInstance().logMain("START by alarm");
        } else if (weakHashMap.size() == 0) {
            Logging.getInstance().logMain("ILLEGAL START");
            service_shutdown_reason = "illegal start";
            stopSelf();
            Log.w(TAG, "cannot be started without useService");
            return START_NOT_STICKY;
        } else {
            Logging.getInstance().logMain("START");
            // Although the next alarm should be registered to android already,
            // we do it again, just to be sure.
            TimerCollection.armAndroidAlarm(this, SharedPrefs.getNextAlarmCheckTimestamp(this));
        }

        // We are a singleton, set the instance variable now.
        mDiscoverService = this;
        // We want to get notified if data has been loaded
        AppData.addServiceToDataLoadedObserver(this);

        // We load all plugins now. It works like this:
        // Plugins are either discovered (extensions) or a fixed part of this app (anel).
        // A plugin counter is increased for every fixed and discovered plugin. The plugin is added
        // to the observersPluginsReady object via add(..) and its initialisation will start. When
        // the plugin is ready it notifies observersPluginsReady and the plugin counter is decreased.
        // As soon as the counter reaches 0, the PluginService (this) is notified via onPluginsReady.
        // All ready plugins will be added to the this.plugins list.
        observersPluginsReady.reset();
        // The following order is important: First increase the counter, then further increase the counter in
        // discoverExtensions and then add the AnelPlugin.
        observersPluginsReady.addPluginCount(1);
        discoverExtensions();
        observersPluginsReady.add(new AnelPlugin(this));

        // Load all Devices, Scenes etc from disk.
        appData.loadData();

        return START_STICKY;
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
            observersPluginsReady.addPluginCount(list.size());
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

        observersServiceReady.onServiceFinished(this);

        appData.onDestroy();
        enterNetworkReducedMode();

        // Clean up
        removeAllPluginReferencesInDevices();
        for (AbstractBasePlugin abstractBasePlugin : plugins)
            abstractBasePlugin.onDestroy();
        plugins.clear();

        weakHashMap.clear();
        mDiscoverService = null;

        Logging.getInstance().logMain("ENDE: " + service_shutdown_reason);
    }

    ///////////////// Service start/stop /////////////////
    void enterNetworkReducedMode() {
        observersServiceModeChanged.onServiceModeChanged(true);

        AppData.observersDataQueryCompleted.reset();

        for (AbstractBasePlugin abstractBasePlugin : plugins)
            if (abstractBasePlugin.isNetworkPlugin())
                abstractBasePlugin.enterNetworkReducedState(this);

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

        for (AbstractBasePlugin abstractBasePlugin : plugins)
            abstractBasePlugin.enterFullNetworkState(this, null);

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
        for (AbstractBasePlugin pi : plugins) {
            if (pi instanceof PluginRemote && ((PluginRemote) pi).serviceName.equals(serviceName)) {
                observersPluginsReady.decreasePluginCount();
                return;
            }
        }

        final PluginRemote plugin = new PluginRemote(this, serviceName, localized_name, packageName);
        Logging.getInstance().logExtensions("Hinzufügen: " + localized_name);
        observersPluginsReady.add(plugin);
        plugin.enterFullNetworkState(PluginService.this, null);

        plugin.registerFinishedObserver(new onPluginFinished() {
            @Override
            public void onPluginFinished(AbstractBasePlugin plugin) {
                removeExtension((PluginRemote) plugin);
            }
        });
    }

    private boolean updatePluginReferencesInDevices(AbstractBasePlugin plugin, boolean updateChangesFlag) {
        boolean affectedDevices = false;
        DeviceCollection deviceCollection = appData.deviceCollection;
        for (Device device : deviceCollection.getItems()) {
            if (device.getPluginInterface() != plugin && device.pluginID.equals(plugin.getPluginID())) {
                affectedDevices = true;
                device.setPluginInterface(plugin);
                if (updateChangesFlag)
                    device.setChangesFlag(Device.CHANGE_CONNECTION_REACHABILITY);
                appData.updateExistingDevice(device, true);
            }
        }
        return affectedDevices;
    }

    private AbstractBasePlugin getPluginByID(String pluginID) {
        for (AbstractBasePlugin abstractBasePlugin : plugins)
            if (abstractBasePlugin.getPluginID().equals(pluginID))
                return abstractBasePlugin;
        return null;
    }

    public void removeExtension(@NonNull PluginRemote plugin) {
        Logging.getInstance().logExtensions("Entfernen: " + plugin.serviceName);
        observersPluginsReady.remove(plugin);
        plugins.remove(plugin);
        // Remove all references in Device objects.
        DeviceCollection deviceCollection = appData.deviceCollection;
        for (Device device : deviceCollection.getItems()) {
            if (device.getPluginInterface() == plugin) {
                device.setPluginInterface(null);
                device.setStatusMessageAllConnections(getString(R.string.error_plugin_removed));
                device.setChangesFlag(Device.CHANGE_CONNECTION_REACHABILITY);
                appData.updateExistingDevice(device, true);
            }
        }
    }

    private void removeAllPluginReferencesInDevices() {
        // Remove all references in Device objects.
        DeviceCollection deviceCollection = appData.deviceCollection;
        observersPluginsReady.reset();
        for (Device device : deviceCollection.getItems()) {
            device.setPluginInterface(null);
            device.setStatusMessageAllConnections(getString(R.string.error_plugin_removed));
            device.setChangesFlag(Device.CHANGE_CONNECTION_REACHABILITY);
        }
    }

    /**
     * This does nothing if a plugin is already awake.
     * Wake up a plugin, but will send it to sleep again if the given device didn't get updated within 3s
     *
     * @param device The device to wake up.
     * @return Return true if wake up worked.
     */
    @Override
    public boolean wakeupPlugin(Device device) {
        AbstractBasePlugin abstractBasePlugin = (AbstractBasePlugin) device.getPluginInterface();
        if (abstractBasePlugin != null) {
            if (abstractBasePlugin.isNetworkReducedState())
                abstractBasePlugin.enterFullNetworkState(this, device);
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

    public AbstractBasePlugin getPlugin(int selected) {
        return plugins.get(selected);
    }

    public AbstractBasePlugin getPlugin(String plugin_id) {
        if (plugin_id == null)
            return null;
        for (AbstractBasePlugin plugin : plugins)
            if (plugin.getPluginID().equals(plugin_id))
                return plugin;
        return null;
    }

    public void requestDataAll() {
        for (AbstractBasePlugin abstractBasePlugin : plugins) {
            abstractBasePlugin.requestData();
        }
    }

    @Override
    public boolean onPluginsReady(PluginService pluginService, List<AbstractBasePlugin> not_activated_plugins) {
        if (!AppData.isDataLoaded()) return true;

        for (AbstractBasePlugin newlyAddedPlugin : not_activated_plugins) {
            Log.w(TAG, "onDataLoaded " + newlyAddedPlugin.getPluginID());
            plugins.add(newlyAddedPlugin);
            updatePluginReferencesInDevices(newlyAddedPlugin, false);
        }
        not_activated_plugins.clear();
        for (Device device : appData.deviceCollection.getItems()) {
            if (device.getPluginInterface() == null)
                throw new RuntimeException("No Plugin for device " + device.getDeviceName());
        }
        observersServiceReady.onServiceReady(pluginService);
        appData.refreshDeviceData(pluginService, false);
        return true;
    }

    @Override
    public boolean onDataLoaded() {
        observersPluginsReady.checkIfReady(); // will emit onPluginsReady again if necessary
        return false;
    }

    public AppData getAppData() {
        return appData;
    }

    public boolean isPluginsLoaded() {
        return observersPluginsReady.isLoaded();
    }

    /**
     * Return and reset the flag, if the service has been started by an alarm.
     *
     * @return
     */
    public long alarmStartedTime() {
        return mStartedByAlarmTime;
    }
}
