package oly.netpowerctrl.tests;

import android.test.AndroidTestCase;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.onDataQueryCompleted;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.listen_service.ListenService;
import oly.netpowerctrl.listen_service.onServiceReady;
import oly.netpowerctrl.listen_service.onServiceRefreshQuery;
import oly.netpowerctrl.network.onDeviceObserverFinishedResult;

/**
 * Created by david on 08.07.14.
 */
public class ScanBroadcast extends AndroidTestCase {
    private AppData c;
    private int refreshStartedStopped_signal = 0;
    private final onServiceRefreshQuery refreshStartedStopped = new onServiceRefreshQuery() {
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

        c = AppData.getInstance();
        c.useAppData(new TestObjects.LoadStoreJSonDataTest());
        assertNotNull(c);

        assertEquals(ListenService.getUsedCount(), 0);
        assertNull(ListenService.getService());
        ListenService.useService(getContext(), false, false);
        assertEquals(ListenService.getUsedCount(), 1);

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
        assertEquals(ListenService.getUsedCount(), 1);
    }

    @Override
    protected void tearDown() throws Exception {
        assertEquals(ListenService.getUsedCount(), 1);

        final CountDownLatch signal = new CountDownLatch(1);
        ListenService.observersServiceReady.register(new onServiceReady() {
            @Override
            public boolean onServiceReady(ListenService service) {
                return false;
            }

            @Override
            public void onServiceFinished() {
                signal.countDown();
            }
        });

        ListenService.stopUseService();
        signal.await(4, TimeUnit.SECONDS);

        assertNull(ListenService.getService());

        c.clear();

        super.tearDown();
    }

    public void testScanBroadcast() throws Exception {
        ListenService service = ListenService.getService();
        assertNotNull(service);

        // DataQueryCompleted should be issued and onObserverJobFinished
        final CountDownLatch signal_receive = new CountDownLatch(2);

        AppData.observersDataQueryCompleted.register(new onDataQueryCompleted() {
            @Override
            public boolean onDataQueryFinished() {
                signal_receive.countDown();
                return false;
            }
        });

        ListenService.observersStartStopRefresh.register(refreshStartedStopped);

        service.findDevices(false, new onDeviceObserverFinishedResult() {
            @Override
            public void onObserverJobFinished(List<Device> timeout_devices) {
                assertTrue(timeout_devices.size() == 0);
                signal_receive.countDown();
            }
        });

        assertTrue("Timeout of findDevices", signal_receive.await(4, TimeUnit.SECONDS));

        ListenService.observersStartStopRefresh.unregister(refreshStartedStopped);
        assertEquals("RefreshStartStop count wrong", 0, refreshStartedStopped_signal);

        assertTrue("No devices found!", AppData.getInstance().unconfiguredDeviceCollection.size() > 0);
    }
}
