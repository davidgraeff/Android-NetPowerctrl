package oly.netpowerctrl.network;

import java.util.List;

import oly.netpowerctrl.devices.Device;

public interface onDeviceObserverFinishedResult {
    void onObserverJobFinished(List<Device> timeout_devices);
}
