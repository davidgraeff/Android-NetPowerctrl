package oly.netpowerctrl.tests;

import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.data.LoadStoreCollections;
import oly.netpowerctrl.devices.Credentials;
import oly.netpowerctrl.plugin_anel.AnelPlugin;

;

/**
 * Mock objects for testing
 */
public class TestObjects {
    static Credentials createDevice() {
        Credentials di = new Credentials();
        di.pluginID = AnelPlugin.PLUGIN_ID;
        di.setDeviceName("TestDevice");
        di.deviceUID = ("aa:bb:cc:dd:ee:ff");
        di.userName = ("admin");
        di.password = ("anel");
        //di.putConnection(new IOConnectionUDP(di, "192.168.1.101", 1077, SharedPrefs.getInstance().getDefaultSendPort()));
        return di;
    }

    /**
     * Replace the original LoadStoreData class with a stub class.
     */
    public static class LoadStoreCollectionsTest extends LoadStoreCollections {
        public LoadStoreCollectionsTest() {
            super();
        }

        @Override
        public void loadData(final DataService pluginService) {
            pluginService.setDataLoadingCompleted();
        }
    }
}
