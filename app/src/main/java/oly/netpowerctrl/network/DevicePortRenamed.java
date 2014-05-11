package oly.netpowerctrl.network;

import oly.netpowerctrl.devices.DevicePort;

/**
 * Implement this interface if you want to be notified of a failed/successful renaming of a deviceport.
 */
public interface DevicePortRenamed {
    void devicePort_renamed(DevicePort oi, boolean success, String error_message);

    void devicePort_start_rename(@SuppressWarnings("UnusedParameters") DevicePort oi);
}
