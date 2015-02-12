package oly.netpowerctrl.data;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import oly.netpowerctrl.R;
import oly.netpowerctrl.consistency_tests.device_tests;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DeviceConnection;
import oly.netpowerctrl.device_base.device.DeviceConnectionUDP;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.device_base.executables.Executable;
import oly.netpowerctrl.devices.DeviceCollection;
import oly.netpowerctrl.devices.UnconfiguredDeviceCollection;
import oly.netpowerctrl.groups.GroupCollection;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.network.onDeviceObserverResult;
import oly.netpowerctrl.network.onExecutionFinished;
import oly.netpowerctrl.network.onHttpRequestResult;
import oly.netpowerctrl.pluginservice.AbstractBasePlugin;
import oly.netpowerctrl.pluginservice.DeviceObserverBase;
import oly.netpowerctrl.pluginservice.DeviceQuery;
import oly.netpowerctrl.pluginservice.PluginService;
import oly.netpowerctrl.scenes.Scene;
import oly.netpowerctrl.scenes.SceneCollection;
import oly.netpowerctrl.scenes.SceneItem;
import oly.netpowerctrl.timer.TimerCollection;
import oly.netpowerctrl.ui.notifications.InAppNotifications;
import oly.netpowerctrl.utils.Logging;

/**
 * Device Updates go into this object and are propagated to all observers and the device collection.
 * All data collections are centralized in this class. Helper methods regarding configured devices
 * are implemented in this class. Those are: executeToggle, rename, countReachable, countNetworkDevices.
 * This class is a singleton (Initialization on Demand Holder).
 */
