package oly.netpowerctrl.network;

import java.util.List;

import oly.netpowerctrl.datastructure.DeviceInfo;

/**
 * Issued by the listener service through the
 * application instance.
 */
public interface DevicesUpdate {
    void onDevicesUpdated(List<DeviceInfo> changed_devices);
}
