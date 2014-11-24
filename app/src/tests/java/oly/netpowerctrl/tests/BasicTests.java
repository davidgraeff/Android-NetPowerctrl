package oly.netpowerctrl.tests;

import android.test.AndroidTestCase;

import java.lang.reflect.Field;

import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.LoadStoreJSonData;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.pluginservice.PluginService;

/**
 * Created by david on 08.07.14.
 */
public class BasicTests extends AndroidTestCase {
    AppData c;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        testAndroidTestCaseSetupProperly();
        c = AppData.getInstance();
        c.useAppData(new TestObjects.LoadStoreJSonDataTest());
        assertNotNull(c);
        SharedPrefs.getInstance();
    }

    @Override
    protected void tearDown() throws Exception {
        c.clear();
        super.tearDown();
    }

    public void testMainApp() throws Exception {
        // Test if load store is set up.
        Field privateStringField = AppData.class.
                getDeclaredField("loadStoreData");
        privateStringField.setAccessible(true);
        LoadStoreJSonData l = (LoadStoreJSonData) privateStringField.get(c);
        assertNotNull(l);
        assertEquals(l instanceof TestObjects.LoadStoreJSonDataTest, true);

        assertNull(PluginService.getService());

        c.updateDevice(TestObjects.createDevice());

        assertEquals(c.deviceCollection.size(), 0);
        assertEquals(c.unconfiguredDeviceCollection.size(), 1);

        c.addToConfiguredDevices(getContext(), c.unconfiguredDeviceCollection.get(0));

        assertEquals(c.unconfiguredDeviceCollection.size(), 0);
        assertEquals(c.deviceCollection.size(), 1);
    }
}
