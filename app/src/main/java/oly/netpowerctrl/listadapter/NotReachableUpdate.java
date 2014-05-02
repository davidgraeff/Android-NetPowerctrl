package oly.netpowerctrl.listadapter;

import java.util.List;

import oly.netpowerctrl.datastructure.DeviceInfo;

/**
 * Inform of not reachable devices
 */
public interface NotReachableUpdate {
    void onNotReachableUpdate(List<DeviceInfo> not_reachable);
}