public class AppData implements onDataQueryCompleted {
    // Observers are static for this singleton class
    public static final DataQueryCompletedObserver observersDataQueryCompleted = new DataQueryCompletedObserver();
    public static final DataQueryRefreshObserver observersStartStopRefresh = new DataQueryRefreshObserver();
    /**
     * Don't use updateDevice from another thread, but appData.updateDeviceFromOtherThread();
     */
    public static final int UPDATE_MESSAGE_DEVICE = 1;
    public static final int UPDATE_MESSAGE_ADD_DEVICE = 3;
    public android.os.Handler updateDeviceHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            Device device = (Device) msg.obj;
            switch (msg.what) {
                case UPDATE_MESSAGE_DEVICE:
                    updateDevice(device, true);
                    break;
                case UPDATE_MESSAGE_ADD_DEVICE:
                    addToConfiguredDevices(device);
                    break;
            }
        }
    };
    private static final DataLoadedObserver observersOnDataLoaded = new DataLoadedObserver();
    private static final String TAG = "AppData";
    static int findDevicesRun = 0;
    final public DeviceCollection deviceCollection = new DeviceCollection(this);
    final public UnconfiguredDeviceCollection unconfiguredDeviceCollection = new UnconfiguredDeviceCollection(this);
    final public GroupCollection groupCollection = new GroupCollection(this);
    final public SceneCollection sceneCollection = new SceneCollection(this);
    final public FavCollection favCollection = new FavCollection(this);
    final public TimerCollection timerCollection = new TimerCollection(this);
    private LoadStoreJSonData loadStoreJSonData = new LoadStoreJSonData();
    /**
     * Detect new devices and check reach-ability of configured devices.
     */
    private boolean isDetecting = false;
    private Handler restrictDetectingHandler = new Handler() {
        public void handleMessage(Message msg) {
            isDetecting = false;
        }
    };

    private int temp_success, temp_errors;
    private boolean notificationAfterNextRefresh;
    private DataQueryFinishedMessageRunnable afterDataQueryFinishedHandler = new DataQueryFinishedMessageRunnable(this);

    public AppData() {
        observersDataQueryCompleted.register(this);
    }

    public static void addServiceToDataLoadedObserver(PluginService service) {
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

    public void onDestroy() {
        loadStoreJSonData.finish(this);
        loadStoreJSonData = null;
    }

    /**
     * This will call setLoadStoreController(..) with a default LoadStoreData object if no such object exist so far.
     * If data is loaded or is loading nothing will happen.
     */
    public void loadData() {
        loadStoreJSonData.loadData(this);
    }

    public LoadStoreJSonData getLoadStoreJSonData() {
        return loadStoreJSonData;
    }

    /**
     * Load all devices, scenes, groups, timers. This is called by setLoadStoreController.
     * Loading of data is asynchronous and done in a background thread. Do no assume data to be
     * loaded after this method returns! Use the observersOnDataLoaded object to be notified.
     *
     * @param loadStoreJSonData The object that is responsible for loading the data.
     */
    public void setLoadStoreController(LoadStoreJSonData loadStoreJSonData) {
        assert loadStoreJSonData != null;
        if (this.loadStoreJSonData != null)
            this.loadStoreJSonData.finish(this);
        this.loadStoreJSonData = loadStoreJSonData;
        loadStoreJSonData.loadData(this);
    }

    //! get a list of all send ports of all configured devices plus the default send port
    public Set<Integer> getAllSendPorts() {
        HashSet<Integer> ports = new HashSet<>();
        ports.add(SharedPrefs.getInstance().getDefaultSendPort());

        List<Device> backup_list = new ArrayList<>(deviceCollection.items);
        for (Device di : backup_list) {
            di.lockDevice();
            for (DeviceConnection ci : di.getDeviceConnections())
                if (ci instanceof DeviceConnectionUDP)
                    ports.add(ci.getDestinationPort());
            di.releaseDevice();
        }
        return ports;
    }

    //! get a list of all receive ports of all configured devices plus the default receive port
    public Set<Integer> getAllReceivePorts() {
        HashSet<Integer> ports = new HashSet<>();
        ports.add(SharedPrefs.getInstance().getDefaultReceivePort());

        List<Device> backup_list = new ArrayList<>(deviceCollection.items);
        for (Device di : backup_list) {
            di.lockDevice();
            for (DeviceConnection ci : di.getDeviceConnections())
                if (ci instanceof DeviceConnectionUDP)
                    ports.add(((DeviceConnectionUDP) ci).getListenPort());
            di.releaseDevice();
        }
        return ports;
    }

    /**
     * Tidy up all lists and references.
     */
    public void clear() {
        // There shouldn't be any device-listen observers anymore,
        // but we clear the list here nevertheless.
        DeviceObserverBase.finishAll();
        unconfiguredDeviceCollection.getItems().clear();
        deviceCollection.getItems().clear();
        sceneCollection.getItems().clear();
        groupCollection.getItems().clear();
        timerCollection.getItems().clear();
        observersOnDataLoaded.reset();
    }

    public void clearNewDevices() {
        unconfiguredDeviceCollection.removeAll();
    }

    /**
     * Add the given device to the configured devices. Remove the device from the unconfigured device list
     * if necessary. Don't use addToConfiguredDevices from another thread,
     * but appData.addToConfiguredDevicesFromOtherThread();
     *
     * @param device
     */
    public void addToConfiguredDevices(Device device) {
        if (device.getUniqueDeviceID() == null) {
            InAppNotifications.showException(App.instance, null, "addToConfiguredDevices. Failed to add device: no unique id!");
            return;
        }

        unconfiguredDeviceCollection.remove(device);

        device.setConfigured(true);
        // This will also safe the new device!
        deviceCollection.add(device);
    }

    public void addToConfiguredDevicesFromOtherThread(Device device) {
        updateDeviceHandler.obtainMessage(UPDATE_MESSAGE_ADD_DEVICE, device).sendToTarget();
    }

    /**
     * Call this by your plugin if a device changed and you are on another thread
     *
     * @param device The changed device
     */
    public void updateDeviceFromOtherThread(final Device device) {
        updateDeviceHandler.obtainMessage(UPDATE_MESSAGE_DEVICE, device).sendToTarget();
    }

    /**
     * Call this if you have made your changes to the given device and want to propagate those now.
     *
     * @param device_info           Either an existing device (Device has to be an object within deviceCollection!)
     *                              or a new device info or a device that will update one of the deviceCollection devices.
     * @param notifyDeviceObservers Usually you want to inform also the DeviceQueries about updates. This must be false
     *                              if you call updateDevice from a DeviceObserverBase or DeviceQuery object!
     */
    public void updateDevice(Device device_info, boolean notifyDeviceObservers) {
        int position = deviceCollection.getPosition(device_info);

        if (device_info.isConfigured() ^ (position != -1))
            throw new RuntimeException("Inconsistent device state: Configured: " + String.valueOf(device_info.isConfigured()) + " position: " + String.valueOf(position));

        if (position == -1) {
            unconfiguredDeviceCollection.add(device_info);
        } else {
            device_tests.test_check_adapters();

            Device existing_device = deviceCollection.items.get(position);
            if (existing_device != device_info) {
                existing_device.lockDevice();
                existing_device.replaceAutomaticAssignedConnections(device_info.getDeviceConnections());
                existing_device.copyValuesFromUpdated(device_info);
                device_tests.test_connection_reachable_consistency(existing_device);
                existing_device.releaseDevice();
                device_info = existing_device;
            }

            @Device.ChangesFlag
            int flag = device_info.getAndClearChangedFlag();
            if (flag != 0)
                deviceCollection.notifyObservers(device_info, ObserverUpdateActions.UpdateAction, position);
            if (flag == Device.CHANGED_DEVICE_WILL_STORE)
                deviceCollection.save(device_info);
        }

        // notify observers who are using the DeviceQuery class
        if (notifyDeviceObservers) DeviceObserverBase.notifyOfUpdatedDevice(device_info);

        device_tests.test_check_adapters();
    }


    public int getReachableConfiguredDevices() {
        int r = 0;
        for (Device device : deviceCollection.items)
            if (device.getFirstReachableConnection() != null)
                ++r;
        return r;
    }

    /**
     * Search for an executable that may be a scene or a devicePort. This is an expensive operation!
     *
     * @param executable_uid The uid
     * @return Return a scene, a devicePort or null.
     */
    public Executable findExecutable(String executable_uid) {
        Executable executable = findDevicePort(executable_uid);

        if (executable == null) {
            if (executable_uid == null)
                return null;
            return sceneCollection.findScene(executable_uid);
        } else
            return executable;
    }

    @Nullable
    public Executable findFirstExecutableByName(@NonNull String executable_title, boolean unprecise) {
        for (Device di : deviceCollection.items) {
            di.lockDevicePorts();
            Iterator<DevicePort> it = di.getDevicePortIterator();
            while (it.hasNext()) {
                DevicePort port = it.next();
                if (!unprecise) {
                    if (executable_title.contains(port.getTitle().toLowerCase())) {
                        di.releaseDevicePorts();
                        return port;
                    }
                } else {
                    String[] strings = port.getTitle().toLowerCase().split("\\W+");
                    for (String string : strings) {
                        if (executable_title.contains(string)) {
                            di.releaseDevicePorts();
                            return port;
                        }
                    }
                }
            }
            di.releaseDevicePorts();
        }

        for (Scene scene : sceneCollection.items)
            if (!unprecise) {
                if (executable_title.contains(scene.getTitle().toLowerCase()))
                    return scene;
            } else {
                String[] strings = scene.getTitle().toLowerCase().split("\\W+");
                for (String string : strings) {
                    if (executable_title.contains(string)) {
                        return scene;
                    }
                }
            }

        return null;
    }

    @Nullable
    public DevicePort findDevicePort(@Nullable String uuid) {
        if (uuid == null)
            return null;

        for (Device di : deviceCollection.items) {
            di.lockDevicePorts();
            Iterator<DevicePort> it = di.getDevicePortIterator();
            while (it.hasNext()) {
                DevicePort port = it.next();
                if (port.getUid().equals(uuid)) {
                    di.releaseDevicePorts();
                    return port;
                }
            }
            di.releaseDevicePorts();
        }
        return null;
    }

    @Nullable
    public Device findDevice(@NonNull String uniqueID) {
        for (Device di : deviceCollection.items) {
            if (di.getUniqueDeviceID().equals(uniqueID)) {
                return di;
            }
        }
        return null;
    }

    @Nullable
    public Device findDeviceUnconfigured(@NonNull String uniqueID) {
        for (Device di : unconfiguredDeviceCollection.items) {
            if (di.getUniqueDeviceID().equals(uniqueID)) {
                return di;
            }
        }
        return null;
    }

    public void rename(@NonNull DevicePort port, String new_name, onHttpRequestResult callback) {
        if (callback != null)
            callback.httpRequestStart(port);

        AbstractBasePlugin remote = (AbstractBasePlugin) port.device.getPluginInterface();
        if (remote != null) {
            remote.rename(port, new_name, callback);
        } else if (callback != null)
            callback.httpRequestResult(port, false, App.getAppString(R.string.error_plugin_not_installed));
    }

    /**
     * Notice: Only call this method if the NetpowerctrlService service is running!
     *
     * @param scene    The scene to executeToggle
     * @param callback The callback for the execution-done messages
     */
    public void execute(@NonNull Scene scene, final onExecutionFinished callback) {
        List<AbstractBasePlugin> abstractBasePlugins = new ArrayList<>();
        Set<Device> deviceSet = new TreeSet<>();

        // Master/Slave
        SceneItem masterItem = scene.getMasterSceneItem();
        int master_command = DevicePort.INVALID;
        if (masterItem != null) {
            // If the command is not toggle, we return it now. It can be applied to slaves
            // directly.
            if (masterItem.command != DevicePort.TOGGLE) {
                master_command = masterItem.command;
            } else {
                // If the command is toggle, we have to find out the final command.
                DevicePort port = findDevicePort(masterItem.uuid);
                if (port == null)
                    master_command = DevicePort.INVALID;
                else
                    master_command = port.getCurrentValueToggled();
            }
        }

        int countValidSceneItems = 0;
        for (SceneItem item : scene.sceneItems) {
            DevicePort devicePort = findDevicePort(item.uuid);
            if (devicePort == null) {
                Log.e(TAG, "Execute scene, DevicePort not found " + item.uuid);
                continue;
            }

            ++countValidSceneItems;
            deviceSet.add(devicePort.device);

            AbstractBasePlugin remote = (AbstractBasePlugin) devicePort.device.getPluginInterface();
            if (remote == null) {
                Log.e(TAG, "Execute scene, PluginInterface not found " + item.uuid);
                continue;
            }

            int command = item.command;
            // Replace toggle by master command if master is set
            if (master_command != DevicePort.INVALID && item.command == DevicePort.TOGGLE)
                command = master_command;

            remote.addToTransaction(devicePort, command);
            if (!abstractBasePlugins.contains(remote))
                abstractBasePlugins.add(remote);
        }

        temp_success = 0;
        temp_errors = 0;
        final int finalCountValidSceneItems = countValidSceneItems;
        for (AbstractBasePlugin p : abstractBasePlugins) {
            p.executeTransaction(new onExecutionFinished() {
                @Override
                public void onExecutionProgress(int success, int errors, int all) {
                    temp_success += success;
                    temp_errors += errors;
                    if (callback != null)
                        callback.onExecutionProgress(temp_success, temp_errors, finalCountValidSceneItems);
                }
            });
        }
    }

    /**
     * Notice: Only call this method if the NetpowerctrlService service is running!
     *
     * @param executable A scene or a device port
     * @param callback   The callback for the execution-done messages
     */
    public void executeToggle(@NonNull final Executable executable, final onExecutionFinished callback) {
        if (executable instanceof Scene) {
            execute(((Scene) executable), callback);
        } else if (executable instanceof DevicePort) {
            DevicePort devicePort = (DevicePort) executable;
            AbstractBasePlugin remote = (AbstractBasePlugin) devicePort.device.getPluginInterface();
            if (remote != null) {
                remote.execute(devicePort, DevicePort.TOGGLE, callback);
            }

            if (callback != null)
                callback.onExecutionProgress(1, 0, 1);
        } else {
            if (callback != null)
                callback.onExecutionProgress(0, 1, 1);
        }
    }

    /**
     * Notice: Only call this method if the NetpowerctrlService service is running!
     *
     * @param devicePort The device port
     * @param command    The command to executeToggle
     * @param callback   The callback for the execution-done messages
     */
    public void execute(@NonNull final DevicePort devicePort, final int command, final onExecutionFinished callback) {
        AbstractBasePlugin remote = (AbstractBasePlugin) devicePort.device.getPluginInterface();
        if (remote != null) {
            remote.execute(devicePort, command, callback);
            if (callback != null) callback.onExecutionProgress(1, 0, 1);
        } else {
            if (callback != null) callback.onExecutionProgress(0, 1, 1);
        }
    }

    public List<Device> findDevices(AbstractBasePlugin abstractBasePlugin) {
        List<Device> list = new ArrayList<>();
        for (Device di : deviceCollection.items) {
            // Mark all devices as changed: If network reduced mode ends all
            // devices propagate changes then.
            if (abstractBasePlugin.equals(di.getPluginInterface())) {
                list.add(di);
            }
        }
        return list;
    }

    public void showNotificationForNextRefresh(boolean notificationAfterNextRefresh) {
        this.notificationAfterNextRefresh = notificationAfterNextRefresh;
    }

    @Override
    public boolean onDataQueryFinished(AppData appData) {
        if (notificationAfterNextRefresh) {
            notificationAfterNextRefresh = false;
            // Show notification 500ms later, to also aggregate new devices for the message
            DataQueryFinishedMessageRunnable.show(afterDataQueryFinishedHandler);
        }
        return true;
    }

    public void refreshDeviceData(PluginService pluginService, final boolean refreshKnownExtensions) {
        // The following mechanism allows only one update request within a
        // 1sec timeframe.
        if (isDetecting)
            return;
        isDetecting = true;

        if (!pluginService.isPluginsLoaded()) {
            throw new RuntimeException("refreshDeviceData before Plugins are ready");
        } else
            restrictDetectingHandler.sendEmptyMessageDelayed(0, 1000);

        final int currentRun = ++findDevicesRun;

        final long startTime = System.nanoTime();
        Logging.getInstance().logEnergy("Suche Geräte\n" + String.valueOf((System.nanoTime() - startTime) / 1000000.0) + " " + String.valueOf(currentRun));

        observersStartStopRefresh.onRefreshStateChanged(true);

        if (refreshKnownExtensions)
            pluginService.discoverExtensions();

        clearNewDevices();
        new DeviceQuery(pluginService, new onDeviceObserverResult() {
            @Override
            public void onObserverJobFinished(DeviceObserverBase deviceObserverBase) {
                timerCollection.checkAlarm(PluginService.getService().alarmStartedTime());
                Logging.getInstance().logEnergy("Suche Geräte fertig\n" + String.valueOf((System.nanoTime() - startTime) / 1000000.0) + " Timeout: " + String.valueOf(deviceObserverBase.timedOutDevices().size()));
                observersDataQueryCompleted.onDataQueryFinished(AppData.this);
                observersStartStopRefresh.onRefreshStateChanged(false);
            }
        }, deviceCollection.getItems().iterator(), true);
    }
}
