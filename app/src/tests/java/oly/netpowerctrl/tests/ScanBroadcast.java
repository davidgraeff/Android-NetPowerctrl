package oly.netpowerctrl.tests;

import android.test.AndroidTestCase;

import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.onDataQueryCompleted;
import oly.netpowerctrl.data.onDataQueryRefreshQuery;
import oly.netpowerctrl.pluginservice.PluginService;
import oly.netpowerctrl.pluginservice.onServiceReady;

/**
 * Created by david on 08.07.14.
 */
public class ScanBroadcast extends AndroidTestCase {
    private AppData c;
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

        c = AppData.getInstance();
        c.useAppData(new TestObjects.LoadStoreJSonDataTest());
        assertNotNull(c);

        assertEquals(PluginService.isServiceUsed(), false);
        assertNull(PluginService.getService());
        PluginService.useService(new WeakReference<Object>(this));
        assertEquals(PluginService.isServiceUsed(), true);

        final CountDownLatch signal = new CountDownLatch(1);
        PluginService.observersServiceReady.register(new onServiceReady() {
            @Override
            public boolean onServiceReady(PluginService service) {
                signal.countDown();
                return false;
            }

            @Override
            public void onServiceFinished() {
                signal.countDown();
            }
        });

        signal.await(4, TimeUnit.SECONDS);
        assertEquals(PluginService.isServiceUsed(), true);
    }

    @Override
    protected void tearDown() throws Exception {
        assertEquals(PluginService.isServiceUsed(), true);

        final CountDownLatch signal = new CountDownLatch(1);
        PluginService.observersServiceReady.register(new onServiceReady() {
            @Override
            public boolean onServiceReady(PluginService service) {
                return false;
            }

            @Override
            public void onServiceFinished() {
                signal.countDown();
            }
        });

        PluginService.stopUseService(this);
        signal.await(4, TimeUnit.SECONDS);

        assertNull(PluginService.getService());

        c.clear();

        super.tearDown();
    }

    public void testScanBroadcast() throws Exception {
        PluginService service = PluginService.getService();
        assertNotNull(service);

        // DataQueryCompleted should be issued and onObserverJobFinished
        final CountDownLatch signal_receive = new CountDownLatch(1);

        AppData.observersDataQueryCompleted.register(new onDataQueryCompleted() {
            @Override
            public boolean onDataQueryFinished(boolean networkDevicesNotReachable) {
                signal_receive.countDown();
                return false;
            }
        });

        AppData.observersStartStopRefresh.register(refreshStartedStopped);

        AppData.getInstance().refreshDeviceData(false);

        assertTrue("Timeout of refreshDeviceData", signal_receive.await(4, TimeUnit.SECONDS));

        AppData.observersStartStopRefresh.unregister(refreshStartedStopped);
        assertEquals("RefreshStartStop count wrong", 0, refreshStartedStopped_signal);

        assertTrue("No devices found!", AppData.getInstance().unconfiguredDeviceCollection.size() > 0);
    }
}
