package oly.netpowerctrl.anel;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.main.App;

/**
 * Created by david on 23.11.14.
 */
class NetworkChangedBroadcastReceiver extends BroadcastReceiver {
    boolean isNetworkChangedListener = false;
    AnelPlugin anelPlugin;

    public void registerReceiver(AnelPlugin anelPlugin) {
        if (!isNetworkChangedListener) {
            isNetworkChangedListener = true;
            this.anelPlugin = anelPlugin;
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            anelPlugin.getDataService().registerReceiver(this, filter);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        DataService dataService = DataService.getService();
        if (dataService == null) {
            context.unregisterReceiver(this);
            return;
        }

        App.getMainThreadHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                anelPlugin.checkDevicesReachabilityAfterNetworkChange();
            }
        }, 1000);
    }

    public void unregister(Service service) {
        // Stop listening for network changes
        if (isNetworkChangedListener) {
            isNetworkChangedListener = false;
            service.unregisterReceiver(this);
        }
    }
}
