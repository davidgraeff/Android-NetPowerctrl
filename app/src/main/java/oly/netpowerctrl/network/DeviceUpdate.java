package oly.netpowerctrl.network;

import oly.netpowerctrl.devices.Device;

public interface DeviceUpdate {
    void onDeviceUpdated(Device di, boolean willBeRemoved);
}
