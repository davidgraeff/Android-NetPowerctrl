package oly.netpowerctrl.application_state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anelservice.DeviceError;
import oly.netpowerctrl.anelservice.DeviceQuery;
import oly.netpowerctrl.anelservice.DeviceUpdate;
import oly.netpowerctrl.anelservice.DevicesUpdate;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.OutletInfo;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.ShowToast;

/**
 * All configured/new devices are listed here, together with
 * observers.
 */
public class RuntimeDataController implements DeviceUpdate, DeviceError {
    public ArrayList<DeviceInfo> configuredDevices = new ArrayList<DeviceInfo>();
    public ArrayList<DeviceInfo> newDevices = new ArrayList<DeviceInfo>();

    private ArrayList<DevicesUpdate> observersConfigured = new ArrayList<DevicesUpdate>();
    private ArrayList<DevicesUpdate> observersNew = new ArrayList<DevicesUpdate>();

    private List<DeviceQuery> updateDeviceStateList = Collections.synchronizedList(new ArrayList<DeviceQuery>());


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
    public boolean registerConfiguredObserver(DevicesUpdate o) {
        if (!observersConfigured.contains(o)) {
            observersConfigured.add(o);
            return true;
        }
        return false;
    }

    @SuppressWarnings("unused")
    public void unregisterConfiguredObserver(DevicesUpdate o) {
        observersConfigured.remove(o);
    }

    void notifyConfiguredObservers(List<DeviceInfo> changed_devices) {
        for (DevicesUpdate o : observersConfigured)
            o.onDevicesUpdated(changed_devices);

    }

    @SuppressWarnings("unused")
    public boolean registerNewDeviceObserver(DevicesUpdate o) {
        if (!observersNew.contains(o)) {
            observersNew.add(o);
            return true;
        }
        return false;
    }

    @SuppressWarnings("unused")
    public void unregisterNewDeviceObserver(DevicesUpdate o) {
        observersNew.remove(o);
    }

    private void notifyNewDeviceObservers(List<DeviceInfo> new_devices) {
        for (DevicesUpdate o : observersNew)
            o.onDevicesUpdated(new_devices);
    }


    public void removeUpdateDeviceState(DeviceQuery o) {
        updateDeviceStateList.remove(o);
    }

    public void addUpdateDeviceState(DeviceQuery o) {
        updateDeviceStateList.add(o);
    }

    public void clear() {
        //            Log.w("stopUseListener","ObserverConfigured: "+Integer.valueOf(observersConfigured.size()).toString() +
//                    " ObserverNew: "+Integer.valueOf(observersNew.size()).toString()+
//                    " updateDevices: "+Integer.valueOf(updateDeviceStateList.size()).toString());
//            for (DevicesUpdate dq: observersConfigured)
//                Log.w("ObserverConfigured_",dq.getClass().toString());

        // There shouldn't be any device-listen observers anymore,
        // but we clear the list here nevertheless.
        for (DeviceQuery dq : updateDeviceStateList)
            dq.finishWithTimeouts();
        updateDeviceStateList.clear();
        newDevices.clear();
    }


    public void reloadConfiguredDevices() {
        List<DeviceInfo> newEntries = SharedPrefs.ReadConfiguredDevices();
        // This is somehow a more complicated way of alDevices := newEntries
        // because with an assignment we would lose the outlet states which are not stored in the SharedPref
        // and only with the next UDP update those states are restored. This
        // induces a flicker where we can first observe all outlets set to off and
        // within a fraction of a second the actual values are applied.
        // Therefore we update by hand without touching outlet states.
        Iterator<DeviceInfo> oldEntriesIt = configuredDevices.iterator();
        while (oldEntriesIt.hasNext()) {
            // remove scenes not existing anymore
            DeviceInfo old_di = oldEntriesIt.next();
            DeviceInfo new_di = null;
            for (DeviceInfo it_di : newEntries) {
                if (it_di.MacAddress.equals(old_di.MacAddress)) {
                    new_di = it_di;
                    break;
                }
            }
            if (new_di == null) {
                oldEntriesIt.remove();
            } else if (old_di.Outlets.size() != new_di.Outlets.size()) {
                // Number of outlets have changed. This is a reason to forget about
                // outlet states and replace the old device with the new one.
                oldEntriesIt.remove();
            }
        }
        // add new scenes
        for (DeviceInfo new_di : newEntries) {
            DeviceInfo old_di = null;
            for (int i = 0; i < configuredDevices.size(); ++i) {
                if (configuredDevices.get(i).MacAddress.equals(new_di.MacAddress)) {
                    old_di = newEntries.get(i);
                    break;
                }
            }
            if (old_di == null) {
                configuredDevices.add(new_di);
            }
        }
        notifyConfiguredObservers(newEntries);
    }

