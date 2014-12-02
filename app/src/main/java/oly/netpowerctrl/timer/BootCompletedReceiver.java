package oly.netpowerctrl.timer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.onDataLoaded;

/**
 * Setup alarms on boot
 */
public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        AppData.observersOnDataLoaded.register(new onDataLoaded() {
            @Override
            public boolean onDataLoaded() {
                if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                    TimerCollection.setupAndroidAlarm(context);
                }
                return false;
            }
        });
        AppData.useAppData();
    }
}
