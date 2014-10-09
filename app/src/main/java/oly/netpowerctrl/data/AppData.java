package oly.netpowerctrl.data;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.device_ports.DevicePort;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.devices.DeviceCollection;
import oly.netpowerctrl.devices.DeviceConnection;
import oly.netpowerctrl.devices.DeviceConnectionUDP;
import oly.netpowerctrl.groups.GroupCollection;
import oly.netpowerctrl.listen_service.PluginInterface;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.network.DeviceObserverBase;
import oly.netpowerctrl.network.DeviceQuery;
import oly.netpowerctrl.network.onExecutionFinished;
import oly.netpowerctrl.network.onHttpRequestResult;
import oly.netpowerctrl.scenes.Scene;
import oly.netpowerctrl.scenes.SceneCollection;
import oly.netpowerctrl.scenes.SceneItem;
import oly.netpowerctrl.timer.TimerController;
import oly.netpowerctrl.utils.notifications.InAppNotifications;

/**
 * Device Updates go into this object and are propagated to all observers and the device collection.
 * All data collections are centralized in this class. Helper methods regarding configured devices
 * are implemented in this class. Those are: execute, rename, countReachable, countNetworkDevices.
 * This class is a singleton (Initialization on Demand Holder).
 */
public class AppData {
    // Observers are static for this singleton class
    public static final DataQueryCompletedObserver observersDataQueryCompleted = new DataQueryCompletedObserver();
    public static final DataLoadedObserver observersOnDataLoaded = new DataLoadedObserver();
    public static final NewDeviceObserver observersNew = new NewDeviceObserver();
    private static final String TAG = "AppData";

    public final List<Device> newDevices = new ArrayList<>();
    final public DeviceCollection deviceCollection = new DeviceCollection();
    final public GroupCollection groupCollection = new GroupCollection();
    final public SceneCollection sceneCollection = new SceneCollection();
    final public TimerController timerController = new TimerController();
    private final List<DeviceObserverBase> updateDeviceStateList = new ArrayList<>();
    private LoadStoreJSonData loadStoreJSonData = null;

    private AppData() {
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
        LoadStoreIconData.init();
    }

    public static boolean isDataLoaded() {
        return observersOnDataLoaded.dataLoaded;
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

        for (Device di : deviceCollection.items)
            for (DeviceConnection ci : di.DeviceConnections)
                if (ci instanceof DeviceConnectionUDP)
                    ports.add(ci.getDestinationPort());

        return ports;
    }

    //! get a list of all receive ports of all configured devices plus the default receive port
    public Set<Integer> getAllReceivePorts() {
        HashSet<Integer> ports = new HashSet<>();
        ports.add(SharedPrefs.getInstance().getDefaultReceivePort());

        for (Device di : deviceCollection.items)
            for (DeviceConnection ci : di.DeviceConnections)
                if (ci instanceof DeviceConnectionUDP)
                    ports.add(((DeviceConnectionUDP) ci).getListenPort());

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
        newDevices.clear();
        deviceCollection.getItems().clear();
        sceneCollection.getItems().clear();
        groupCollection.getItems().clear();
        timerController.getItems().clear();
        observersOnDataLoaded.dataLoaded = false;
    }

    public void clearNewDevices() {
        newDevices.clear();
        observersNew.onNewDevice(null);
    }

    public void addToConfiguredDevices(Context context, Device device) {
        if (device.UniqueDeviceID == null) {
            InAppNotifications.showException(context, null, "addToConfiguredDevices. Failed to add device: no unique id!");
            return;
        }
        if (deviceCollection.add(device)) {
            // An existing device has been replaced. Do nothing else here.
            return;
        }

        // This device is now configured
        device.configured = true;

        // Remove from new devices list
        for (int i = 0; i < newDevices.size(); ++i) {
            if (newDevices.get(i).equalsByUniqueID(device)) {
                newDevices.remove(i);
                observersNew.onNewDevice(device);
                break;
            }
        }

        // Initiate detect devices, if this added device is not flagged as reachable at the moment.
        if (device.getFirstReachableConnection() == null)
            new DeviceQuery(context, null, device);
    }

    /**
     * Call this by your plugin if a device changed and you are on another thread
     *
     * @param device_info The changed device
     */
    public void onDeviceUpdatedOtherThread(final Device device_info) {
        App.getMainThreadHandler().post(new Runnable() {
            public void run() {
                onDeviceUpdated(device_info);
            }
        });
    }

    /**
     * Call this by your plugin if a device changed. This method is also called
     * by the DeviceQuery class if devices are not reachable anymore.
     *
     * @param device_info The changed device
     */
    public void onDeviceUpdated(Device device_info) {
        // if it matches a configured device, update it's outlet states and exit the method.
        Device existing_device = deviceCollection.update(device_info);

        if (existing_device != null) {
            // notify observers who are using the DeviceQuery class (existing device)
            notifyDeviceQueries(existing_device);
            return;
        }

        // notify observers who are using the DeviceQuery class (new device)
        notifyDeviceQueries(device_info);

        if (device_info.UniqueDeviceID != null) {
            // Do we have this new device already in the list?
            for (Device target : newDevices) {
                if (device_info.equalsByUniqueID(target))
                    return;
            }
            // No: Add device to new_device list
            newDevices.add(device_info);
            observersNew.onNewDevice(device_info);
        }
    }

