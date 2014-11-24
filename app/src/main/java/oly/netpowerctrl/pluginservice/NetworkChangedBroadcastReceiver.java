package oly.netpowerctrl.pluginservice;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.utils.Logging;

/**
 * Created by david on 23.11.14.
 */
class NetworkChangedBroadcastReceiver extends BroadcastReceiver {
    boolean isNetworkChangedListener = false;

    public void registerReceiver(Service service) {
        if (!isNetworkChangedListener) {
            isNetworkChangedListener = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            service.registerReceiver(this, filter);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        PluginService pluginService = PluginService.getService();
        if (pluginService == null) {
            return;
        }

        @SuppressWarnings("ConstantConditions")
        ConnectivityManager cm = (ConnectivityManager) pluginService.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected()) {
            if (SharedPrefs.getInstance().logEnergySaveMode())
                Logging.appendLog(pluginService, "Energiesparen aus: Netzwechsel erkannt");
            pluginService.wakeupAllDevices();
        } else {
            if (SharedPrefs.getInstance().logEnergySaveMode())
                Logging.appendLog(pluginService, "Energiesparen an: Kein Netzwerk");

            pluginService.enterNetworkReducedMode();
        }
    }

    public void unregister(Service service) {
        // Stop listening for network changes
        if (isNetworkChangedListener) {
            isNetworkChangedListener = false;
            service.unregisterReceiver(this);
        }
    }
}