    public void addToConfiguredDevices(DeviceInfo current_device, boolean write_to_disk) {
        // Already in configured devices?
        for (int i = configuredDevices.size() - 1; i >= 0; --i) {
            if (current_device.MacAddress.equals(configuredDevices.get(i).MacAddress)) {
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
            if (newDevices.get(i).MacAddress.equals(current_device.MacAddress)) {
                newDevices.remove(i);
                notifyNewDeviceObservers(newDevices);
                break;
            }
        }

        // Initiate detect devices, if this added device is not flagged as reachable at the moment.
        if (!current_device.reachable)
            new DeviceQuery(null, current_device);
    }

    public void deleteAllConfiguredDevices() {
        configuredDevices.clear();
        saveConfiguredDevices(true);
    }

    public void deleteConfiguredDevice(int position) {
        configuredDevices.remove(position);
        saveConfiguredDevices(true);
    }

    @Override
    public void onDeviceUpdated(DeviceInfo device_info) {
        // if it matches a configured device, update it's outlet states
        for (DeviceInfo target : configuredDevices) {
            if (!device_info.MacAddress.equals(target.MacAddress))
                continue;
            target.copyFreshValues(device_info);
            target.reachable = true;
            target.updated = System.currentTimeMillis();

            // notify all observers
            List<DeviceInfo> updates_devices = new ArrayList<DeviceInfo>();
            updates_devices.add(target);
            notifyConfiguredObservers(updates_devices);

            // notify observers who are using the DeviceQuery class
            Iterator<DeviceQuery> it = updateDeviceStateList.iterator();
            while (it.hasNext()) {
                // Return true if the DeviceQuery object has finished its task.
                if (it.next().notifyObservers(target))
                    it.remove();
            }

            return;
        }

        // Do we have this new device already in the list?
        for (DeviceInfo target : newDevices) {
            if (device_info.MacAddress.equals(target.MacAddress))
                return;
        }
        // No: Add device to new_device list
        newDevices.add(device_info);
        notifyNewDeviceObservers(newDevices);
    }

    @Override
    public void onDeviceError(String deviceName, String errMessage) {
        // error packet received
        String desc;
        if (errMessage.trim().equals("NoPass"))
            desc = NetpowerctrlApplication.instance.getString(R.string.error_nopass);
        else
            desc = errMessage;
        String error = NetpowerctrlApplication.instance.getString(R.string.error_packet_received) + ": " + desc;
        ShowToast.FromOtherThread(NetpowerctrlApplication.instance, error);
    }


    public DeviceInfo findDevice(String mac_address) {
        for (DeviceInfo di : configuredDevices) {
            if (di.MacAddress.equals(mac_address)) {
                return di;
            }
        }
        return null;
    }

    public DeviceInfo findDeviceByHostnameAndIP(String hostname, int port) {
        for (DeviceInfo di : configuredDevices) {
            if (di.HostName.equals(hostname) && di.SendPort == port) {
                return di;
            }
        }
        return null;
    }

    public OutletInfo findOutlet(String mac_address, int outletNumber) {
        for (DeviceInfo di : configuredDevices) {
            if (di.MacAddress.equals(mac_address)) {
                for (OutletInfo oi : di.Outlets) {
                    if (oi.OutletNumber == outletNumber) {
                        return oi;
                    }
                }
                return null;
            }
        }
        return null;
    }

    public void saveConfiguredDevices(boolean updateObservers) {
        SharedPrefs.SaveConfiguredDevices(configuredDevices);
        if (updateObservers)
            notifyConfiguredObservers(configuredDevices);
    }

    public int getReachableConfiguredDevices() {
        int r = 0;
        for (DeviceInfo di : configuredDevices)
            if (di.reachable)
                ++r;
        return r;
    }

}
