package oly.netpowerctrl.network;

import oly.netpowerctrl.devices.DeviceInfo;

public interface DeviceUpdate {
    void onDeviceUpdated(DeviceInfo di, boolean willBeRemoved);
}
