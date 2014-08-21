package tests;

import android.test.ApplicationTestCase;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import oly.netpowerctrl.application_state.LoadStoreData;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.application_state.NetpowerctrlService;
import oly.netpowerctrl.application_state.RuntimeDataController;
import oly.netpowerctrl.application_state.ServiceReady;

/**
 * Created by david on 08.07.14.
 */
public class ServiceTests extends ApplicationTestCase<NetpowerctrlApplicationTest> {
    public ServiceTests() {
        super(NetpowerctrlApplicationTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
//        setContext(new ContextWrapper(new MockContext()) {
//            @Override
//            public Object getSystemService(String name) {
//                return null;
//            }
//        });
        createApplication();
        testAndroidTestCaseSetupProperly();
        assertEquals(getApplication(), NetpowerctrlApplication.instance);
    }

    public void testServiceInit() throws Exception {
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
        NetpowerctrlService.useService(false, false);

        final CountDownLatch signal = new CountDownLatch(1);
        NetpowerctrlService.observersServiceReady.register(new ServiceReady() {
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
        NetpowerctrlApplication.instance = getApplication(); //HACK

        NetpowerctrlService service = NetpowerctrlService.getService();
        assertNotNull(service);
        assertEquals(service, NetpowerctrlService.getService());


    }
}
