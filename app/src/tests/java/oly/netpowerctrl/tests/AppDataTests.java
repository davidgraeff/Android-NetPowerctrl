package oly.netpowerctrl.tests;

import android.test.AndroidTestCase;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.LoadStoreJSonData;
import oly.netpowerctrl.pluginservice.PluginService;

/**
 * Testing AppData class. AppData should be fully independent of PluginService.
 */
public class AppDataTests extends AndroidTestCase {
    AppData c;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        testAndroidTestCaseSetupProperly();
    }

    @Override
    protected void tearDown() throws Exception {
        c.clear();
        super.tearDown();
    }

    public void testMainApp() throws Exception {
        c = new AppData();
        c.setLoadStoreController(new TestObjects.LoadStoreJSonDataTest());
        assertNotNull(c);

        // Test if load store is set up.
        Field privateStringField = AppData.class.
                getDeclaredField("loadStoreJSonData");
        privateStringField.setAccessible(true);
        LoadStoreJSonData l = (LoadStoreJSonData) privateStringField.get(c);
        assertNotNull(l);
        assertEquals(l instanceof TestObjects.LoadStoreJSonDataTest, true);

        assertNull(PluginService.getService());

        Method method = AppData.class.
                getDeclaredMethod("updateDevice");
        method.invoke(c, TestObjects.createDevice());

        assertEquals(c.deviceCollection.size(), 0);
        assertEquals(c.unconfiguredDeviceCollection.size(), 1);

        c.addToConfiguredDevices(c.unconfiguredDeviceCollection.get(0));

        assertEquals(c.unconfiguredDeviceCollection.size(), 0);
        assertEquals(c.deviceCollection.size(), 1);
    }
}
