package oly.netpowerctrl.network;

import oly.netpowerctrl.devices.Device;

public interface onConfiguredDeviceUpdate {
    void onConfiguredDeviceUpdated(Device di, boolean willBeRemoved);
}
