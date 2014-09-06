package oly.netpowerctrl.devices;

/**
 * Created by david on 05.09.14.
 */
public interface onCreateDeviceResult {
    void testFinished(boolean success);

    void testDeviceNotReachable();
}
