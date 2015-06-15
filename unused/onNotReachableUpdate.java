package oly.netpowerctrl.network;

import java.util.List;

import oly.netpowerctrl.credentials.device.Device;

/**
 * Inform of not reachable devices
 */
public interface onNotReachableUpdate {
    void onNotReachableUpdate(List<Device> not_reachable);
}
