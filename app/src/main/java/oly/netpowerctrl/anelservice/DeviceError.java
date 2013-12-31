package oly.netpowerctrl.anelservice;

/**
 * Created by david on 30.12.13.
 */
public interface DeviceError {
    void onDeviceError(String devicename, String errMessage);
}
