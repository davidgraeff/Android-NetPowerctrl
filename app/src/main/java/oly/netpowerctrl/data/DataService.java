package oly.netpowerctrl.data;

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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import oly.netpowerctrl.anel.AnelPlugin;
import oly.netpowerctrl.data.query.DataQueryCompletedObserver;
import oly.netpowerctrl.data.query.DataQueryFinishedMessageRunnable;
import oly.netpowerctrl.data.query.DataQueryRefreshObserver;
import oly.netpowerctrl.data.query.onDataQueryCompleted;
import oly.netpowerctrl.devices.Credentials;
import oly.netpowerctrl.devices.CredentialsCollection;
import oly.netpowerctrl.devices.DeviceQuery;
import oly.netpowerctrl.devices.DevicesObserver;
import oly.netpowerctrl.executables.ExecutableCollection;
import oly.netpowerctrl.groups.GroupCollection;
import oly.netpowerctrl.ioconnection.DeviceIOConnections;
import oly.netpowerctrl.ioconnection.IOConnection;
import oly.netpowerctrl.ioconnection.IOConnectionsCollection;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.status_bar.FavCollection;
import oly.netpowerctrl.timer.TimerCollection;
import oly.netpowerctrl.utils.Logging;

/**
 * Look for and load plugins. After network change: Rescan for reachable devices.
 */
public class DataService extends Service implements onDataLoaded, onPluginsReady, onDataQueryCompleted {
    public static final ServiceReadyObserver observersServiceReady = new ServiceReadyObserver();
    // Observers are static for this singleton class
    public static final DataQueryCompletedObserver observersDataQueryCompleted = new DataQueryCompletedObserver();
    public static final DataQueryRefreshObserver observersStartStopRefresh = new DataQueryRefreshObserver();
    private static final String TAG = "NetpowerctrlService";
    private static final String PLUGIN_QUERY_ACTION = "oly.netpowerctrl.plugins.INetPwrCtrlPlugin";
    private static final String PAYLOAD_SERVICE_NAME = "SERVICE_NAME";
    private static final DataLoadedObserver observersOnDataLoaded = new DataLoadedObserver();
    public static String service_shutdown_reason = "";
    ///////////////// Service start/stop listener /////////////////
    static private WeakHashMap<Object, Boolean> weakHashMap = new WeakHashMap<>();
    static private DataService mDiscoverService;
    public final PluginsReadyObserver observersPluginsReady = new PluginsReadyObserver(this);
    final public ExecutableCollection executables = new ExecutableCollection(this);
    final public CredentialsCollection credentials = new CredentialsCollection(this);
    final public GroupCollection groups = new GroupCollection(this);
    final public FavCollection favourites = new FavCollection(this);
    final public TimerCollection timers = new TimerCollection(this);
    final public IOConnectionsCollection connections = new IOConnectionsCollection(this);
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
    private final Handler stopServiceHandler = new Handler();
    private final List<AbstractBasePlugin> plugins = new ArrayList<>();
    private long mStartedByAlarmTime = -1;
    private LoadStoreCollections loadStoreCollections = new LoadStoreCollections();

