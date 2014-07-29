package oly.netpowerctrl.network;

import java.util.List;

import oly.netpowerctrl.devices.Device;

public interface DeviceObserverFinishedResult {
    void onObserverJobFinished(List<Device> timeout_devices);
}
