package oly.netpowerctrl.devices;

import java.util.List;

/**
 * Inform of not reachable devices
 */
public interface NotReachableUpdate {
    void onNotReachableUpdate(List<DeviceInfo> not_reachable);
}
