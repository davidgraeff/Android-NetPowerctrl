package oly.netpowerctrl.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.lang.ref.WeakReference;

import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.data.onServiceReady;
import oly.netpowerctrl.executables.Executable;

/**
 * Setup alarms on boot
 */
public class BroadcastReceiverRequestItems extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Log.w("Broadcast", "BroadcastReceiverRequestItems Received");
        if ("oly.netpowerctrl.getallactions".equals(intent.getAction())) {
            final Object keepServiceAliveObject = new Object();
            DataService.observersServiceReady.register(new onServiceReady() {
                @Override
                public boolean onServiceReady(DataService service) {
                    String exe_stateless = "", exe_toggles = "", exe_ranged = "";
                    for (Executable executable : service.executables.getItems().values()) {
                        switch (executable.getType()) {
                            case TypeRangedValue:
                                exe_ranged += executable.getTitle() + ";" + executable.getUid() + "|";
                                break;
                            case TypeStateless:
                                exe_stateless += executable.getTitle() + ";" + executable.getUid() + "|";
                                break;
                            case TypeToggle:
                                exe_toggles += executable.getTitle() + ";" + executable.getUid() + "|";
                                break;
                        }
                    }
                    DataService.stopUseService(keepServiceAliveObject);

                    Intent i = new Intent("oly.netpowerctrl.sendallactions");
                    i.putExtra("exe_stateless", exe_stateless);
                    i.putExtra("exe_toggles", exe_toggles);
                    i.putExtra("exe_ranged", exe_ranged);
                    context.sendBroadcast(i);
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
