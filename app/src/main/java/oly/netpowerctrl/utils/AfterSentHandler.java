package oly.netpowerctrl.utils;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.OutletInfo;
import oly.netpowerctrl.listadapter.OutledSwitchListAdapter;
import oly.netpowerctrl.network.UDPSendToDevice;

public class AfterSentHandler extends Handler {
    int listposition;
    boolean state;    // the state we want the outlet to be in
    int retries;
    static final int WHAT = 111;
    final OutledSwitchListAdapter ola;

    public AfterSentHandler(OutledSwitchListAdapter l) {
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

    public void setData(int listposition, boolean expected_state) {
        this.listposition = listposition;
        state = expected_state;
        retries = 0;
    }

    public void handleMessage(Message m) {
        if (retries > 3) {
            //give up
            Toast.makeText(ola.context, ola.context.getResources().getString(R.string.error_setting_outlet), Toast.LENGTH_LONG).show();
            return;
        }

        OutletInfo oi = (OutletInfo) ola.getItem(listposition);
        if (oi.State != state) {
            retries++;
            Log.w("DeviceControl", "Sending again, no response " + Integer.valueOf(listposition).toString());
            startDelayedCheck();
            UDPSendToDevice.sendOutlet(ola.context, oi.device, oi.OutletNumber, state);
        }
    }
}