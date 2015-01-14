package oly.netpowerctrl.timer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.lang.ref.WeakReference;

import oly.netpowerctrl.main.App;
import oly.netpowerctrl.pluginservice.PluginService;
import oly.netpowerctrl.pluginservice.onServiceReady;

/**
 * Setup alarms on boot
 */
public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, final Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            final Object keepServiceAliveObject = new Object();
            PluginService.observersServiceReady.register(new onServiceReady() {
                @Override
                public boolean onServiceReady(PluginService service) {
                    service.getAppData().timerCollection.setupNextAndroidAlarmFromPointInTime(App.instance, System.currentTimeMillis());
                    PluginService.stopUseService(keepServiceAliveObject);
                    return false;
                }

                @Override
                public void onServiceFinished(PluginService service) {

                }
            });
            PluginService.useService(new WeakReference<>(keepServiceAliveObject));
        }
    }
}
