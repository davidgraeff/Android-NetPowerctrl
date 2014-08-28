package oly.netpowerctrl.tests;

import android.content.Context;

import oly.netpowerctrl.anel.AnelPlugin;
import oly.netpowerctrl.application_state.LoadStoreData;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.devices.DeviceCollection;
import oly.netpowerctrl.devices.DeviceConnectionUDP;
import oly.netpowerctrl.groups.GroupCollection;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.scenes.SceneCollection;
import oly.netpowerctrl.timer.TimerController;

/**
 * Mock objects for testing
 */
public class TestObjects {
    static Device createDevice() {
        Device di = new Device(AnelPlugin.PLUGIN_ID);
        di.DeviceName = "TestDevice";
        di.UniqueDeviceID = "aa:bb:cc:dd:ee:ff";
        di.UserName = "admin";
        di.Password = "anel";
        di.addConnection(new DeviceConnectionUDP(di, "192.168.1.101", 1077, SharedPrefs.getInstance().getDefaultSendPort()));
        di.setUpdatedNow();
        return di;
    }

    /**
     * Created by david on 08.07.14.
     */
    public static class LoadStoreDataTest extends LoadStoreData {
        public LoadStoreDataTest(Context context) {
            super(context);
        }

        public void read(SceneCollection target) {

        }

        public void read(DeviceCollection target) {

        }

        public void read(GroupCollection target) {

        }

        public void read(TimerController target) {

        }

        public void markVersion() {
        }
    }
}
