package oly.netpowerctrl.network;

import oly.netpowerctrl.datastructure.DeviceInfo;

public interface DeviceUpdate {
    void onDeviceUpdated(DeviceInfo di, boolean willBeRemoved);
}
