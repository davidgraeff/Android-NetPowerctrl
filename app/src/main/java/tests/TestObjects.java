package tests;

import oly.netpowerctrl.anel.AnelPlugin;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.devices.DeviceConnectionUDP;
import oly.netpowerctrl.preferences.SharedPrefs;

/**
 * Created by david on 08.07.14.
 */
public class TestObjects {
    static Device createDevice() {
        Device di = Device.createNewDevice(AnelPlugin.PLUGIN_ID);
        di.DeviceName = "TestDevice";
        di.UniqueDeviceID = "aa:bb:cc:dd:ee:ff";
        di.UserName = "admin";
        di.Password = "anel";
        di.addConnection(new DeviceConnectionUDP(di, "192.168.1.101", 1077, SharedPrefs.getDefaultSendPort()));
        di.setUpdatedNow();
        return di;
    }
}
