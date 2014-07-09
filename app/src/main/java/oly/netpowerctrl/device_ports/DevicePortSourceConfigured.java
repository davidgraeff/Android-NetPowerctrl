package oly.netpowerctrl.device_ports;

import java.lang.ref.WeakReference;

import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.devices.DeviceInfo;
import oly.netpowerctrl.network.DeviceUpdate;

/**
 * Created by david on 07.07.14.
 */
public class DevicePortSourceConfigured implements DevicePortSource, DeviceUpdate {
    final static String TAG = "DevicePortSourceConfigured";
    private WeakReference<DevicePortsBaseAdapter> adapterWeakReference;
    private boolean automaticUpdatesEnabled = false;
    private boolean hideNotReachable = false;
    private boolean isBatch = false;

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
        adapter.markAllRemoved();
        isBatch = true;

        for (DeviceInfo deviceInfo : NetpowerctrlApplication.getDataController().deviceCollection.devices) {
            if (hideNotReachable && !deviceInfo.isReachable())
                continue;
            onDeviceUpdated(deviceInfo, false);
        }

        isBatch = false;
        adapter.removeAllMarked();

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
        adapterWeakReference = new WeakReference<DevicePortsBaseAdapter>(adapter);
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
    public void onDeviceUpdated(DeviceInfo di, boolean willBeRemoved) {
        if (adapterWeakReference == null || di == null)
            return;

        DevicePortsBaseAdapter adapter = adapterWeakReference.get();
        if (adapter == null) {
            setAutomaticUpdate(false);
            return;
        }

        if (willBeRemoved || (hideNotReachable && !di.isReachable()))
            adapter.removeAll(di);
        else {
            adapter.addAll(di);
        }

        if (!isBatch)
            adapter.notifyDataSetChanged();
    }

}
