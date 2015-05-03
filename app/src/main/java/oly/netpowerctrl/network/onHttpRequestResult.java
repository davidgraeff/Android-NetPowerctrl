package oly.netpowerctrl.network;

import oly.netpowerctrl.executables.Executable;

/**
 * Implement this interface if you want to be notified of a failed/successful http request of a DevicePort.
 */
public interface onHttpRequestResult {
    // Plugin Developer: Your plugin always have to call this method.
    void httpRequestResult(Executable oi, boolean success, String error_message);

    // Plugin Developer: Do not call this in your plugin!
    void httpRequestStart(@SuppressWarnings("UnusedParameters") Executable oi);
}
