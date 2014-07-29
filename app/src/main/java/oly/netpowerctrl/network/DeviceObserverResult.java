package oly.netpowerctrl.network;

import java.util.List;

import oly.netpowerctrl.devices.Device;

/**
 * on device error
 */
public interface DeviceObserverResult {
    void onDeviceUpdated(Device di);

    void onObserverJobFinished(List<Device> timeout_devices);
}
