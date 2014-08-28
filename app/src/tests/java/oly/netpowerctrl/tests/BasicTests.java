package oly.netpowerctrl.tests;

import android.test.AndroidTestCase;

import java.lang.reflect.Field;

import oly.netpowerctrl.application_state.LoadStoreData;
import oly.netpowerctrl.application_state.NetpowerctrlService;
import oly.netpowerctrl.application_state.RuntimeDataController;
import oly.netpowerctrl.preferences.SharedPrefs;

/**
 * Created by david on 08.07.14.
 */
public class BasicTests extends AndroidTestCase {
    RuntimeDataController c;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        testAndroidTestCaseSetupProperly();
        c = RuntimeDataController.createRuntimeDataController(
                new TestObjects.LoadStoreDataTest(getContext()));
        assertNotNull(c);
        new SharedPrefs(getContext());
    }

    @Override
    protected void tearDown() throws Exception {
        c.finish();
        super.tearDown();
    }

    public void testMainApp() throws Exception {
        // Test if load store is set up.
        Field privateStringField = RuntimeDataController.class.
                getDeclaredField("loadStoreData");
        privateStringField.setAccessible(true);
        LoadStoreData l = (LoadStoreData) privateStringField.get(c);
        assertNotNull(l);
        assertEquals(l instanceof TestObjects.LoadStoreDataTest, true);

        assertNull(NetpowerctrlService.getService());

        c.onDeviceUpdated(TestObjects.createDevice());

        assertEquals(c.deviceCollection.devices.size(), 0);
        assertEquals(c.newDevices.size(), 1);

        c.addToConfiguredDevices(getContext(), c.newDevices.get(0));

        assertEquals(c.newDevices.size(), 0);
        assertEquals(c.deviceCollection.devices.size(), 1);
    }
}
