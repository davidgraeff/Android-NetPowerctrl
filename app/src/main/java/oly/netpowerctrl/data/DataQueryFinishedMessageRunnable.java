package oly.netpowerctrl.data;

import android.widget.Toast;

import oly.netpowerctrl.R;
import oly.netpowerctrl.main.App;

/**
 * Created by david on 01.12.14.
 */
class DataQueryFinishedMessageRunnable implements Runnable {
    private AppData appData;

    public DataQueryFinishedMessageRunnable(AppData appData) {
        this.appData = appData;
    }

    static void show(DataQueryFinishedMessageRunnable afterDataQueryFinishedHandler) {
        App.getMainThreadHandler().postDelayed(afterDataQueryFinishedHandler, 500);
    }

    @Override
    public void run() {
        //noinspection ConstantConditions
        Toast.makeText(App.instance,
                App.instance.getString(R.string.devices_refreshed,
                        appData.getReachableConfiguredDevices(),
                        appData.unconfiguredDeviceCollection.size()),
                Toast.LENGTH_SHORT
        ).show();
    }
}
