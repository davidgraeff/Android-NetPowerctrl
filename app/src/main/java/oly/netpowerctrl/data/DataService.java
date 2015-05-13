package oly.netpowerctrl.data;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import oly.netpowerctrl.data.query.DataQueryCompletedObserver;
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
import oly.netpowerctrl.main.GuiThreadHandler;
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.network.NetworkChangedBroadcastReceiver;
import oly.netpowerctrl.network.ReachabilityStates;
import oly.netpowerctrl.network.UDPSend;
import oly.netpowerctrl.plugin_anel.AnelPlugin;
import oly.netpowerctrl.plugin_simpleudp.SimpleUDPPlugin;
import oly.netpowerctrl.status_bar.FavCollection;
import oly.netpowerctrl.utils.Logging;

/**
 * Look for and load plugins. After network change: Rescan for reachable devices.
 */
public class DataService extends Service implements onDataLoaded, onDataQueryCompleted {
    ///////////////// Observers /////////////////
    public static final ServiceReadyObserver observersServiceReady = new ServiceReadyObserver();
    public static final DataQueryCompletedObserver observersDataQueryCompleted = new DataQueryCompletedObserver();
    public static final DataQueryRefreshObserver observersStartStopRefresh = new DataQueryRefreshObserver();
    private static final String TAG = "DataService";
    private static final String PLUGIN_QUERY_ACTION = "oly.netpowerctrl.plugins.INetPwrCtrlPlugin";
    private static final String PAYLOAD_SERVICE_NAME = "SERVICE_NAME";
    private static final NetworkChangedBroadcastReceiver networkChangedListener = new NetworkChangedBroadcastReceiver();
    public static String service_shutdown_reason = "";
    ///////////////// Service start/stop /////////////////
    static private WeakHashMap<Object, Boolean> weakHashMap = new WeakHashMap<>();
    static private DataService mDiscoverService;
    ///////////////// Data /////////////////
    final public ExecutableCollection executables = new ExecutableCollection(this);
    final public CredentialsCollection credentials = new CredentialsCollection(this);
    final public GroupCollection groups = new GroupCollection(this);
    final public FavCollection favourites = new FavCollection(this);
    final public IOConnectionsCollection connections = new IOConnectionsCollection(this);
    private final DataLoadedObserver observersOnDataLoaded = new DataLoadedObserver();
    private final List<AbstractBasePlugin> plugins = new ArrayList<>();
    ///////////////// Other /////////////////
    private LoadStoreCollections loadStoreCollections = new LoadStoreCollections();
    private DeviceQuery deviceQuery = new DeviceQuery(this);
    private boolean notificationAfterNextRefresh;

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
            App.getMainThreadHandler().removeMessages(GuiThreadHandler.SERVICE_DELAYED_EXIT);
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
            App.getMainThreadHandler().sendEmptyMessageDelayed(GuiThreadHandler.SERVICE_DELAYED_EXIT, 2000);
        }
    }

    /**
     * Do not call this directly but use stopUseService.
     */
    public static void finishService() {
        if (mDiscoverService != null)
            mDiscoverService.stopSelf();
    }

    public static DataService getService() {
        return mDiscoverService;
    }

    public boolean isDataLoaded() {
        return observersOnDataLoaded.isDone();
    }

    /**
     * Called by the asynchronous loading process after loading is done.
     */
    public void setDataLoadingCompleted() {
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
            App.getMainThreadHandler().sendEmptyMessageDelayed(GuiThreadHandler.SERVICE_DELAYED_EXIT, 2000);
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
        // If the service is already running, just check the alarms
        if (mDiscoverService != null) {
            return super.onStartCommand(intent, flags, startId);
        }

        if (weakHashMap.size() == 0) {
            Logging.getInstance().logMain("ILLEGAL START");
            service_shutdown_reason = "illegal start";
            stopSelf();
            Log.w(TAG, "cannot be started without useService");
            return START_NOT_STICKY;
        } else {
            Logging.getInstance().logMain("START");
        }

        // We are a singleton, set the instance variable now.
        mDiscoverService = this;

        observersDataQueryCompleted.register(this);

        // We want to get notified if data has been loaded
        observersOnDataLoaded.register(this);

        discoverExtensions();
        plugins.add(new AnelPlugin(this));
        plugins.add(new SimpleUDPPlugin(this));

        // Load all Devices, Scenes etc from disk.
        /**
         * This will call setLoadStoreController(..) with a default LoadStoreData object if no such object exist so far.
         * If data is already loaded or is loading is in progress, nothing will happen.
         */
        loadStoreCollections.loadData(this);

        networkChangedListener.registerReceiver(this);

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
        networkChangedListener.unregister(this);

        observersServiceReady.onServiceFinished(this);

        loadStoreCollections.finish(this);
        loadStoreCollections = null;

        UDPSend.killSendThread();

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

    private void interconnect_collection_objects() {

        // Connect credentials with plugins
        for (AbstractBasePlugin newlyAddedPlugin : plugins) {
            for (Credentials credentials : this.credentials.getItems().values()) {
                if (credentials.getPlugin() != newlyAddedPlugin && credentials.pluginID.equals(newlyAddedPlugin.getPluginID())) {
                    credentials.setPlugin(newlyAddedPlugin);
                }
            }
        }
        // Check all credentials for a plugin and set up IOConnections
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
        connections.removeOrphaned();
        executables.removeOrphaned();

        for (AbstractBasePlugin newlyAddedPlugin : plugins)
            newlyAddedPlugin.onStart(this);
    }

    @Override
    public boolean onDataLoaded() {
        interconnect_collection_objects();
        observersServiceReady.onServiceReady(this);
        refreshExistingDevices();
        return false;
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
        this.credentials.put(credentials);
        connections.save(credentials.deviceUID);
    }

    public void showNotificationForNextRefresh(boolean notificationAfterNextRefresh) {
        this.notificationAfterNextRefresh = notificationAfterNextRefresh;
    }

    @Override
    public boolean onDataQueryFinished(DataService dataService) {
        if (notificationAfterNextRefresh) {
            notificationAfterNextRefresh = false;
            // Show notification 500ms later, to also aggregate new devices for the message
            App.getMainThreadHandler().sendEmptyMessageDelayed(GuiThreadHandler.SERVICE_SHOW_DETECTED_MESSAGE, 500);
        }
        return true;
    }

    /**
     * Refreshes given device.
     */
    public void refreshExistingDevice(Credentials credentials, DevicesObserver.onDevicesObserverFinished callback) {
        deviceQuery.addDeviceObserver(new DevicesObserver(credentials, callback));
    }

    /**
     * Refreshes all configured devices.
     */
    public void refreshExistingDevices() {
        if (observersStartStopRefresh.isRefreshing()) return;
        observersStartStopRefresh.onRefreshStateChanged(true);
        deviceQuery.addDeviceObserver(new DevicesObserver(credentials.getItems().values(), new DevicesObserver.onDevicesObserverFinished() {
            @Override
            public void onObserverJobFinished(DevicesObserver devicesObserver) {
                Logging.getInstance().logEnergy("...fertig\n" + " Timeout: " + String.valueOf(devicesObserver.timedOutDevices().size()));
                observersDataQueryCompleted.onDataQueryFinished(DataService.this);
                observersStartStopRefresh.onRefreshStateChanged(false);
            }
        }));
    }

    /**
     * Refreshes all configured devices (detect unreachable devices) and detect new un-configured devices.
     */
    public void detectDevices() {
        if (observersStartStopRefresh.isRefreshing()) return;
        observersStartStopRefresh.onRefreshStateChanged(true);
        deviceQuery.addDeviceObserver(new DevicesObserver(new DevicesObserver.onDevicesObserverFinished() {
            @Override
            public void onObserverJobFinished(DevicesObserver devicesObserver) {
                Logging.getInstance().logEnergy("...fertig\n" + " Timeout: " + String.valueOf(devicesObserver.timedOutDevices().size()));
                observersDataQueryCompleted.onDataQueryFinished(DataService.this);
                observersStartStopRefresh.onRefreshStateChanged(false);
            }
        }));
        connections.clearNotConfigured();
        discoverExtensions();
        for (AbstractBasePlugin abstractBasePlugin : plugins) {
            abstractBasePlugin.requestData();
        }
    }

    /**
     * This is issued by the network observer if no network connection is active (no wlan, no mobile network)
     */
    public void makeAllOffline() {
        connections.clearNotConfigured();
        connections.applyStateToAll(ReachabilityStates.NotReachable);
    }

    public void remove(Credentials credentials) {
        this.executables.remove(credentials);
        this.connections.remove(credentials);
        this.credentials.remove(credentials);
    }
}
