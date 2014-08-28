package oly.netpowerctrl.device_ports;

import java.lang.ref.WeakReference;

import oly.netpowerctrl.application_state.RuntimeDataController;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.network.onConfiguredDeviceUpdate;

/**
 * Created by david on 07.07.14.
 */
public class DevicePortSourceConfigured implements DevicePortSource, onConfiguredDeviceUpdate {
    final static String TAG = "DevicePortSourceConfigured";
    private WeakReference<DevicePortsBaseAdapter> adapterWeakReference;
    private boolean automaticUpdatesEnabled = false;
    private boolean hideNotReachable = false;
    private onChange onChangeListener = null;

    public void setOnChangeListener(onChange onChangeListener) {
        this.onChangeListener = onChangeListener;
    }

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

        for (Device device : RuntimeDataController.getDataController().deviceCollection.devices) {
            if (hideNotReachable && device.getFirstReachableConnection() == null)
                continue;
            adapter.addAll(device, false);
        }

        adapter.removeAllMarked(true);

        adapter.notifyDataSetChanged();
        if (onChangeListener != null)
            onChangeListener.devicePortSourceChanged();
    }

    @Override
    public void setAutomaticUpdate(boolean enabled) {
        automaticUpdatesEnabled = enabled;
        if (!enabled) {
            RuntimeDataController.getDataController().deviceCollection.unregisterDeviceObserver(this);
        } else {
            RuntimeDataController.getDataController().deviceCollection.registerDeviceObserver(this);
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
    public void onConfiguredDeviceUpdated(Device device, boolean willBeRemoved) {
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
        if (onChangeListener != null)
            onChangeListener.devicePortSourceChanged();
    }

    public interface onChange {
        void devicePortSourceChanged();
    }

}
