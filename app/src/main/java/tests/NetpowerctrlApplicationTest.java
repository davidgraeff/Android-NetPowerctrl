package tests;

import oly.netpowerctrl.application_state.LoadStoreData;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.application_state.RuntimeDataController;

/**
 * Created by david on 08.07.14.
 */
public class NetpowerctrlApplicationTest extends NetpowerctrlApplication {
    @Override
    public void onCreate() {
        instance = this;
        dataController = new RuntimeDataController();
        LoadStoreData loadStoreData = new LoadStoreDataTest();
        dataController.setLoadStoreProvider(loadStoreData);
    }
}
