package oly.netpowerctrl.tests;

import android.test.AndroidTestCase;

import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.data.onServiceReady;

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
        assertNull(DataService.getService());
        DataService.useService(new WeakReference<Object>(this));

        final CountDownLatch signal = new CountDownLatch(1);
        DataService.observersServiceReady.register(new onServiceReady() {
            @Override
            public boolean onServiceReady(DataService service) {
                signal.countDown();
                return false;
            }

            @Override
            public void onServiceFinished(DataService service) {
            }
        });

        signal.await(4, TimeUnit.SECONDS);

        DataService service = DataService.getService();
        assertNotNull(service);
        assertEquals(service, DataService.getService());

        assertEquals(DataService.isServiceUsed(), true);

        final CountDownLatch shutDownSignal = new CountDownLatch(1);
        DataService.observersServiceReady.register(new onServiceReady() {
            @Override
            public boolean onServiceReady(DataService service) {
                return false;
            }

            @Override
            public void onServiceFinished(DataService service) {
                shutDownSignal.countDown();
            }
        });

        DataService.stopUseService(this);
        shutDownSignal.await(4, TimeUnit.SECONDS);

        assertEquals(DataService.isServiceUsed(), false);
        assertNull(DataService.getService());
    }
}
