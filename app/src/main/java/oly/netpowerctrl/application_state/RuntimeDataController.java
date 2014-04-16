package oly.netpowerctrl.application_state;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.DevicePort;
import oly.netpowerctrl.datastructure.Groups;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.datastructure.SceneCollection;
import oly.netpowerctrl.network.DeviceObserverBase;
import oly.netpowerctrl.network.DevicePortRenamed;
import oly.netpowerctrl.network.DeviceQuery;
import oly.netpowerctrl.network.DeviceUpdate;
import oly.netpowerctrl.network.ExecutionFinished;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.Icons;
import oly.netpowerctrl.utils.ShowToast;

/**
 * All configured/new devices are listed here, together with
 * observers.
 */
public class RuntimeDataController {
    public List<DeviceInfo> configuredDevices = new ArrayList<DeviceInfo>();
    public List<DeviceInfo> newDevices = new ArrayList<DeviceInfo>();
    public Groups groups;
    public SceneCollection scenes;
    private boolean initialDataQueryCompleted = false;

    private WeakHashMap<RuntimeDataControllerStateChanged, Boolean> observersStateChanged = new WeakHashMap<RuntimeDataControllerStateChanged, Boolean>();
    private WeakHashMap<DeviceUpdate, Boolean> observersConfiguredDevice = new WeakHashMap<DeviceUpdate, Boolean>();
    private WeakHashMap<DeviceUpdate, Boolean> observersNew = new WeakHashMap<DeviceUpdate, Boolean>();

    private final List<DeviceObserverBase> updateDeviceStateList = Collections.synchronizedList(new ArrayList<DeviceObserverBase>());

    RuntimeDataController() {
        groups = SharedPrefs.readGroups();
        scenes = SharedPrefs.ReadScenes();
        configuredDevices = SharedPrefs.ReadConfiguredDevices();
        //notifyStateReloaded();
    }

    //! get a list of all send ports of all configured scenes plus the default send port
    public Set<Integer> getAllSendPorts() {
        HashSet<Integer> ports = new HashSet<Integer>();
        ports.add(SharedPrefs.getDefaultSendPort());

        for (DeviceInfo di : configuredDevices)
            ports.add(di.SendPort);

        return ports;
    }

    //! get a list of all receive ports of all configured scenes plus the default receive port
    public Set<Integer> getAllReceivePorts() {
        HashSet<Integer> ports = new HashSet<Integer>();
        ports.add(SharedPrefs.getDefaultReceivePort());

        for (DeviceInfo di : configuredDevices)
            ports.add(di.ReceivePort);

        return ports;
    }


    @SuppressWarnings("unused")
    public void registerConfiguredDeviceChangeObserver(DeviceUpdate o) {
        observersConfiguredDevice.put(o, true);
    }

    @SuppressWarnings("unused")
    public void unregisterConfiguredDeviceChangeObserver(DeviceUpdate o) {
        observersConfiguredDevice.remove(o);
    }

    void notifyConfiguredDeviceChangeObservers(DeviceInfo di, boolean willBeRemoved) {
        for (DeviceUpdate o : observersConfiguredDevice.keySet())
            o.onDeviceUpdated(di, willBeRemoved);
    }

    @SuppressWarnings("unused")
    public void registerRuntimeDataControllerStateChanged(RuntimeDataControllerStateChanged o) {
        observersStateChanged.put(o, true);
    }

    @SuppressWarnings("unused")
    public void unregisterRuntimeDataControllerStateChanged(RuntimeDataControllerStateChanged o) {
        observersStateChanged.remove(o);
    }

