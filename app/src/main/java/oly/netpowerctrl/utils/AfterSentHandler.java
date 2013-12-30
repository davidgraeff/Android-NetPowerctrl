package oly.netpowerctrl.utils;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.OutletInfo;
import oly.netpowerctrl.listadapter.OutletSwitchListAdapter;
import oly.netpowerctrl.network.UDPSendToDevice;

public class AfterSentHandler extends Handler {
    private int list_position;
    private boolean state;    // the state we want the outlet to be in
    private int retries;
    private static final int WHAT = 111;
    private final OutletSwitchListAdapter ola;

    public AfterSentHandler(OutletSwitchListAdapter l) {
        ola = l;
    }

    public void startDelayedCheck() {
        Message m = this.obtainMessage();
        m.what = WHAT;
        sendMessageDelayed(m, 500); // check after 500 ms
    }

    public void removeMessages() {
        removeMessages(WHAT);
    }

    public void setData(int list_position, boolean expected_state) {
        this.list_position = list_position;
        state = expected_state;
        retries = 0;
    }

    public void handleMessage(Message m) {
        if (retries > 3) {
            //give up
            Toast.makeText(ola.context, ola.context.getResources().getString(R.string.error_setting_outlet), Toast.LENGTH_LONG).show();
            return;
        }

        OutletInfo oi = (OutletInfo) ola.getItem(list_position);
        if (oi.State != state) {
            retries++;
            Log.w("DeviceControl", "Sending again, no response " + Integer.valueOf(list_position).toString());
            startDelayedCheck();
            UDPSendToDevice.sendOutlet(ola.context, oi.device, oi.OutletNumber, state);
        }
    }
}