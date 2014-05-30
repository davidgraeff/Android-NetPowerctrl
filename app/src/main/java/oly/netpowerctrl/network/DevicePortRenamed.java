package oly.netpowerctrl.network;

import oly.netpowerctrl.devices.DevicePort;

/**
 * Implement this interface if you want to be notified of a failed/successful renaming of a deviceport.
 */
public interface DevicePortRenamed {
    // Your plugin always have to call this method.
    void devicePort_renamed(DevicePort oi, boolean success, String error_message);

    // Do not call this in your plugin!
    void devicePort_start_rename(@SuppressWarnings("UnusedParameters") DevicePort oi);
}
