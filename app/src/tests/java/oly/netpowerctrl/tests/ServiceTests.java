package oly.netpowerctrl.tests;

import android.test.AndroidTestCase;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.LoadStoreJSonData;
import oly.netpowerctrl.listen_service.ListenService;
import oly.netpowerctrl.listen_service.onServiceReady;

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
        AppData c = AppData.getInstance();
        c.useAppData(new TestObjects.LoadStoreJSonDataTest());
        assertNotNull(c);

        // Test if load store is set up.
        Field privateStringField = AppData.class.
                getDeclaredField("loadStoreData");
        privateStringField.setAccessible(true);
        LoadStoreJSonData l = (LoadStoreJSonData) privateStringField.get(c);
        assertNotNull(l);
        assertEquals(l instanceof TestObjects.LoadStoreJSonDataTest, true);

        assertNull(ListenService.getService());
        ListenService.useService(getContext(), false, false);

        final CountDownLatch signal = new CountDownLatch(1);
        ListenService.observersServiceReady.register(new onServiceReady() {
            @Override
            public boolean onServiceReady(ListenService service) {
                signal.countDown();
                return false;
            }

            @Override
            public void onServiceFinished() {
            }
        });

        signal.await(4, TimeUnit.SECONDS);

        ListenService service = ListenService.getService();
        assertNotNull(service);
        assertEquals(service, ListenService.getService());

        assertEquals(ListenService.getUsedCount(), 1);

        final CountDownLatch shutDownSignal = new CountDownLatch(1);
        ListenService.observersServiceReady.register(new onServiceReady() {
            @Override
            public boolean onServiceReady(ListenService service) {
                return false;
            }

            @Override
            public void onServiceFinished() {
                shutDownSignal.countDown();
            }
        });

        ListenService.stopUseService();
        shutDownSignal.await(4, TimeUnit.SECONDS);

        assertNull(ListenService.getService());
    }
}
