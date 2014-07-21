package oly.netpowerctrl.network;

import java.util.List;

import oly.netpowerctrl.devices.DeviceInfo;

/**
 * on device error
 */
public interface DeviceObserverResult {
    void onDeviceUpdated(DeviceInfo di);

    void onObserverJobFinished(List<DeviceInfo> timeout_devices);
}
