package oly.netpowerctrl.devices;

import android.content.Context;

import oly.netpowerctrl.device_base.device.Device;

/**
 * Created by david on 05.09.14.
 */
public interface EditDeviceInterface {
    Device getDevice();

    void setDevice(Device device);

    void setResultListener(onCreateDeviceResult createDeviceResult);

    boolean isTesting();

    boolean startTest(Context context);

    boolean wakeupPlugin(Context context);

    boolean isTestOK();

    enum TestStates {TEST_INIT, TEST_REACHABLE, TEST_ACCESS, TEST_OK}
}
