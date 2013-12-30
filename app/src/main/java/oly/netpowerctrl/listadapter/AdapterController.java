package oly.netpowerctrl.listadapter;

import android.content.Context;

/**
 * All the list/grid adapters for each fragment (new devices, configured devices, outlets etc)
 * are kept in memory and updated immediately if the device listener service propagates new
 * outlet values. The adapters are collected in this class which itself is instanciated in the
 * main activity.
 */
public class AdapterController {
    public DeviceListAdapter adpConfiguredDevices;
    public DeviceListAdapter adpNewDevices;
    public OutletSwitchListAdapter adpOutlets;
    public ScenesListAdapter adpGroups;

    public AdapterController(Context ctx) {
        adpConfiguredDevices = new DeviceListAdapter(ctx, false);
        adpNewDevices = new DeviceListAdapter(ctx, true);
        adpOutlets = new OutletSwitchListAdapter(ctx);
        adpGroups = new ScenesListAdapter(ctx);
    }
}
