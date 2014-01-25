package oly.netpowerctrl.anelservice;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import oly.netpowerctrl.R;
import oly.netpowerctrl.main.NetpowerctrlApplication;

/**
 * Use this class on an OutletInfo object right before you
 * change send it to a device for a state change. Call
 * startDelayedCheck() on this object and the sending will
 * be retried if the given OutletInfo does not changed its
 * state to the requested one.
 */
public class SendJobRepeater extends Handler {
    private int retries = 0;
    private static final int WHAT = 111;
    private DeviceSend.SendJob job;
    private long current_time = System.currentTimeMillis();

    public SendJobRepeater(DeviceSend.SendJob job) {
        super(Looper.getMainLooper());
        this.job = job;
        if (!job.di.reachable)
            return;
        job.setRepeater(this);
    }

    public void startDelayedCheck() {
        Message m = this.obtainMessage();
        m.what = WHAT;
        sendMessageDelayed(m, 600 - (retries * 100)); // check after 600,500,400 ms
    }

    @Override
    public void handleMessage(Message m) {
        NetpowerctrlApplication context = NetpowerctrlApplication.instance;
        if (!job.di.reachable || retries >= 3) {
            //give up
            Toast.makeText(context, context.getString(R.string.error_setting_outlet, retries), Toast.LENGTH_LONG).show();
            return;
        }

        if (!job.di.updatedAfter(current_time)) {
            retries++;
            job.setRepeater(this);
            DeviceSend.instance().addJob(job);
        }
    }
}