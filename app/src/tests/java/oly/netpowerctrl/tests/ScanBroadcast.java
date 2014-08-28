package oly.netpowerctrl.tests;

import android.test.AndroidTestCase;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import oly.netpowerctrl.application_state.NetpowerctrlService;
import oly.netpowerctrl.application_state.RuntimeDataController;
import oly.netpowerctrl.application_state.onDataQueryCompleted;
import oly.netpowerctrl.application_state.onRefreshStartedStopped;
import oly.netpowerctrl.application_state.onServiceReady;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.network.DeviceObserverFinishedResult;

/**
 * Created by david on 08.07.14.
 */
public class ScanBroadcast extends AndroidTestCase {

    int refreshStartedStopped_signal = 0;
    private onRefreshStartedStopped refreshStartedStopped = new onRefreshStartedStopped() {
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

        RuntimeDataController c = RuntimeDataController.createRuntimeDataController(
                new TestObjects.LoadStoreDataTest(getContext()));
        assertNotNull(c);

        assertEquals(NetpowerctrlService.getUsedCount(), 0);
        assertNull(NetpowerctrlService.getService());
        NetpowerctrlService.useService(getContext(), false, false);
        assertEquals(NetpowerctrlService.getUsedCount(), 1);

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
        assertEquals(NetpowerctrlService.getUsedCount(), 1);
    }

    @Override
    protected void tearDown() throws Exception {
        assertEquals(NetpowerctrlService.getUsedCount(), 1);

        final CountDownLatch signal = new CountDownLatch(1);
        NetpowerctrlService.observersServiceReady.register(new onServiceReady() {
            @Override
            public boolean onServiceReady(NetpowerctrlService service) {
                return false;
            }

            @Override
            public void onServiceFinished() {
                signal.countDown();
            }
        });

        NetpowerctrlService.stopUseService();
        signal.await(4, TimeUnit.SECONDS);

        assertNull(NetpowerctrlService.getService());

        super.tearDown();
    }

    public void testScanBroadcast() throws Exception {
        NetpowerctrlService service = NetpowerctrlService.getService();
        assertNotNull(service);

        // DataQueryCompleted should be issued and onObserverJobFinished
        final CountDownLatch signal_receive = new CountDownLatch(2);

        RuntimeDataController.observersDataQueryCompleted.register(new onDataQueryCompleted() {
            @Override
            public boolean onDataQueryFinished() {
                signal_receive.countDown();
                return false;
            }
        });

        NetpowerctrlService.observersStartStopRefresh.register(refreshStartedStopped);

        service.findDevices(false, new DeviceObserverFinishedResult() {
            @Override
            public void onObserverJobFinished(List<Device> timeout_devices) {
                assertTrue(timeout_devices.size() == 0);
                signal_receive.countDown();
            }
        });

        assertTrue("Timeout of findDevices", signal_receive.await(4, TimeUnit.SECONDS));

        NetpowerctrlService.observersStartStopRefresh.unregister(refreshStartedStopped);
        assertEquals("RefreshStartStop count wrong", 0, refreshStartedStopped_signal);

        assertTrue("No devices found!", RuntimeDataController.getDataController().newDevices.size() > 0);
    }
}
