package oly.netpowerctrl.data.query;

import android.widget.Toast;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.main.App;

/**
 * Created by david on 01.12.14.
 */
public class DataQueryFinishedMessageRunnable implements Runnable {
    private DataService dataService;

    public DataQueryFinishedMessageRunnable(DataService dataService) {
        this.dataService = dataService;
    }

    public static void show(DataQueryFinishedMessageRunnable afterDataQueryFinishedHandler) {
        App.getMainThreadHandler().postDelayed(afterDataQueryFinishedHandler, 500);
    }

    @Override
    public void run() {
        //noinspection ConstantConditions
        Toast.makeText(App.instance,
                App.instance.getString(R.string.devices_refreshed,
                        dataService.connections.getRecentlyDetectedDevices(true, 1500),
                        dataService.connections.getRecentlyDetectedDevices(false, 1500)),
                Toast.LENGTH_SHORT
        ).show();
    }
}
