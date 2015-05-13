package oly.netpowerctrl.timer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.lang.ref.WeakReference;

import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.data.onServiceReady;
import oly.netpowerctrl.main.App;

/**
 * Setup alarms on boot
 */
public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, final Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            final Object keepServiceAliveObject = new Object();
            DataService.observersServiceReady.register(new onServiceReady() {
                @Override
                public boolean onServiceReady(DataService service) {
                    service.timers.setupNextAndroidAlarmFromPointInTime(App.instance, System.currentTimeMillis());
                    DataService.stopUseService(keepServiceAliveObject);
                    return false;
                }

                @Override
                public void onServiceFinished(DataService service) {

                }
            });
            DataService.useService(new WeakReference<>(keepServiceAliveObject));
        }
    }
}
