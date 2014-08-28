package oly.netpowerctrl.application_state;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
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
import oly.netpowerctrl.network.AsyncRunnerResult;
import oly.netpowerctrl.network.DeviceObserverBase;
import oly.netpowerctrl.network.DeviceQuery;
import oly.netpowerctrl.network.ExecutionFinished;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.scenes.Scene;
import oly.netpowerctrl.scenes.SceneCollection;
import oly.netpowerctrl.timer.TimerController;
import oly.netpowerctrl.utils_gui.ShowToast;

/**
 * Device Updates go into this singleton object and are propagated to all observers and the device collection.
 * There should never be more than one instance of this class, the singleton object can be accessed by the
 * main Application class.
 * All data collections are centralized in this class. Helper methods regarding configured devices
 * go into this class. At the moment those are: execute, rename, countReachable, countNetworkDevices.
 */
public class RuntimeDataController {
    public static final DataQueryCompletedObserver observersDataQueryCompleted = new DataQueryCompletedObserver();
    public static final DataLoadedObserver observersOnDataLoaded = new DataLoadedObserver();
    public static final NewDeviceObserver observersNew = new NewDeviceObserver();
    private static RuntimeDataController dataController = null;
    public final List<Device> newDevices = new ArrayList<>();
    final public DeviceCollection deviceCollection = new DeviceCollection();
    final public GroupCollection groupCollection = new GroupCollection();
    final public SceneCollection sceneCollection = new SceneCollection();
    final public TimerController timerController = new TimerController();
    private final List<DeviceObserverBase> updateDeviceStateList =
            Collections.synchronizedList(new ArrayList<DeviceObserverBase>());
    private LoadStoreData loadStoreData;

    public static RuntimeDataController createRuntimeDataController(LoadStoreData loadStoreData) {
        dataController = new RuntimeDataController();
        dataController.loadStoreData = loadStoreData;
        if (dataController.loadStoreData != null)
            dataController.loadData(false);
        return dataController;
    }

    static public RuntimeDataController getDataController() {
        if (dataController == null) {
            createRuntimeDataController(new LoadStoreData(NetpowerctrlApplication.instance));
        }
        return dataController;
    }

    public void finish() {
        loadStoreData = null;
        dataController = null;
    }

    /**
     * Call this to reload all data from disk. This is useful after NFC/Neighbour/GDrive sync.
     *
     * @param notifyObservers Notify all observers of the RuntimeDataControllerState that we
     *                        reloaded data. This should invalidate all caches (icons etc).
     */
    void loadData(final boolean notifyObservers) {
        Thread t = new Thread("loadDataThread") {
            public void run() {
                loadStoreData.read(groupCollection);
                loadStoreData.read(sceneCollection);
                loadStoreData.read(deviceCollection);
                loadStoreData.read(timerController);
                loadStoreData.markVersion();
                observersOnDataLoaded.dataLoaded = true;
                if (notifyObservers)
                    observersOnDataLoaded.onDataLoaded();
            }
        };
        t.start();
    }

    //! get a list of all send ports of all configured devices plus the default send port
    public Set<Integer> getAllSendPorts() {
        HashSet<Integer> ports = new HashSet<>();
        ports.add(SharedPrefs.getInstance().getDefaultSendPort());

        for (Device di : deviceCollection.devices)
            for (DeviceConnection ci : di.DeviceConnections)
                if (ci instanceof DeviceConnectionUDP)
                    ports.add(ci.getDestinationPort());

        return ports;
    }

    //! get a list of all receive ports of all configured devices plus the default receive port
    public Set<Integer> getAllReceivePorts() {
        HashSet<Integer> ports = new HashSet<>();
        ports.add(SharedPrefs.getInstance().getDefaultReceivePort());

        for (Device di : deviceCollection.devices)
            for (DeviceConnection ci : di.DeviceConnections)
                if (ci instanceof DeviceConnectionUDP)
                    ports.add(ci.getListenPort());

        return ports;
    }

    public void removeUpdateDeviceState(DeviceObserverBase o) {
        synchronized (updateDeviceStateList) {
            updateDeviceStateList.remove(o);
        }
    }

