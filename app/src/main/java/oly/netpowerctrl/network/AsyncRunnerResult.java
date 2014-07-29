package oly.netpowerctrl.network;

import oly.netpowerctrl.device_ports.DevicePort;

/**
 * Implement this interface if you want to be notified of a failed/successful renaming of a deviceport.
 */
public interface AsyncRunnerResult {
    // Your plugin always have to call this method.
    void asyncRunnerResult(DevicePort oi, boolean success, String error_message);

    // Do not call this in your plugin!
    void asyncRunnerStart(@SuppressWarnings("UnusedParameters") DevicePort oi);
}
