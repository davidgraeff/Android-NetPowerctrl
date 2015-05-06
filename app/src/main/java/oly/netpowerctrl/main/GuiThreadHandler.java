package oly.netpowerctrl.main;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.DataService;

/**
 * Created by david on 04.05.15.
 */
public class GuiThreadHandler extends Handler {
    public static final int SERVICE_DELAYED_EXIT = 1816;
    public static final int SERVICE_SHOW_DETECTED_MESSAGE = 1817;

    GuiThreadHandler() {
        super(Looper.getMainLooper());
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case SERVICE_DELAYED_EXIT:
                DataService.finishService();
                break;
            case SERVICE_SHOW_DETECTED_MESSAGE:
                DataService dataService = DataService.getService();
                //noinspection ConstantConditions
                Toast.makeText(App.instance,
                        App.instance.getString(R.string.devices_refreshed,
                                dataService.connections.getRecentlyDetectedDevices(true, 1500),
                                dataService.connections.getRecentlyDetectedDevices(false, 1500)),
                        Toast.LENGTH_SHORT
                ).show();
                break;
            default:
                super.handleMessage(msg);
        }
    }
}