    void notifyStateReloaded() {
        for (RuntimeDataControllerStateChanged o : observersStateChanged.keySet())
            o.onDataReloaded();
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
        Iterator<DeviceObserverBase> it = updateDeviceStateList.iterator();
        while (it.hasNext()) {
            // Return true if the DeviceQuery object has finished its task.
            if (it.next().notifyObservers(target))
                it.remove();
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

    public void addToConfiguredDevices(DeviceInfo current_device, boolean write_to_disk) {
        // Already in configured devices?
        for (int i = configuredDevices.size() - 1; i >= 0; --i) {
            if (current_device.UniqueDeviceID.equals(configuredDevices.get(i).UniqueDeviceID)) {
                configuredDevices.set(i, current_device);
                if (write_to_disk) {
                    saveConfiguredDevices(true);
                }
                return;
            }
        }

        configuredDevices.add(current_device);
        if (write_to_disk) {
            saveConfiguredDevices(true);
        }

        // Remove from new devices list
        for (int i = 0; i < newDevices.size(); ++i) {
            if (newDevices.get(i).UniqueDeviceID.equals(current_device.UniqueDeviceID)) {
                newDevices.remove(i);
                notifyNewDeviceObservers(current_device, true);
                break;
            }
        }

        // Initiate detect devices, if this added device is not flagged as reachable at the moment.
        if (!current_device.isReachable())
            new DeviceQuery(null, current_device);
    }

    public void deleteAllConfiguredDevices() {
        configuredDevices.clear();
        saveConfiguredDevices(true);
    }

    public void deleteConfiguredDevice(int position) {
        DeviceInfo di = configuredDevices.get(position);
        di.configured = false;
        configuredDevices.remove(position);
        notifyConfiguredDeviceChangeObservers(di, true);
        saveConfiguredDevices(true);
    }

    /**
     * Call this by your plugin if a device changed
     *
     * @param device_info
     */
    public void onDeviceUpdated(DeviceInfo device_info) {
        // if it matches a configured device, update it's outlet states
        for (DeviceInfo target : configuredDevices) {
            if (!device_info.equalsByUniqueID(target))
                continue;

            if (!target.copyFreshValues(device_info)) {
                Log.w("RuntimeDataController", "same values: " + device_info.DeviceName);
                notifyDeviceQueries(target);
                return;
            }

            notifyConfiguredDeviceChangeObservers(target, false);
            notifyDeviceQueries(target);

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


    public void saveConfiguredDevices(boolean updateObservers) {
        SharedPrefs.SaveConfiguredDevices(configuredDevices);
        if (updateObservers)
            notifyStateReloaded();
    }

    public int getReachableConfiguredDevices() {
        int r = 0;
        for (DeviceInfo di : configuredDevices)
            if (di.isReachable())
                ++r;
        return r;
    }

    public DevicePort findDevicePort(UUID uuid) {
        if (uuid == null)
            return null;

        for (DeviceInfo di : configuredDevices) {
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
        for (DeviceInfo di : configuredDevices) {
            if (di.UniqueDeviceID.equals(uniqueID)) {
                return di;
            }
        }
        return null;
    }

    public void rename(DevicePort port, String new_name, DevicePortRenamed callback) {
        if (callback != null)
            callback.devicePort_start_rename(port);

        PluginInterface remote = port.device.getPluginInterface();
        if (remote != null) {
            remote.rename(port, new_name, callback);
            if (callback != null)
                callback.devicePort_renamed(port, true, null);
            return;
        } else if (callback != null)
            callback.devicePort_renamed(port, false, NetpowerctrlApplication.instance.getString(R.string.error_plugin_not_installed));
    }

    public void execute(Scene scene, ExecutionFinished callback) {
        List<PluginInterface> pluginInterfaces = new ArrayList<PluginInterface>();
        for (Scene.SceneItem item : scene.sceneItems) {
            DevicePort p = NetpowerctrlApplication.getDataController().findDevicePort(item.uuid);
            if (p == null)
                continue;

            PluginInterface remote = p.device.getPluginInterface();
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
        PluginInterface remote = port.device.getPluginInterface();
        if (remote != null) {
            remote.execute(port, command, callback);
            return;
        }

        if (callback == null)
            return;

        callback.onExecutionFinished(1);
    }

    public boolean isInitialDataQueryCompleted() {
        return initialDataQueryCompleted;
    }

    public void setDevicePortBitmap(Context context, DevicePort port, Bitmap bitmap) {
        if (port == null)
            return;

        Icons.saveIcon(context, port.uuid,
                Icons.resizeBitmap(context, bitmap, 128, 128),
                Icons.IconType.DevicePortIcon, port.getIconState());
        notifyConfiguredDeviceChangeObservers(port.device, false);
    }
}
