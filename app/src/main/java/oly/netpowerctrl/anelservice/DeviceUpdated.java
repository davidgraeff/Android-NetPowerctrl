package oly.netpowerctrl.anelservice;

import oly.netpowerctrl.datastructure.DeviceInfo;

/**
 * Issued by the listener service through the
 * application instance.
 */
public interface DeviceUpdated {
    void onDeviceUpdated(DeviceInfo di);
}
