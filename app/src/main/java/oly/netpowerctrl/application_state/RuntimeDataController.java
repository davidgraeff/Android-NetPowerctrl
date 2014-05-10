package oly.netpowerctrl.application_state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.DeviceCollection;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.DevicePort;
import oly.netpowerctrl.datastructure.GroupCollection;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.datastructure.SceneCollection;
import oly.netpowerctrl.network.DeviceObserverBase;
import oly.netpowerctrl.network.DevicePortRenamed;
import oly.netpowerctrl.network.DeviceQuery;
import oly.netpowerctrl.network.DeviceUpdate;
import oly.netpowerctrl.network.ExecutionFinished;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.ShowToast;

/**
 * Device Updates go into this singleton object and are propagated to all observers and the device collection.
 * There should never be more than one instance of this class, the singleton object can be accessed by the
 * main Application class.
 * All data collections are centralized in this class. Helper methods regarding configured devices
 * go into this class. At the moment those are: execute, rename, countReachable, countNetworkDevices.
 */
public class RuntimeDataController {
    public final List<DeviceInfo> newDevices = new ArrayList<DeviceInfo>();
    public DeviceCollection deviceCollection;
    public GroupCollection groupCollection;
    public SceneCollection sceneCollection;
    private final LoadStoreData loadStoreData = new LoadStoreData();
    private boolean initialDataQueryCompleted = false;

    private final WeakHashMap<RuntimeDataControllerStateChanged, Boolean> observersStateChanged = new WeakHashMap<RuntimeDataControllerStateChanged, Boolean>();
    private final WeakHashMap<DeviceUpdate, Boolean> observersNew = new WeakHashMap<DeviceUpdate, Boolean>();

    private final List<DeviceObserverBase> updateDeviceStateList = Collections.synchronizedList(new ArrayList<DeviceObserverBase>());

    RuntimeDataController() {
        loadData(false);
    }

    /**
     * Call this to reload all data from disk. This is useful after NFC/Neighbour/GDrive sync.
     *
     * @param notifyObservers Notify all observers of the RuntimeDataControllerState that we
     *                        reloaded data. This should invalidate all caches (icons etc).
     */
    public void loadData(boolean notifyObservers) {
        groupCollection = loadStoreData.readGroups();
        sceneCollection = loadStoreData.readScenes();
        deviceCollection = loadStoreData.readDevices();
        SharedPrefs.setCurrentPreferenceVersion();
        if (notifyObservers)
            notifyStateReloaded();
    }

    //! get a list of all send ports of all configured devices plus the default send port
    public Set<Integer> getAllSendPorts() {
        HashSet<Integer> ports = new HashSet<Integer>();
        ports.add(SharedPrefs.getDefaultSendPort());

        for (DeviceInfo di : deviceCollection.devices)
            ports.add(di.SendPort);

        return ports;
    }

    //! get a list of all receive ports of all configured devices plus the default receive port
    public Set<Integer> getAllReceivePorts() {
        HashSet<Integer> ports = new HashSet<Integer>();
        ports.add(SharedPrefs.getDefaultReceivePort());

        for (DeviceInfo di : deviceCollection.devices)
            ports.add(di.ReceivePort);

        return ports;
    }

    @SuppressWarnings("unused")
    public void registerRuntimeDataControllerStateChanged(RuntimeDataControllerStateChanged o) {
        observersStateChanged.put(o, true);
    }

    @SuppressWarnings("unused")
    public void unregisterRuntimeDataControllerStateChanged(RuntimeDataControllerStateChanged o) {
        observersStateChanged.remove(o);
    }

    public void notifyStateReloaded() {
        for (RuntimeDataControllerStateChanged o : observersStateChanged.keySet())
            o.onDataLoaded();
    }

    void notifyStateQueryFinished() {
        initialDataQueryCompleted = true;
        for (RuntimeDataControllerStateChanged o : observersStateChanged.keySet())
            o.onDataQueryFinished();
    }

    @SuppressWarnings("unused")
    public void registerNewDeviceObserver(DeviceUpdate o) {
        observersNew.put(o, true);
    }

    @SuppressWarnings("unused")
    public void unregisterNewDeviceObserver(DeviceUpdate o) {
        observersNew.remove(o);
    }

