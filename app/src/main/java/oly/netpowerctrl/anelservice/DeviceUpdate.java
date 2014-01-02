package oly.netpowerctrl.anelservice;

import oly.netpowerctrl.datastructure.DeviceInfo;

public interface DeviceUpdate {
    void onDeviceUpdated(DeviceInfo di);
}
