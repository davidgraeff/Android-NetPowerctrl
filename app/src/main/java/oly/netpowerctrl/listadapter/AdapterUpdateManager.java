package oly.netpowerctrl.listadapter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;

import oly.netpowerctrl.R;
import oly.netpowerctrl.service.DeviceQuery;
import oly.netpowerctrl.service.DiscoveryThread;
import oly.netpowerctrl.utils.DeviceInfo;
import oly.netpowerctrl.utils.GreenFlasher;
import oly.netpowerctrl.utils.OutletInfo;
import oly.netpowerctrl.utils.SharedPrefs;

/**
 * Created by david on 21.12.13.
 */
public class AdapterUpdateManager {
    public ArrayList<DeviceInfo> configuredDevices = new ArrayList<DeviceInfo>();
    public ArrayList<DeviceInfo> newDevices = new ArrayList<DeviceInfo>();
    public DeviceListAdapter adpConfiguredDevices;
    public DeviceListAdapter adpNewDevices;
    public OutledSwitchListAdapter adpOutlets;
    public GroupListAdapter adpGroups;

    Context ctx;

    public AdapterUpdateManager(Context ctx) {
        this.ctx = ctx;

        adpConfiguredDevices = new DeviceListAdapter(ctx, configuredDevices);
        adpNewDevices = new DeviceListAdapter(ctx, newDevices);

        adpOutlets = new OutledSwitchListAdapter(ctx, configuredDevices);

        adpGroups = new GroupListAdapter(ctx);
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
            for (int i=0;i<newEntries.size();++i) {
                if (newEntries.get(i).MacAddress.equals(old_di.MacAddress)) {
                    new_di = newEntries.get(i);
                    break;
                }
            }
            if (new_di == null) {
                oldEntriesIt.remove();
            } else if (old_di.Outlets.size() != new_di.Outlets.size()) {
                // Number of outlets have changed. This is a reason to forget about
                // outlet states and replace the old device with the new one.
                old_di = new_di;
            }
        }
        // add new devices
        Iterator<DeviceInfo> newEntriesIt = newEntries.iterator();
        while (newEntriesIt.hasNext()) {
            DeviceInfo new_di = newEntriesIt.next();
            DeviceInfo old_di = null;
            for (int i=0;i<configuredDevices.size();++i) {
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

    public void CopyDevice(DeviceInfo current_device) {
        DeviceInfo new_device = new DeviceInfo(current_device);
        new_device.setConfigured(true);
        new_device.DeviceName = String.format(ctx.getResources().getString(R.string.copy_of), new_device.DeviceName);
        configuredDevices.add(new_device);
        DeviceQuery.sendBroadcastQuery(ctx);
        SaveConfiguredDevices();
    }

    public void AddToConfiguredDevices(DeviceInfo current_device) {
        DeviceInfo new_device = new DeviceInfo(current_device);
        new_device.setConfigured(true);
        configuredDevices.add(new_device);
        DeviceQuery.sendBroadcastQuery(ctx);
        SaveConfiguredDevices();

        // Remove from new devices
        for (int i=0;i<newDevices.size();++i)
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

    private BroadcastReceiver onDeviceDiscovered= new BroadcastReceiver() {
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

            // remove it's disabled outlets
            for ( Iterator<OutletInfo> i = device_info.Outlets.iterator(); i.hasNext(); )
                if (i.next().Disabled)
                    i.remove();

            // if it matches a configured device, update it's outlet states
            for (DeviceInfo target: configuredDevices) {
                if (!device_info.MacAddress.equals(target.MacAddress))
                    continue;
                for (OutletInfo srcoi: device_info.Outlets) {
                    for (OutletInfo tgtoi: target.Outlets) {
                        if (tgtoi.OutletNumber == srcoi.OutletNumber) {
                            tgtoi.State = srcoi.State;
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
            for (DeviceInfo target: newDevices) {
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
        IntentFilter itf= new IntentFilter(DiscoveryThread.BROADCAST_DEVICE_DISCOVERED);
        LocalBroadcastManager.getInstance(ctx).registerReceiver(onDeviceDiscovered, itf);
    }

    public void stop() {
        LocalBroadcastManager.getInstance(ctx).unregisterReceiver(onDeviceDiscovered);
    }
}
