package oly.netpowerctrl.network;

import oly.netpowerctrl.datastructure.DevicePort;

/**
 * Implement this interface if you want to be notified of a failed/successful renaming of a deviceport.
 */
public interface DevicePortRenamed {
    void devicePort_renamed(DevicePort oi, boolean success, String error_message);

    void devicePort_start_rename(DevicePort oi);
}