package oly.netpowerctrl.data;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import oly.netpowerctrl.R;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DeviceConnection;
import oly.netpowerctrl.device_base.device.DeviceConnectionUDP;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.device_base.executables.Executable;
import oly.netpowerctrl.devices.DeviceCollection;
import oly.netpowerctrl.devices.UnconfiguredDeviceCollection;
import oly.netpowerctrl.groups.GroupCollection;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.network.DeviceObserverBase;
import oly.netpowerctrl.network.DeviceQuery;
import oly.netpowerctrl.network.onDeviceObserverResult;
import oly.netpowerctrl.network.onExecutionFinished;
import oly.netpowerctrl.network.onHttpRequestResult;
import oly.netpowerctrl.pluginservice.PluginInterface;
import oly.netpowerctrl.pluginservice.PluginService;
import oly.netpowerctrl.pluginservice.onPluginsReady;
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
public class AppData {
    // Observers are static for this singleton class
    public static final DataQueryCompletedObserver observersDataQueryCompleted = new DataQueryCompletedObserver();
    public static final DataQueryRefreshObserver observersStartStopRefresh = new DataQueryRefreshObserver();
    public static final DataLoadedObserver observersOnDataLoaded = new DataLoadedObserver();
    /**
     * Don't use updateDevice from another thread, but AppData.getInstance().updateDeviceFromOtherThread();
     */
    public static final int UPDATE_MESSAGE_NEW_DEVICE = 0;
    public static final int UPDATE_MESSAGE_EXISTING_DEVICE = 1;
    public static final int UPDATE_MESSAGE_BROKEN_DEVICE = 2;
    public static final int UPDATE_MESSAGE_ADD_DEVICE = 3;
    public android.os.Handler updateDeviceHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_MESSAGE_NEW_DEVICE:
                    updateDevice((Device) msg.obj);
                    break;
                case UPDATE_MESSAGE_EXISTING_DEVICE:
                    updateExistingDevice((Device) msg.obj);
                    break;
                case UPDATE_MESSAGE_BROKEN_DEVICE:
                    String[] d = (String[]) msg.obj;
                    onDeviceErrorByName(d[0], d[1]);
                    break;
                case UPDATE_MESSAGE_ADD_DEVICE:
                    addToConfiguredDevices((Device) msg.obj);
                    break;
            }
        }
    };
    private static final String TAG = "AppData";
    static int findDevicesRun = 0;
    final public DeviceCollection deviceCollection = new DeviceCollection();
    final public UnconfiguredDeviceCollection unconfiguredDeviceCollection = new UnconfiguredDeviceCollection();
    final public GroupCollection groupCollection = new GroupCollection();
    final public SceneCollection sceneCollection = new SceneCollection();
    final public FavCollection favCollection = new FavCollection();
    final public TimerCollection timerCollection = new TimerCollection();
    private final List<DeviceObserverBase> updateDeviceStateList = new ArrayList<>();
    private LoadStoreJSonData loadStoreJSonData = null;

    /**
     * Detect new devices and check reach-ability of configured devices.
     */
    private boolean isDetecting = false;
    private Handler restrictDetectingHandler = new Handler() {
        public void handleMessage(Message msg) {
            isDetecting = false;
        }
    };
    private onPluginsReady refreshAfterPluginsReady = new onPluginsReady() {
        @Override
        public boolean onPluginsReady() {
            isDetecting = false;
            refreshDeviceData(false);
            return false;
        }
    };
    private int temp_success, temp_errors;

    private AppData() {
        deviceCollection.registerObserver(sceneCollection.deviceObserver);
        deviceCollection.registerObserver(timerCollection.deviceObserver);
    }

    public static AppData getInstance() {
        return SingletonHolder.instance;
    }

    /**
     * This will call useAppData(..) with a default LoadStoreData object if no such object exist so far.
     * If data is loaded or is loading nothing will happen.
     */
    public static void useAppData() {
        AppData appData = getInstance();
        if (appData.loadStoreJSonData != null)
            return;

        appData.loadStoreJSonData = new LoadStoreJSonData();
        appData.loadStoreJSonData.loadData(appData);
    }

    public static boolean isDataLoaded() {
        return observersOnDataLoaded.dataLoaded;
    }

    static public boolean isNetworkDevice(Device device) {
        PluginInterface pi = (PluginInterface) device.getPluginInterface();
        return pi != null && pi.isNetworkPlugin();
    }

    public LoadStoreJSonData getLoadStoreJSonData() {
        return loadStoreJSonData;
    }

    /**
     * Load all devices, scenes, groups, timers. This is called by useAppData.
     * Loading of data is asynchronous and done in a background thread. Do no assume data to be
     * loaded after this method returns! Use the observersOnDataLoaded object to be notified.
     *
     * @param loadStoreJSonData The object that is responsible for loading the data.
     */
    public void useAppData(LoadStoreJSonData loadStoreJSonData) {
        assert loadStoreJSonData != null;
        if (this.loadStoreJSonData != null)
            this.loadStoreJSonData.finish();
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
     * Remove a DeviceObserver from the global list of DeviceObservers.
     * This is not multi thread safe!
     *
     * @param deviceObserverBase The DeviceObserver
     */
    public void removeUpdateDeviceState(DeviceObserverBase deviceObserverBase) {
        updateDeviceStateList.remove(deviceObserverBase);
    }

    /**
     * Add a DeviceObserver to the global list of DeviceObservers. The Observer
     * will be notified of all device updates until it is removed from the list again.
     * This is not multi thread safe!
     *
     * @param deviceObserverBase The DeviceObserver
     */
    public void addUpdateDeviceState(DeviceObserverBase deviceObserverBase) {
        updateDeviceStateList.add(deviceObserverBase);
    }

    /**
     * Notify observers who are using the DeviceQuery class
     *
     * @param source Source device that has been updated
     */
    private void notifyDeviceQueries(Device source) {
        List<DeviceObserverBase> deviceObserverBaseList = new ArrayList<>();

        Iterator<DeviceObserverBase> it = updateDeviceStateList.iterator();
        while (it.hasNext()) {
            // Return true if the DeviceQuery object has finished its task.
            DeviceObserverBase deviceObserverBase = it.next();
            if (deviceObserverBase.notifyObservers(source)) {
                it.remove();
                deviceObserverBaseList.add(deviceObserverBase);
            }
        }

        // Handle all finished device observers now. We have to split the finishing operation
        // and updateDeviceStateList iteration because finishWithTimeouts may cause that that this
        // method is called again and would lead to a concurrent access violation otherwise.
        for (DeviceObserverBase deviceObserverBase : deviceObserverBaseList) {
            deviceObserverBase.finishWithTimeouts();
        }
    }

    /**
     * Tidy up all lists and references.
     */
    public void clear() {
        // There shouldn't be any device-listen observers anymore,
        // but we clear the list here nevertheless.
        for (DeviceObserverBase dq : updateDeviceStateList)
            dq.finishWithTimeouts();
        updateDeviceStateList.clear();
        unconfiguredDeviceCollection.getItems().clear();
        deviceCollection.getItems().clear();
        sceneCollection.getItems().clear();
        groupCollection.getItems().clear();
        timerCollection.getItems().clear();
        observersOnDataLoaded.dataLoaded = false;
    }

    public void clearNewDevices() {
        unconfiguredDeviceCollection.removeAll();
    }

    /**
     * Add the given device to the configured devices. Remove the device from the unconfigured device list
     * if necessary. Don't use addToConfiguredDevices from another thread,
     * but AppData.getInstance().addToConfiguredDevicesFromOtherThread();
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
        if (deviceCollection.add(device)) {
            // An existing device has been replaced. Do nothing else here.
            return;
        }

        // Initiate detect devices, if this added device is not flagged as reachable at the moment.
        if (device.getFirstReachableConnection() == null)
            new DeviceQuery(App.instance, null, device);
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
        updateDeviceHandler.obtainMessage(UPDATE_MESSAGE_NEW_DEVICE, device).sendToTarget();
    }

    public void updateExistingDeviceFromOtherThread(final Device device) {
        updateDeviceHandler.obtainMessage(UPDATE_MESSAGE_EXISTING_DEVICE, device).sendToTarget();
    }

    /**
     * Call this by your plugin if a device changed. This method is also called
     * by the DeviceQuery class if devices are not reachable anymore.
     *
     * @param updated_device_info The changed device
     */
    public void updateDevice(Device updated_device_info) {
        // if it matches a configured device, update it's outlet states and exit the method.
        int position = deviceCollection.getPosition(updated_device_info);

        if (position == -1) {
            updated_device_info.setConfigured(false);
            // notify observers who are using the DeviceQuery class (new device)
            notifyDeviceQueries(updated_device_info);
            unconfiguredDeviceCollection.add(updated_device_info);
            return;
        }

        Device existing_device = deviceCollection.items.get(position);
        if (existing_device == updated_device_info) {
            throw new RuntimeException("Same object not allowed here: " + updated_device_info.getDeviceName());
        }

        existing_device.lockDevice();
        existing_device.replaceAutomaticAssignedConnections(updated_device_info.getDeviceConnections());
        existing_device.copyValuesFromUpdated(updated_device_info);
        existing_device.releaseDevice();

        // notify observers who are using the DeviceQuery class (existing device)
        notifyDeviceQueries(existing_device);

        int flag = existing_device.getAndClearChangedFlag();
        if (flag != 0) notifyAfterUpdate(existing_device, position, flag);
    }

    /**
     * Call this if you have made your changes to the given device and want to propagate those now.
     *
     * @param existing_device The existing device (Device has to be an object within deviceCollection!)
     */
    public void updateExistingDevice(Device existing_device) {
        notifyDeviceQueries(existing_device);

        int flag = existing_device.getAndClearChangedFlag();
        if (flag != 0)
            notifyAfterUpdate(existing_device, deviceCollection.getPosition(existing_device), flag);
    }

    private void notifyAfterUpdate(Device existing_device, int position, int flag) {
        Log.w(TAG, "device " + existing_device.getDeviceName() + " " + String.valueOf(flag));

        if ((flag & Device.CHANGE_DEVICE) != 0) {
            deviceCollection.save(existing_device);
            deviceCollection.notifyObservers(existing_device, ObserverUpdateActions.UpdateAction, position);
        } else if ((flag & Device.CHANGE_DEVICE_REACHABILITY) != 0) {
            deviceCollection.notifyObservers(existing_device, ObserverUpdateActions.UpdateAction, position);
        } else if ((flag & Device.CHANGE_CONNECTION_REACHABILITY) != 0) {
            deviceCollection.notifyObservers(existing_device, ObserverUpdateActions.ConnectionUpdateAction, position);
        }

        if (!existing_device.isReachable() && SharedPrefs.getInstance().notifyDeviceNotReachable()) {
            long current_time = System.currentTimeMillis();
            Toast.makeText(App.instance,
                    App.getAppString(R.string.error_setting_outlet, existing_device.getDeviceName(),
                            (int) ((current_time - existing_device.getUpdatedTime()) / 1000)),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    public void onDeviceErrorByName(String name, String errMessage) {
        // notify observers who are using the DeviceQuery class
        Iterator<DeviceObserverBase> it = updateDeviceStateList.iterator();
        while (it.hasNext()) {
            // Return true if the DeviceQuery object has finished its task.
            DeviceObserverBase deviceObserverBase = it.next();
            if (deviceObserverBase.notifyObservers(name)) {
                it.remove();
                deviceObserverBase.finishWithTimeouts();
            }
        }

        // error packet received
        String error = App.getAppString(R.string.error_packet_received) + ": " + errMessage;
        InAppNotifications.FromOtherThread(App.instance, error);
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

        PluginService.getService().wakeupPlugin(port.device);

        PluginInterface remote = (PluginInterface) port.device.getPluginInterface();
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
        List<PluginInterface> pluginInterfaces = new ArrayList<>();
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

            PluginInterface remote = (PluginInterface) devicePort.device.getPluginInterface();
            if (remote == null) {
                Log.e(TAG, "Execute scene, PluginInterface not found " + item.uuid);
                continue;
            }

            int command = item.command;
            // Replace toggle by master command if master is set
            if (master_command != DevicePort.INVALID && item.command == DevicePort.TOGGLE)
                command = master_command;

            remote.addToTransaction(devicePort, command);
            if (!pluginInterfaces.contains(remote))
                pluginInterfaces.add(remote);
        }

        for (Device device : deviceSet) {
            PluginService.getService().wakeupPlugin(device);
        }

        temp_success = 0;
        temp_errors = 0;
        final int finalCountValidSceneItems = countValidSceneItems;
        for (PluginInterface p : pluginInterfaces) {
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
            PluginInterface remote = (PluginInterface) devicePort.device.getPluginInterface();
            if (remote != null) {
                PluginService.getService().wakeupPlugin(devicePort.device);
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
        PluginInterface remote = (PluginInterface) devicePort.device.getPluginInterface();
        if (remote != null) {
            PluginService.getService().wakeupPlugin(devicePort.device);
            remote.execute(devicePort, command, callback);
            if (callback != null) callback.onExecutionProgress(1, 0, 1);
        } else {
            if (callback != null) callback.onExecutionProgress(0, 1, 1);
        }
    }

    public int countNetworkDevices() {
        int i = 0;
        for (Device di : deviceCollection.items)
            if (isNetworkDevice(di)) ++i;
        return i;
    }

    public List<Device> findDevices(PluginInterface pluginInterface) {
        List<Device> list = new ArrayList<>();
        for (Device di : deviceCollection.items) {
            // Mark all devices as changed: If network reduced mode ends all
            // devices propagate changes then.
            if (pluginInterface.equals(di.getPluginInterface())) {
                list.add(di);
            }
        }
        return list;
    }

    public void refreshDeviceData(final boolean refreshKnownExtensions) {
        // The following mechanism allows only one update request within a
        // 1sec timeframe.
        if (isDetecting)
            return;
        isDetecting = true;

        if (!PluginService.observersPluginsReady.isLoaded()) {
            PluginService.observersPluginsReady.register(refreshAfterPluginsReady);
            return;
        } else
            restrictDetectingHandler.sendEmptyMessageDelayed(0, 1000);

        final int currentRun = ++findDevicesRun;

        final long startTime = System.nanoTime();
        Logging.getInstance().logEnergy("Suche Geräte\n" + String.valueOf((System.nanoTime() - startTime) / 1000000.0) + " " + String.valueOf(currentRun));

        observersStartStopRefresh.onRefreshStateChanged(true);

        if (refreshKnownExtensions)
            PluginService.getService().discoverExtensions();

        clearNewDevices();
        new DeviceQuery(App.instance, new onDeviceObserverResult() {
            @Override
            public void onObserverDeviceUpdated(Device di) {
            }

            @Override
            public void onObserverJobFinished(List<Device> timeout_devices) {
                Logging.getInstance().logEnergy("Suche Geräte fertig\n" + String.valueOf((System.nanoTime() - startTime) / 1000000.0) + " Timeout: " + String.valueOf(timeout_devices.size()));
                observersDataQueryCompleted.onDataQueryFinished(timeout_devices.size() == countNetworkDevices());
                observersStartStopRefresh.onRefreshStateChanged(false);
            }
        });
    }

    private static class SingletonHolder {
        public static final AppData instance = new AppData();
    }
}
