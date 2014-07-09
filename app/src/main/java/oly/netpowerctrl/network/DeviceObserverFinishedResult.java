package oly.netpowerctrl.network;

import java.util.List;

import oly.netpowerctrl.devices.DeviceInfo;

public interface DeviceObserverFinishedResult {
    void onObserverJobFinished(List<DeviceInfo> timeout_devices);
}