    private void notifyNewDeviceObservers(DeviceInfo di, boolean removedFromNew) {
        for (DeviceUpdate o : observersNew.keySet())
            o.onDeviceUpdated(di, removedFromNew);
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

    private void notifyDeviceQueries(DeviceInfo target) {
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
        //            Log.w("stopUseListener","ObserverConfigured: "+Integer.valueOf(observersStateChanged.size()).toString() +
//                    " ObserverNew: "+Integer.valueOf(observersNew.size()).toString()+
//                    " updateDevices: "+Integer.valueOf(updateDeviceStateList.size()).toString());
//            for (RuntimeDataControllerStateChanged dq: observersStateChanged)
//                Log.w("ObserverConfigured_",dq.getClass().toString());

        // There shouldn't be any device-listen observers anymore,
        // but we clear the list here nevertheless.
        for (DeviceObserverBase dq : updateDeviceStateList)
            dq.finishWithTimeouts();
        updateDeviceStateList.clear();
        newDevices.clear();
    }

    public void clearNewDevices() {
        newDevices.clear();
        notifyNewDeviceObservers(null, true);
    }

    public void addToConfiguredDevices(DeviceInfo current_device) {
        if (deviceCollection.add(current_device)) {
            return;
        }

        // Remove from new devices list
        for (int i = 0; i < newDevices.size(); ++i) {
            if (newDevices.get(i).equalsByUniqueID(current_device)) {
                newDevices.remove(i);
                notifyNewDeviceObservers(current_device, true);
                break;
            }
        }

        // Initiate detect devices, if this added device is not flagged as reachable at the moment.
        if (!current_device.isReachable())
            new DeviceQuery(null, current_device);
    }

    /**
     * Call this by your plugin if a device changed and you are on another thread
     *
     * @param device_info
     */
    public void onDeviceUpdatedOtherThread(final DeviceInfo device_info) {
        NetpowerctrlApplication.getMainThreadHandler().post(new Runnable() {
            public void run() {
                onDeviceUpdated(device_info);
            }
        });
    }

    /**
     * Call this by your plugin if a device changed
     *
     * @param device_info
     */
    public void onDeviceUpdated(DeviceInfo device_info) {
        // if it matches a configured device, update it's outlet states and exit the method.
        DeviceInfo existing_device = deviceCollection.update(device_info);
        if (existing_device != null) {
            // notify observers who are using the DeviceQuery class
            notifyDeviceQueries(existing_device);
            return;
        }

        // notify observers who are using the DeviceQuery class
        notifyDeviceQueries(device_info);

        // Do we have this new device already in the list?
        for (DeviceInfo target : newDevices) {
            if (device_info.UniqueDeviceID.equals(target.UniqueDeviceID))
                return;
        }
        // No: Add device to new_device list
        newDevices.add(device_info);
        notifyNewDeviceObservers(device_info, false);
    }

    public void onDeviceErrorByName(String name, String errMessage) {
        // notify observers who are using the DeviceQuery class
        Iterator<DeviceObserverBase> it = updateDeviceStateList.iterator();
        while (it.hasNext()) {
            // Return true if the DeviceQuery object has finished its task.
            if (it.next().notifyObservers(name))
                it.remove();
        }

        // error packet received
        String error = NetpowerctrlApplication.instance.getString(R.string.error_packet_received) + ": " + errMessage;
        ShowToast.FromOtherThread(NetpowerctrlApplication.instance, error);
    }

    public int getReachableConfiguredDevices() {
        int r = 0;
        for (DeviceInfo di : deviceCollection.devices)
            if (di.isReachable())
                ++r;
        return r;
    }

    public DevicePort findDevicePort(UUID uuid) {
        if (uuid == null)
            return null;

        for (DeviceInfo di : deviceCollection.devices) {
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

    public DeviceInfo findDeviceByUniqueID(String uniqueID) {
        for (DeviceInfo di : deviceCollection.devices) {
            if (di.UniqueDeviceID.equals(uniqueID)) {
                return di;
            }
        }
        return null;
    }

    public void rename(DevicePort port, String new_name, DevicePortRenamed callback) {
        if (callback != null)
            callback.devicePort_start_rename(port);

        PluginInterface remote = port.device.getPluginInterface(NetpowerctrlApplication.getService());
        if (remote != null) {
            remote.rename(port, new_name, callback);
            if (callback != null)
                callback.devicePort_renamed(port, true, null);
        } else if (callback != null)
            callback.devicePort_renamed(port, false, NetpowerctrlApplication.instance.getString(R.string.error_plugin_not_installed));
    }

    public void execute(Scene scene, ExecutionFinished callback) {
        List<PluginInterface> pluginInterfaces = new ArrayList<PluginInterface>();

        for (Scene.SceneItem item : scene.sceneItems) {
            DevicePort p = NetpowerctrlApplication.getDataController().findDevicePort(item.uuid);
            if (p == null)
                continue;

            PluginInterface remote = p.device.getPluginInterface(NetpowerctrlApplication.getService());
            if (remote == null)
                continue;

            remote.addToTransaction(p, item.command);
            if (!pluginInterfaces.contains(remote))
                pluginInterfaces.add(remote);
        }

        for (PluginInterface p : pluginInterfaces) {
            p.executeTransaction(callback);
        }
    }

    public void execute(final DevicePort port, final int command, final ExecutionFinished callback) {
        PluginInterface remote = port.device.getPluginInterface(NetpowerctrlApplication.getService());
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
                    DevicePort p = NetpowerctrlApplication.getDataController().findDevicePort(slave_uuid);
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

    public boolean isInitialDataQueryCompleted() {
        return initialDataQueryCompleted;
    }


    public int countNetworkDevices() {
        int i = 0;
        for (DeviceInfo di : deviceCollection.devices)
            if (di.isNetworkDevice(NetpowerctrlApplication.getService())) ++i;
        return i;
    }
}
