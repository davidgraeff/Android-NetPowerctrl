package oly.netpowerctrl.network;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;

/**
 * Use this class on an AnelDeviceSwitch object right before you
 * change send it to a device for a state change. Call
 * startDelayedCheck() on this object and the sending will
 * be retried if the given AnelDeviceSwitch does not changed its
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
        if (retries >= 3) { //!job.di.reachable ||
            //give up
            if (job.di.getUpdatedTime() > 0) // if the device got updated at some time
                Toast.makeText(context,
                        context.getString(R.string.error_setting_outlet, job.di.DeviceName,
                                current_time - job.di.getUpdatedTime() / 1000),
                        Toast.LENGTH_LONG).show();
            // if the device never responded so far or the updated counter has been reseted by
            // e.g. the network change listener, we just do nothing here
            return;
        }

        if (!job.di.updatedAfter(current_time)) {
            retries++;
            job.setRepeater(this);
            DeviceSend.instance().addJob(job);
        }
    }
}