    private DeviceQuery deviceQuery = new DeviceQuery(this);
    private boolean notificationAfterNextRefresh;
    private DataQueryFinishedMessageRunnable afterDataQueryFinishedHandler = new DataQueryFinishedMessageRunnable(this);

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
        return (observersServiceReady.isReady);
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
            Intent intent = new Intent(context, DataService.class);
            context.startService(intent);
        } else { // service already running. if service was going to go down, stop the shutdown task.
            mDiscoverService.stopServiceHandler.removeCallbacks(mDiscoverService.stopRunnable);
        }
    }

    public static boolean isServiceUsed() {
        return weakHashMap.size() > 0;
    }

    public static void stopUseService(Object useReference) {
        Log.w(TAG, "stopUse");
        weakHashMap.remove(useReference);
        if (weakHashMap.size() == 0 && mDiscoverService != null) {
            service_shutdown_reason = "No use of service!";
            mDiscoverService.stopServiceHandler.postDelayed(mDiscoverService.stopRunnable, 2000);
        }
    }

    public static DataService getService() {
        return mDiscoverService;
    }

    public static void addServiceToDataLoadedObserver(DataService service) {
        observersOnDataLoaded.register(service);
    }

    public static boolean isDataLoaded() {
        return observersOnDataLoaded.isDone();
    }

    /**
     * Called by the asynchronous loading process after loading is done.
     */
    public static void setDataLoadingCompleted() {
        observersOnDataLoaded.onDataLoaded();
    }

    /**
     * Notify observers who are listening to device responses
     *
     * @param ioConnection Source device that has been updated
     */
    public void notifyOfUpdatedDevice(IOConnection ioConnection) {
        deviceQuery.deviceResponded(ioConnection.credentials);
    }

    public void addDeviceObserver(DevicesObserver devicesObserver) {
        if (devicesObserver.addAllExisting)
            for (Credentials c : credentials.getItems().values()) {
                devicesObserver.credentialsList.put(c.deviceUID, c);
            }

        deviceQuery.addDeviceObserver(devicesObserver);

        if (devicesObserver.broadcast) {
            connections.clearNotConfigured();
            discoverExtensions();
            for (AbstractBasePlugin abstractBasePlugin : plugins) {
                abstractBasePlugin.requestData();
            }
        }
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
            mDiscoverService.stopServiceHandler.postDelayed(mDiscoverService.stopRunnable, 2000);
        }
    }

    /**
     * Startup is like this:
     * 1) Create anel plugin
     * 2a) Load app data (devices, alarms etc)
     * 2b) Discover devices and extensions
     * 3) If all data loaded and all extensions for all loaded devices are loaded (or timeout):
     * 3.1) updatePluginReferences
     * 3.2) resumeAllPlugins (initiates update for every loaded device, if not updated the last 5sec)
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
                timers.checkAlarm(System.currentTimeMillis());
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

        observersDataQueryCompleted.register(this);

        // We want to get notified if data has been loaded
        addServiceToDataLoadedObserver(this);

        // We load all plugins now. It works like this:
        // Plugins are either discovered (extensions) or a fixed part of this app (anel).
        // A plugin counter is increased for every fixed and discovered plugin. The plugin is added
        // to the observersPluginsReady object via add(..) and its initialisation will start. When
        // the plugin is ready it notifies observersPluginsReady and the plugin counter is decreased.
        // As soon as the counter reaches 0, the DataService (this) is notified via onPluginsReady.
        // All ready plugins will be added to the this.plugins list.
        observersPluginsReady.reset();
        // The following order is important: First increase the counter, then further increase the counter in
        // discoverExtensions and then add the AnelPlugin.
        observersPluginsReady.addPluginCount(1);
        discoverExtensions();
        observersPluginsReady.add(new AnelPlugin(this));

        // Load all Devices, Scenes etc from disk.
        /**
         * This will call setLoadStoreController(..) with a default LoadStoreData object if no such object exist so far.
         * If data is already loaded or is loading is in progress, nothing will happen.
         */
        loadStoreCollections.loadData(this);

        return START_STICKY;
    }

    public void discoverExtensions() {
        Logging.getInstance().logExtensions("Suche neue Erweiterungen");
        Intent i = new Intent(PLUGIN_QUERY_ACTION);
        i.addFlags(Intent.FLAG_FROM_BACKGROUND);
        i.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        //i.addFlags(Intent.FLAG_DEBUG_LOG_RESOLUTION);
        i.putExtra(PAYLOAD_SERVICE_NAME, MainActivity.class.getCanonicalName());
        List<ResolveInfo> list = getPackageManager().queryIntentServices(i, 0);

        observersPluginsReady.addPluginCount(list.size());

        for (ResolveInfo resolveInfo : list) {
            String serviceName = resolveInfo.serviceInfo.name;
            //String localized_name = resolveInfo.loadLabel(getPackageManager()).toString();
            String packageName = resolveInfo.serviceInfo.packageName;

            Intent intent = new Intent();
            //intent.setPackage(packageName);
            intent.setClassName(packageName, serviceName);
            intent.setAction(PLUGIN_QUERY_ACTION);
            sendBroadcast(intent);
        }
    }

    @Override
    public void onDestroy() {

        observersServiceReady.onServiceFinished(this);

        loadStoreCollections.finish(this);
        loadStoreCollections = null;

        // Clean up
        for (AbstractBasePlugin abstractBasePlugin : plugins)
            abstractBasePlugin.onDestroy();
        plugins.clear();

        weakHashMap.clear();
        mDiscoverService = null;

        Logging.getInstance().logMain("ENDE: " + service_shutdown_reason);
    }

    public AbstractBasePlugin getPlugin(int position) {
        return plugins.get(position);
    }

    public String[] pluginNames() {
        String[] ids = new String[plugins.size()];
        for (int i = 0; i < plugins.size(); ++i)
            ids[i] = plugins.get(i).getLocalizedName();
        return ids;
    }

    public AbstractBasePlugin getPlugin(String plugin_id) {
        for (AbstractBasePlugin plugin : plugins)
            if (plugin.getPluginID().equals(plugin_id))
                return plugin;
        return null;
    }

    @Override
    public boolean onPluginsReady(DataService dataService, List<AbstractBasePlugin> not_activated_plugins) {
        if (!isDataLoaded()) return true;

        // Connect credentials with plugins
        for (AbstractBasePlugin newlyAddedPlugin : not_activated_plugins) {
            //Log.w(TAG, "onDataLoaded " + newlyAddedPlugin.getPluginID());
            plugins.add(newlyAddedPlugin);
            for (Credentials credentials : this.credentials.getItems().values()) {
                if (credentials.getPlugin() != newlyAddedPlugin && credentials.pluginID.equals(newlyAddedPlugin.getPluginID())) {
                    credentials.setPlugin(newlyAddedPlugin);
                }
            }
            newlyAddedPlugin.onStart(this);
        }
        // Check all credentials for a plugin and set up IOConnections
        not_activated_plugins.clear();
        Iterator<Map.Entry<String, Credentials>> deviceIterator = credentials.getItems().entrySet().iterator();
        while (deviceIterator.hasNext()) {
            Credentials credentials = deviceIterator.next().getValue();
            if (credentials.getPlugin() == null) {
                Log.w(TAG, "Remove but not delete device without installed plugin: " + credentials.getDeviceName());
                deviceIterator.remove();
            } else {
                // set up IOConnections
                DeviceIOConnections deviceIOConnections = connections.openDevice(credentials.deviceUID);
                if (deviceIOConnections != null)
                    deviceIOConnections.applyCredentials(credentials);
                // set up executables
                executables.applyCredentials(credentials);
            }
        }

        observersServiceReady.onServiceReady(dataService);
        refreshDevices();
        return true;
    }

    @Override
    public boolean onDataLoaded() {
        observersPluginsReady.checkIfReady(); // will emit onPluginsReady again if necessary
        return false;
    }

    public boolean isPluginsLoaded() {
        return observersPluginsReady.isLoaded();
    }

    /**
     * @return Return the flag if the service has been started by an alarm. Will return the alarm time otherwise 0.
     */
    public long alarmStartedTime() {
        return mStartedByAlarmTime;
    }

    public LoadStoreCollections getLoadStoreCollections() {
        return loadStoreCollections;
    }

    /**
     * Load all devices, scenes, groups, timers. This is called by setLoadStoreController.
     * Loading of data is asynchronous and done in a background thread. Do no assume data to be
     * loaded after this method returns! Use the observersOnDataLoaded object to be notified.
     *
     * @param loadStoreCollections The object that is responsible for loading the data.
     */
    public void setLoadStoreController(LoadStoreCollections loadStoreCollections) {
        assert loadStoreCollections != null;
        if (this.loadStoreCollections != null)
            this.loadStoreCollections.finish(this);
        this.loadStoreCollections = loadStoreCollections;
        loadStoreCollections.loadData(this);
    }

    /**
     * Tidy up all lists and references.
     */
    public void clear() {
        // There shouldn't be any device-listen observers anymore,
        // but we clear the list here nevertheless.
        deviceQuery.finish();
        connections.storage.clear();
        credentials.storage.clear();
        groups.storage.clear();
        timers.storage.clear();
        executables.storage.clear();
        observersOnDataLoaded.reset();
    }

    /**
     * Add the given device to the configured devices. Use addToConfiguredDevices from the gui thread only.
     *
     * @param credentials The unconfigured device to add to the configured devices.
     */
    public void addToConfiguredDevices(Credentials credentials) {
        credentials.setConfigured(true);
        connections.save(credentials.deviceUID);
        this.credentials.put(credentials);
    }

    public void showNotificationForNextRefresh(boolean notificationAfterNextRefresh) {
        this.notificationAfterNextRefresh = notificationAfterNextRefresh;
    }

    @Override
    public boolean onDataQueryFinished(DataService dataService) {
        if (notificationAfterNextRefresh) {
            notificationAfterNextRefresh = false;
            // Show notification 500ms later, to also aggregate new devices for the message
            DataQueryFinishedMessageRunnable.show(afterDataQueryFinishedHandler);
        }
        return true;
    }

    /**
     * Refreshes all configured devices (detect unreachable devices) and detect new un-configured devices.
     */
    public void refreshDevices() {
        observersStartStopRefresh.onRefreshStateChanged(true);
        addDeviceObserver(new DevicesObserver(new DevicesObserver.onDevicesObserverFinished() {
            @Override
            public void onObserverJobFinished(DevicesObserver devicesObserver) {
                //timers.checkAlarm(DataService.getService().alarmStartedTime());
                Logging.getInstance().logEnergy("...fertig\n" + " Timeout: " + String.valueOf(devicesObserver.timedOutDevices().size()));
                observersDataQueryCompleted.onDataQueryFinished(DataService.this);
                observersStartStopRefresh.onRefreshStateChanged(false);
            }
        }));
    }

    public void remove(Credentials credentials) {
        this.credentials.remove(credentials);
        this.executables.remove(credentials);
        this.connections.remove(credentials);
    }
}
