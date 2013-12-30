package oly.netpowerctrl.listadapter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.Iterator;

import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.OutletInfo;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.service.DeviceQuery;
import oly.netpowerctrl.service.DiscoveryThread;

/**
 * All the list/grid adapters for each fragment (new devices, configured devices, outlets etc)
 * are kept in memory and updated immediately if the device listener service propagates new
 * outlet values. The adapters are collected in this class which itself is instanciated in the
 * main activity.
 */
public class AdapterUpdateManager {
    public ArrayList<DeviceInfo> configuredDevices = new ArrayList<DeviceInfo>();
    public ArrayList<DeviceInfo> newDevices = new ArrayList<DeviceInfo>();
    public DeviceListAdapter adpConfiguredDevices;
    public DeviceListAdapter adpNewDevices;
    public OutledSwitchListAdapter adpOutlets;
    public ScenesListAdapter adpGroups;

    Context ctx;

    public AdapterUpdateManager(Context ctx) {
        this.ctx = ctx;

        adpConfiguredDevices = new DeviceListAdapter(ctx, configuredDevices);
        adpNewDevices = new DeviceListAdapter(ctx, newDevices);

        adpOutlets = new OutledSwitchListAdapter(ctx, configuredDevices);

        adpGroups = new ScenesListAdapter(ctx);
    }

    public void updateConfiguredDevices() {
        ArrayList<DeviceInfo> newEntries = SharedPrefs.ReadConfiguredDevices(ctx);
        // This is somehow a more complicated way of alDevices := newEntries
        // because with an assignment we would lose the outlet states which are not stored in the SharedPref
        // and only with the next UDP update those states are restored. This
        // induces a flicker where we can first observe all outlets set to off and
        // within a fraction of a second the actual values are applied.
        // Therefore we update by hand without touching outlet states.
        Iterator<DeviceInfo> oldEntriesIt = configuredDevices.iterator();
        while (oldEntriesIt.hasNext()) {
            // remove devices not existing anymore
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
        // add new devices
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
        adpConfiguredDevices.update();
        adpOutlets.update();
    }

    public void AddToConfiguredDevices(DeviceInfo current_device) {
        // remove it's disabled outlets
        for (Iterator<OutletInfo> i = current_device.Outlets.iterator(); i.hasNext(); )
            if (i.next().Disabled)
                i.remove();

        // Already in configured devices?
        for (int i = configuredDevices.size() - 1; i >= 0; --i) {
            if (current_device.MacAddress.equals(configuredDevices.get(i).MacAddress)) {
                configuredDevices.set(i, current_device);
                DeviceQuery.sendBroadcastQuery(ctx);
                SaveConfiguredDevices();
                return;
            }
        }

        DeviceInfo new_device = new DeviceInfo(current_device);
        new_device.setConfigured(true);
        configuredDevices.add(new_device);
        DeviceQuery.sendBroadcastQuery(ctx);
        SaveConfiguredDevices();

        // Remove from new devices
        for (int i = 0; i < newDevices.size(); ++i)
            if (newDevices.get(i).MacAddress.equals(current_device.MacAddress)) {
                newDevices.remove(i);
                adpNewDevices.update();
                break;
            }
    }

    private void SaveConfiguredDevices() {
        SharedPrefs.SaveConfiguredDevices(configuredDevices, ctx);
        DeviceQuery.restartDiscovery(ctx);  // ports may have changed
        adpConfiguredDevices.update();
        adpOutlets.update();
    }

    public void deleteAllConfiguredDevices() {
        configuredDevices.clear();
        SaveConfiguredDevices();
    }

    public void deleteDevice(int position) {
        configuredDevices.remove(position);
        SaveConfiguredDevices();
    }

//    public DeviceInfo findDevice(UUID uuid) {
//        for (DeviceInfo di : configuredDevices) {
//            if (di.equals(uuid)) {
//                return di;
//            }
//        }
//        return null;
//    }

    private BroadcastReceiver onDeviceDiscovered = new BroadcastReceiver() {
        @Override
        synchronized public void onReceive(Context context, Intent intent) {
            DeviceInfo device_info = null;
            Bundle extra = intent.getExtras();
            if (extra != null) {
                Object o = extra.get("device_info");
                if (o != null) {
                    device_info = (DeviceInfo) o;
                }
            }
            if (device_info == null)
                return;

            // if it matches a configured device, update it's outlet states
            for (DeviceInfo target : configuredDevices) {
                if (!device_info.MacAddress.equals(target.MacAddress))
                    continue;
                for (OutletInfo srcoi : device_info.Outlets) {
                    for (OutletInfo tgtoi : target.Outlets) {
                        if (tgtoi.OutletNumber == srcoi.OutletNumber) {
                            tgtoi.State = srcoi.State;
                            tgtoi.Disabled = srcoi.Disabled;
                            break;
                        }
                    }
                }

                adpConfiguredDevices.update();
                adpOutlets.update();
                return;
            }

            // Device is a new one, set configured to false
            device_info.setConfigured(false);
            // Do we have this new device already in the list?
            for (DeviceInfo target : newDevices) {
                if (device_info.MacAddress.equals(target.MacAddress))
                    return;
            }
            // No: Add device to new_device list
            newDevices.add(device_info);
            adpNewDevices.update();

            // Highlight updated rows
//            for (int i=0; i<lvDevices.getChildCount(); i++) {
//                View child = lvDevices.getChildAt(i);
//                if (child != null && adpDevices.getCount()>(Integer)child.getTag()) {
//                    DeviceInfo di = (DeviceInfo)adpDevices.getItem((Integer)child.getTag());
//                    if (di.MacAddress.equals(device_info.MacAddress)) {
//                        GreenFlasher.flashBgColor(child);
//                    }
//                }
//            }
        }
    };

    public void start() {
        IntentFilter itf = new IntentFilter(DiscoveryThread.BROADCAST_DEVICE_DISCOVERED);
        LocalBroadcastManager.getInstance(ctx).registerReceiver(onDeviceDiscovered, itf);
    }

    public void stop() {
        LocalBroadcastManager.getInstance(ctx).unregisterReceiver(onDeviceDiscovered);
    }
}
