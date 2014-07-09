package tests;

import oly.netpowerctrl.anel.AnelPlugin;
import oly.netpowerctrl.devices.DeviceInfo;
import oly.netpowerctrl.preferences.SharedPrefs;

/**
 * Created by david on 08.07.14.
 */
public class TestObjects {
    static DeviceInfo createDevice() {
        DeviceInfo di = DeviceInfo.createNewDevice(AnelPlugin.PLUGIN_ID);
        di.DeviceName = "TestDevice";
        di.HostName = "192.168.1.101";
        di.UniqueDeviceID = "aa:bb:cc:dd:ee:ff";
        di.ReceivePort = 1077;
        di.UserName = "admin";
        di.Password = "anel";
        di.SendPort = SharedPrefs.getDefaultSendPort();
        di.setReachable();
        di.setUpdatedNow();
        return di;
    }
}
