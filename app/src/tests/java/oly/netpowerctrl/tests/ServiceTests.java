package oly.netpowerctrl.tests;

import android.test.AndroidTestCase;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.LoadStoreJSonData;
import oly.netpowerctrl.pluginservice.PluginService;
import oly.netpowerctrl.pluginservice.onServiceReady;

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

        assertNull(PluginService.getService());
        PluginService.useService(new WeakReference<Object>(this));

        final CountDownLatch signal = new CountDownLatch(1);
        PluginService.observersServiceReady.register(new onServiceReady() {
            @Override
            public boolean onServiceReady(PluginService service) {
                signal.countDown();
                return false;
            }

            @Override
            public void onServiceFinished() {
            }
        });

        signal.await(4, TimeUnit.SECONDS);

        PluginService service = PluginService.getService();
        assertNotNull(service);
        assertEquals(service, PluginService.getService());

        assertEquals(PluginService.isServiceUsed(), true);

        final CountDownLatch shutDownSignal = new CountDownLatch(1);
        PluginService.observersServiceReady.register(new onServiceReady() {
            @Override
            public boolean onServiceReady(PluginService service) {
                return false;
            }

            @Override
            public void onServiceFinished() {
                shutDownSignal.countDown();
            }
        });

        PluginService.stopUseService(this);
        shutDownSignal.await(4, TimeUnit.SECONDS);

        assertEquals(PluginService.isServiceUsed(), false);
        assertNull(PluginService.getService());
    }
}
