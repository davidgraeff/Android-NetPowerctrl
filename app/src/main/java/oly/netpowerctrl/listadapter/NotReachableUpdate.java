package oly.netpowerctrl.listadapter;

import java.util.List;

import oly.netpowerctrl.datastructure.DeviceInfo;

/**
 * Created by david on 07.01.14.
 */
public interface NotReachableUpdate {
    void onNotReachableUpdate(List<DeviceInfo> not_reachable);
}