    public void onDeviceErrorByName(Context context, String name, String errMessage) {
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
        String error = context.getString(R.string.error_packet_received) + ": " + errMessage;
        InAppNotifications.FromOtherThread(context, error);
    }

    public int getReachableConfiguredDevices() {
        int r = 0;
        for (Device device : deviceCollection.items)
            if (device.getFirstReachableConnection() != null)
                ++r;
        return r;
    }

    public DevicePort findDevicePort(UUID uuid) {
        if (uuid == null)
            return null;

        for (Device di : deviceCollection.items) {
            di.lockDevicePorts();
            Iterator<DevicePort> it = di.getDevicePortIterator();
            while (it.hasNext()) {
                DevicePort port = it.next();
                if (port.uuid.equals(uuid)) {
                    di.releaseDevicePorts();
                    return port;
                }
            }
            di.releaseDevicePorts();
        }
        return null;
    }

    public Device findDeviceByUniqueID(String uniqueID) {
        for (Device di : deviceCollection.items) {
            if (di.UniqueDeviceID.equals(uniqueID)) {
                return di;
            }
        }
        return null;
    }

    public void rename(DevicePort port, String new_name, onHttpRequestResult callback) {
        if (callback != null)
            callback.httpRequestStart(port);

        PluginInterface remote = port.device.getPluginInterface();
        if (remote != null) {
            remote.rename(port, new_name, callback);
        } else if (callback != null)
            callback.httpRequestResult(port, false, App.getAppString(R.string.error_plugin_not_installed));
    }

    /**
     * Notice: Only call this method if the NetpowerctrlService service is running!
     *
     * @param scene    The scene to execute
     * @param callback The callback for the execution-done messages
     */
    public void execute(Scene scene, onExecutionFinished callback) {
        List<PluginInterface> pluginInterfaces = new ArrayList<>();

        // Master/Slave
        int master_command = scene.getMasterCommand();

        for (SceneItem item : scene.sceneItems) {
            DevicePort p = getInstance().findDevicePort(item.uuid);
            if (p == null) {
                Log.e(TAG, "Execute scene, DevicePort not found " + item.uuid.toString());
                continue;
            }

            PluginInterface remote = p.device.getPluginInterface();
            if (remote == null) {
                Log.e(TAG, "Execute scene, PluginInterface not found " + item.uuid.toString());
                continue;
            }

            int command = item.command;
            // Replace toggle by master command if master is set
            if (master_command != DevicePort.INVALID && item.command == DevicePort.TOGGLE)
                command = master_command;

            remote.addToTransaction(p, command);
            if (!pluginInterfaces.contains(remote))
                pluginInterfaces.add(remote);
        }

        for (PluginInterface p : pluginInterfaces) {
            p.executeTransaction(callback);
        }
    }

    /**
     * Notice: Only call this method if the NetpowerctrlService service is running!
     *
     * @param port     The device port
     * @param command  The command to execute
     * @param callback The callback for the execution-done messages
     */
    public void execute(final DevicePort port, final int command, final onExecutionFinished callback) {
        PluginInterface remote = port.device.getPluginInterface();
        if (remote != null) {
            // mark device as changed. Slaves (see below) will also enter this method and will be
            // marked as changed. This is necessary for this scenario: The slave is already in the target state
            // but will be marked as currently-executed (progressbar is visible). Because an update of the device
            // will not be propagated because nothing actually changed, this currently-executed state will
            // stay indefinitely. Therefore we mark all currently-executed devices as changed here.
            port.device.setHasChanged();
            remote.execute(port, command, callback);

            // Support for slaves of an outlet.
            List<UUID> slaves = port.getSlaves();
            if (slaves.size() > 0) {
                boolean bValue = false;
                if (command == DevicePort.ON)
                    bValue = true;
                else if (command == DevicePort.OFF)
                    bValue = false;
                else if (command == DevicePort.TOGGLE)
                    bValue = port.current_value <= 0;

                for (UUID slave_uuid : slaves) {
                    DevicePort p = getInstance().findDevicePort(slave_uuid);
                    if (p != null && p != port)
                        execute(p, bValue ? DevicePort.ON : DevicePort.OFF, null);
                }
            }
            return;
        }

        if (callback == null)
            return;

        callback.onExecutionFinished(1);
    }

    public int countNetworkDevices() {
        int i = 0;
        for (Device di : deviceCollection.items)
            if (di.isNetworkDevice()) ++i;
        return i;
    }

    private static class SingletonHolder {
        public static final AppData instance = new AppData();
    }
}
