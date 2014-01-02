package oly.netpowerctrl.anelservice;

/**
 * on device error
 */
public interface DeviceError {
    void onDeviceError(String deviceName, String errMessage);
}
