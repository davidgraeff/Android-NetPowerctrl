package oly.netpowerctrl.tests;

import android.test.AndroidTestCase;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.data.LoadStoreCollections;
import oly.netpowerctrl.devices.Credentials;

;

/**
 * Testing DataService class. DataService should be fully independent of DataService.
 */
public class AppDataTests extends AndroidTestCase {
    DataService c;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        testAndroidTestCaseSetupProperly();
    }

    @Override
    protected void tearDown() throws Exception {
        c.clearDataStorage();
        super.tearDown();
    }

    public void testMainApp() throws Exception {
        c = new DataService();
        c.setLoadStoreController(new TestObjects.LoadStoreCollectionsTest());
        assertNotNull(c);

        // Test if load store is set up.
        Field privateStringField = DataService.class.
                getDeclaredField("loadStoreJSonData");
        privateStringField.setAccessible(true);
        LoadStoreCollections l = (LoadStoreCollections) privateStringField.get(c);
        assertNotNull(l);
        assertEquals(l instanceof TestObjects.LoadStoreCollectionsTest, true);

        assertNull(DataService.getService());

        Credentials credentials = TestObjects.createDevice();

        Method method = DataService.class.
                getDeclaredMethod("updateCredentials");
        method.invoke(c, credentials);

        assertEquals(c.credentials.size(), 0);

        c.addToConfiguredDevices(credentials);

        assertEquals(c.credentials.size(), 1);
    }
}
