package oly.netpowerctrl.tests;

import oly.netpowerctrl.anel.AnelPlugin;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.LoadStoreJSonData;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DeviceConnectionUDP;

/**
 * Mock objects for testing
 */
public class TestObjects {
    static Device createDevice() {
        Device di = new Device(AnelPlugin.PLUGIN_ID);
        di.DeviceName = "TestDevice";
        di.setUniqueDeviceID("aa:bb:cc:dd:ee:ff");
        di.UserName = "admin";
        di.Password = "anel";
        di.addConnection(new DeviceConnectionUDP(di, "192.168.1.101", 1077, SharedPrefs.getInstance().getDefaultSendPort()));
        di.setUpdatedNow();
        return di;
    }

    /**
     * Replace the original LoadStoreData class with a stub class.
     */
    public static class LoadStoreJSonDataTest extends LoadStoreJSonData {
        public LoadStoreJSonDataTest() {
            super();
        }

        @Override
        public void loadData(final AppData appData) {
            // Do nothing
            // Notify data is loaded now
            AppData.observersOnDataLoaded.onDataLoaded();
        }

        @Override
        public void finish() {
        }

        public void markVersion() {
        }
    }
}
