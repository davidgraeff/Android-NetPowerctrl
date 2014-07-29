package oly.netpowerctrl.network;

import java.util.List;

import oly.netpowerctrl.devices.Device;

/**
 * Inform of not reachable devices
 */
public interface NotReachableUpdate {
    void onNotReachableUpdate(List<Device> not_reachable);
}
