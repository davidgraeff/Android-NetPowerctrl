package oly.netpowerctrl.consistency_tests;

import android.support.v7.widget.RecyclerView;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DeviceConnection;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.device_base.executables.Executable;
import oly.netpowerctrl.device_base.executables.ExecutableReachability;
import oly.netpowerctrl.devices.DeviceCollection;
import oly.netpowerctrl.devices.DevicesAdapter;
import oly.netpowerctrl.executables.AdapterSource;
import oly.netpowerctrl.executables.ExecutableAdapterItem;
import oly.netpowerctrl.executables.ExecutablesAdapter;
import oly.netpowerctrl.pluginservice.PluginService;

/**
 * Created by david on 02.02.15.
 */
public class device_tests {
    private static List<RWIthName> rwIthNameList = new ArrayList<>();

    public static void test_connection_reachable_consistency(Device device) {
        boolean isReachable = false;
        for (DeviceConnection deviceConnection : device.getDeviceConnections()) {
            if (deviceConnection.reachableState() == ExecutableReachability.Reachable)
                isReachable = true;
        }

        if (isReachable && device.reachableState() != ExecutableReachability.Reachable)
            throw new RuntimeException("test_connection_reachable_consistency");
    }

    public static void test_check_adapters() {
        AppData appData = PluginService.getService().getAppData();
        if (appData == null) return;

        DeviceCollection deviceCollection = appData.deviceCollection;

        for (RWIthName rwIthName : rwIthNameList) {
            RecyclerView r = rwIthName.r;
            if (r.getAdapter() instanceof ExecutablesAdapter) {
                Log.w("test_check_adapters", "Check ExecutablesAdapter " + rwIthName.name);
                ExecutablesAdapter executablesAdapter = (ExecutablesAdapter) r.getAdapter();
                AdapterSource adapterSource = executablesAdapter.getSource();
                for (ExecutableAdapterItem item : adapterSource.mItems) {
                    Executable executable = item.getExecutable();
                    if (executable == null || !(executable instanceof DevicePort))
                        continue;
                    DevicePort devicePort = (DevicePort) executable;
                    int pos = deviceCollection.getPosition(devicePort.device);
                    if (pos == -1)
                        throw new RuntimeException("Device in adapter not in deviceCollection!");
                    Device device = deviceCollection.get(pos);
                    if (device != devicePort.device)
                        throw new RuntimeException("Device in adapter not the same as in deviceCollection!");
                    if (executable.reachableState() == ExecutableReachability.NotReachable)
                        throw new RuntimeException("ExecutablesAdapter contains not reachable entry!");
                }

            } else if (r.getAdapter() instanceof DevicesAdapter) {
                Log.w("test_check_adapters", "Check DevicesAdapter " + rwIthName.name);
                int c = r.getChildCount();
                for (int i = 0; i < c; ++i) {
                    RecyclerView.ViewHolder v = r.getChildViewHolder(r.getChildAt(i));
                    DevicesAdapter.ViewHolder viewHolder = (DevicesAdapter.ViewHolder) v;
                    if (viewHolder.lastKnownReachableState != viewHolder.device.reachableState()) {
                        throw new RuntimeException("DevicesAdapter inconsistent reachable state!");
                    }
                }
            }
        }
    }

    public void addToObservedRecyclerView(RecyclerView r, String name) {
        rwIthNameList.add(new RWIthName(r, name));
    }

    public void removeToObservedRecyclerView(RecyclerView r) {
        Iterator<RWIthName> it = rwIthNameList.iterator();
        while (it.hasNext()) {
            if (it.next().r == r)
                it.remove();
        }
    }

    private static class RWIthName {
        RecyclerView r;
        String name;

        private RWIthName(RecyclerView r, String name) {
            this.r = r;
            this.name = name;
        }
    }
}
