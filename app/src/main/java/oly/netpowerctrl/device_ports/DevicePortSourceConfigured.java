package oly.netpowerctrl.device_ports;

import java.lang.ref.WeakReference;

import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.network.DeviceUpdate;

/**
 * Created by david on 07.07.14.
 */
public class DevicePortSourceConfigured implements DevicePortSource, DeviceUpdate {
    final static String TAG = "DevicePortSourceConfigured";
    private WeakReference<DevicePortsBaseAdapter> adapterWeakReference;
    private boolean automaticUpdatesEnabled = false;
    private boolean hideNotReachable = false;

    public boolean isHideNotReachable() {
        return hideNotReachable;
    }

    public void setHideNotReachable(boolean hideNotReachable) {
        this.hideNotReachable = hideNotReachable;
    }

    @Override
    public void updateNow() {
        if (adapterWeakReference == null)
            return;

        DevicePortsBaseAdapter adapter = adapterWeakReference.get();
        if (adapter == null) {
            setAutomaticUpdate(false);
            return;
        }

        adapter.markAllRemoved();

        for (Device device : NetpowerctrlApplication.getDataController().deviceCollection.devices) {
            if (hideNotReachable && device.getFirstReachableConnection() == null)
                continue;
            adapter.addAll(device, false);
        }

        adapter.removeAllMarked(true);

        adapter.notifyDataSetChanged();
    }

    @Override
    public void setAutomaticUpdate(boolean enabled) {
        automaticUpdatesEnabled = enabled;
        if (!enabled) {
            NetpowerctrlApplication.getDataController().deviceCollection.unregisterDeviceObserver(this);
        } else {
            NetpowerctrlApplication.getDataController().deviceCollection.registerDeviceObserver(this);
        }
    }

    @Override
    public void setTargetAdapter(DevicePortsBaseAdapter adapter) {
        adapterWeakReference = new WeakReference<>(adapter);
    }

    @Override
    public boolean isAutomaticUpdateEnabled() {
        return automaticUpdatesEnabled;
    }

    public void onPause() {
        boolean temp = automaticUpdatesEnabled;
        setAutomaticUpdate(false);
        automaticUpdatesEnabled = temp;
    }

    public void onResume() {
        setAutomaticUpdate(automaticUpdatesEnabled);
    }

    @Override
    public void onDeviceUpdated(Device device, boolean willBeRemoved) {
        if (adapterWeakReference == null || device == null)
            return;

        DevicePortsBaseAdapter adapter = adapterWeakReference.get();
        if (adapter == null) {
            setAutomaticUpdate(false);
            return;
        }

        if (willBeRemoved || (hideNotReachable && device.getFirstReachableConnection() == null))
            adapter.removeAll(device, true);
        else {
            adapter.addAll(device, true);
        }

        adapter.notifyDataSetChanged();
    }

}
