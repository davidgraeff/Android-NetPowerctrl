package oly.netpowerctrl.devices;

import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.pluginservice.PluginService;

/**
 * Created by david on 05.09.14.
 */
public interface EditDeviceInterface {
    Device getDevice();

    void setDevice(Device device);

    void setResultListener(onCreateDeviceResult createDeviceResult);

    TestStates getTestState();

    boolean startTest(PluginService context);

    enum TestStates {TEST_INIT, TEST_REACHABLE, TEST_ACCESS, TEST_OK}
}