    public void addUpdateDeviceState(DeviceObserverBase o) {
        synchronized (updateDeviceStateList) {
            updateDeviceStateList.add(o);
        }
    }

    private void notifyDeviceQueries(Device target) {
        // notify observers who are using the DeviceQuery class
        synchronized (updateDeviceStateList) {
            Iterator<DeviceObserverBase> it = updateDeviceStateList.iterator();
            while (it.hasNext()) {
                // Return true if the DeviceQuery object has finished its task.
                if (it.next().notifyObservers(target))
                    it.remove();
            }
        }
    }

    public void clear() {
        // There shouldn't be any device-listen observers anymore,
        // but we clear the list here nevertheless.
        for (DeviceObserverBase dq : updateDeviceStateList)
            dq.finishWithTimeouts();
        updateDeviceStateList.clear();
        newDevices.clear();
    }

    public void clearNewDevices() {
        newDevices.clear();
        observersNew.onNewDevice(null);
    }

    public void addToConfiguredDevices(Context context, Device device) {
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
     * @param device_info
     */
    public void onDeviceUpdatedOtherThread(final Device device_info) {
        NetpowerctrlApplication.getMainThreadHandler().post(new Runnable() {
            public void run() {
                onDeviceUpdated(device_info);
            }
        });
    }

    /**
     * Call this by your plugin if a device changed. This method is also called
     * by the DeviceQuery class if devices are not reachable anymore.
     *
     * @param device_info
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

        // Do we have this new device already in the list?
        for (Device target : newDevices) {
            if (device_info.UniqueDeviceID.equals(target.UniqueDeviceID))
                return;
        }
        // No: Add device to new_device list
        newDevices.add(device_info);
        observersNew.onNewDevice(device_info);
    }

    public void onDeviceErrorByName(Context context, String name, String errMessage) {
        // notify observers who are using the DeviceQuery class
        Iterator<DeviceObserverBase> it = updateDeviceStateList.iterator();
        while (it.hasNext()) {
            // Return true if the DeviceQuery object has finished its task.
            if (it.next().notifyObservers(name))
                it.remove();
        }

        // error packet received
        String error = context.getString(R.string.error_packet_received) + ": " + errMessage;
        ShowToast.FromOtherThread(context, error);
    }

    public int getReachableConfiguredDevices() {
        int r = 0;
        for (Device device : deviceCollection.devices)
            if (device.getFirstReachableConnection() != null)
                ++r;
        return r;
    }

    public DevicePort findDevicePort(UUID uuid) {
        if (uuid == null)
            return null;

        for (Device di : deviceCollection.devices) {
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
        for (Device di : deviceCollection.devices) {
            if (di.UniqueDeviceID.equals(uniqueID)) {
                return di;
            }
        }
        return null;
    }

    public void rename(DevicePort port, String new_name, AsyncRunnerResult callback) {
        if (callback != null)
            callback.asyncRunnerStart(port);

        PluginInterface remote = port.device.getPluginInterface();
        if (remote != null) {
            remote.rename(port, new_name, callback);
        } else if (callback != null)
            callback.asyncRunnerResult(port, false, NetpowerctrlApplication.getAppString(R.string.error_plugin_not_installed));
    }

    /**
     * Notice: Only call this method if the NetpowerctrlService service is running!
     *
     * @param scene    The scene to execute
     * @param callback The callback for the execution-done messages
     */
    public void execute(Scene scene, ExecutionFinished callback) {
        List<PluginInterface> pluginInterfaces = new ArrayList<>();

        // Master/Slave
        int master_command = scene.getMasterCommand();

        for (Scene.SceneItem item : scene.sceneItems) {
            DevicePort p = getDataController().findDevicePort(item.uuid);
            if (p == null)
                continue;

            PluginInterface remote = p.device.getPluginInterface();
            if (remote == null)
                continue;

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
    public void execute(final DevicePort port, final int command, final ExecutionFinished callback) {
        PluginInterface remote = port.device.getPluginInterface();
        if (remote != null) {
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
                    DevicePort p = getDataController().findDevicePort(slave_uuid);
                    if (p != null)
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
        for (Device di : deviceCollection.devices)
            if (di.isNetworkDevice()) ++i;
        return i;
    }
}
