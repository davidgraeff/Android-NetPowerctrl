package oly.netpowerctrl.network;

import java.util.List;

import oly.netpowerctrl.devices.DeviceInfo;

/**
 * on device error
 */
public interface DeviceObserverResult {
    void onDeviceError(DeviceInfo di);

    void onDeviceTimeout(DeviceInfo di);

    void onDeviceUpdated(DeviceInfo di);

    void onObserverJobFinished(List<DeviceInfo> timeout_devices);
}
