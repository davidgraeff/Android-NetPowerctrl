package oly.netpowerctrl.anel;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import oly.netpowerctrl.main.App;
import oly.netpowerctrl.pluginservice.PluginService;

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
            anelPlugin.getPluginService().registerReceiver(this, filter);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        PluginService pluginService = PluginService.getService();
        if (pluginService == null) {
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
