package oly.netpowerctrl.tests;

import android.test.AndroidTestCase;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import oly.netpowerctrl.application_state.LoadStoreData;
import oly.netpowerctrl.application_state.NetpowerctrlService;
import oly.netpowerctrl.application_state.RuntimeDataController;
import oly.netpowerctrl.application_state.onServiceReady;

/**
 * Testing service start and shutdown
 */
public class ServiceTests extends AndroidTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        testAndroidTestCaseSetupProperly();
//        assertEquals(getApplication(), oly.netpowerctrl.application_state.NetpowerctrlApplication.instance);
    }

    public void testServiceInit() throws Exception {
        RuntimeDataController c = RuntimeDataController.createRuntimeDataController(
                new TestObjects.LoadStoreDataTest(getContext()));
        assertNotNull(c);

        // Test if load store is set up.
        Field privateStringField = RuntimeDataController.class.
                getDeclaredField("loadStoreData");
        privateStringField.setAccessible(true);
        LoadStoreData l = (LoadStoreData) privateStringField.get(c);
        assertNotNull(l);
        assertEquals(l instanceof TestObjects.LoadStoreDataTest, true);

        assertNull(NetpowerctrlService.getService());
        NetpowerctrlService.useService(getContext(), false, false);

        final CountDownLatch signal = new CountDownLatch(1);
        NetpowerctrlService.observersServiceReady.register(new onServiceReady() {
            @Override
            public boolean onServiceReady(NetpowerctrlService service) {
                signal.countDown();
                return false;
            }

            @Override
            public void onServiceFinished() {
            }
        });

        signal.await(4, TimeUnit.SECONDS);

        NetpowerctrlService service = NetpowerctrlService.getService();
        assertNotNull(service);
        assertEquals(service, NetpowerctrlService.getService());

        assertEquals(NetpowerctrlService.getUsedCount(), 1);

        final CountDownLatch shutDownSignal = new CountDownLatch(1);
        NetpowerctrlService.observersServiceReady.register(new onServiceReady() {
            @Override
            public boolean onServiceReady(NetpowerctrlService service) {
                return false;
            }

            @Override
            public void onServiceFinished() {
                shutDownSignal.countDown();
            }
        });

        NetpowerctrlService.stopUseService();
        shutDownSignal.await(4, TimeUnit.SECONDS);

        assertNull(NetpowerctrlService.getService());
    }
}
