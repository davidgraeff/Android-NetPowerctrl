package oly.netpowerctrl.anelservice;

import oly.netpowerctrl.datastructure.DeviceInfo;

/**
 * on device error
 */
public interface DeviceUpdateStateOrTimeout {
    void onDeviceTimeout(DeviceInfo di);

    void onDeviceUpdated(DeviceInfo di);
}
