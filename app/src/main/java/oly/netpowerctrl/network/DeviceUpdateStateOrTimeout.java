package oly.netpowerctrl.network;

import java.util.List;

import oly.netpowerctrl.datastructure.DeviceInfo;

/**
 * on device error
 */
public interface DeviceUpdateStateOrTimeout {
    void onDeviceTimeout(DeviceInfo di);

    void onDeviceUpdated(DeviceInfo di);

    void onDeviceQueryFinished(List<DeviceInfo> timeout_devices);
}
