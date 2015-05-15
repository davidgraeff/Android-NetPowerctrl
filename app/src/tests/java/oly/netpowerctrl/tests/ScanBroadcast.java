package oly.netpowerctrl.tests;

import android.test.AndroidTestCase;

import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.data.onServiceReady;
import oly.netpowerctrl.data.query.onDataQueryCompleted;
import oly.netpowerctrl.data.query.onDataQueryRefreshQuery;

;

/**
 * Created by david on 08.07.14.
 */
public class ScanBroadcast extends AndroidTestCase {
    private DataService c;
    private int refreshStartedStopped_signal = 0;
    private final onDataQueryRefreshQuery refreshStartedStopped = new onDataQueryRefreshQuery() {
        @Override
        public void onRefreshStateChanged(boolean isRefreshing) {
            if (isRefreshing)
                ++refreshStartedStopped_signal;
            else
                --refreshStartedStopped_signal;
        }
    };

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        testAndroidTestCaseSetupProperly();
//        assertEquals(getApplication(), NetpowerctrlApplication.instance);
//        assertNotNull(getApplication());

        assertEquals(DataService.isServiceUsed(), false);
        assertNull(DataService.getService());
        DataService.useService(new WeakReference<Object>(this));
        assertEquals(DataService.isServiceUsed(), true);

        final CountDownLatch signal = new CountDownLatch(1);
        DataService.observersServiceReady.register(new onServiceReady() {
            @Override
            public boolean onServiceReady(DataService service) {
                signal.countDown();
                return false;
            }

            @Override
            public void onServiceFinished(DataService service) {
                signal.countDown();
            }
        });

        signal.await(4, TimeUnit.SECONDS);
        assertEquals(DataService.isServiceUsed(), true);
    }

    @Override
    protected void tearDown() throws Exception {
        assertEquals(DataService.isServiceUsed(), true);

        final CountDownLatch signal = new CountDownLatch(1);
        DataService.observersServiceReady.register(new onServiceReady() {
            @Override
            public boolean onServiceReady(DataService service) {
                return false;
            }

            @Override
            public void onServiceFinished(DataService service) {
                signal.countDown();
            }
        });

        DataService.stopUseService(this);
        signal.await(4, TimeUnit.SECONDS);

        assertNull(DataService.getService());

        c.clearDataStorage();

        super.tearDown();
    }

    public void testScanBroadcast() throws Exception {
        DataService service = DataService.getService();
        assertNotNull(service);

        // DataQueryCompleted should be issued and onObserverJobFinished
        final CountDownLatch signal_receive = new CountDownLatch(1);

        DataService.observersDataQueryCompleted.register(new onDataQueryCompleted() {
            @Override
            public boolean onDataQueryFinished(DataService dataService) {
                signal_receive.countDown();
                return false;
            }
        });

        DataService.observersStartStopRefresh.register(refreshStartedStopped);

        service.detectDevices();

        assertTrue("Timeout of refreshExistingDevices", signal_receive.await(4, TimeUnit.SECONDS));

        DataService.observersStartStopRefresh.unregister(refreshStartedStopped);
        assertEquals("RefreshStartStop count wrong", 0, refreshStartedStopped_signal);

        assertTrue("No devices found!", service.connections.getRecentlyDetectedDevices(true, 1500) > 0);
    }
}
