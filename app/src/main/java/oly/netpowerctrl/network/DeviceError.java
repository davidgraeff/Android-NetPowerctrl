package oly.netpowerctrl.network;

/**
 * on device error
 */
public interface DeviceError {
    void onDeviceError(String deviceName, String errMessage);
}
