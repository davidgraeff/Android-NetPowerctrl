package oly.netpowerctrl.network;

import java.util.List;

import oly.netpowerctrl.device_base.device.Device;

/**
 * on device error
 */
public interface onDeviceObserverResult {
    void onObserverJobFinished(List<Device> timeout_devices);
}
