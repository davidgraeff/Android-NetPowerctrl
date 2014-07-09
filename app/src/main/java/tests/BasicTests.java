package tests;

import android.content.Intent;
import android.test.ApplicationTestCase;

import java.lang.reflect.Field;

import oly.netpowerctrl.application_state.LoadStoreData;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.application_state.NetpowerctrlService;
import oly.netpowerctrl.application_state.RuntimeDataController;
import oly.netpowerctrl.main.MainActivity;

/**
 * Created by david on 08.07.14.
 */
public class BasicTests extends ApplicationTestCase<NetpowerctrlApplicationTest> {
    public BasicTests() {
        super(NetpowerctrlApplicationTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createApplication();
        testAndroidTestCaseSetupProperly();
    }

    public void testMainApp() throws Exception {
        assertNotNull(getApplication());
        RuntimeDataController c = NetpowerctrlApplication.getDataController();
        assertNotNull(c);
        c.setLoadStoreProvider(new LoadStoreDataTest());

        // Test if load store is set up.
        Field privateStringField = RuntimeDataController.class.
                getDeclaredField("loadStoreData");
        privateStringField.setAccessible(true);
        LoadStoreData l = (LoadStoreData) privateStringField.get(c);
        assertNotNull(l);
        assertEquals(l instanceof LoadStoreDataTest, true);

        assertNull(NetpowerctrlService.getService());

        c.onDeviceUpdated(TestObjects.createDevice());

        assertEquals(c.deviceCollection.devices.size(), 0);
        assertEquals(c.newDevices.size(), 1);

        c.addToConfiguredDevices(c.newDevices.get(0));

        assertEquals(c.newDevices.size(), 0);
        assertEquals(c.deviceCollection.devices.size(), 1);

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClass(getContext(), MainActivity.class);
        getContext().startActivity(intent);
    }
}
