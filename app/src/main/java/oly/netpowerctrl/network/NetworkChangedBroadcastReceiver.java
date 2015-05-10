package oly.netpowerctrl.network;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import oly.netpowerctrl.main.App;
import oly.netpowerctrl.main.GuiThreadHandler;

/**
 * Listen to network
 */
public class NetworkChangedBroadcastReceiver extends BroadcastReceiver {
    boolean isNetworkChangedListener = false;
    private int activeNetInfo;

    public void registerReceiver(Context context) {
        if (!isNetworkChangedListener) {
            isNetworkChangedListener = true;
            final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = connectivityManager.getActiveNetworkInfo();
            activeNetInfo = (info == null) ? 0 : info.toString().hashCode();

            IntentFilter filter = new IntentFilter();
            filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            context.registerReceiver(this, filter);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        int newNetInfo = (info == null) ? 0 : info.toString().hashCode();
        if (activeNetInfo == newNetInfo) return;
        activeNetInfo = newNetInfo;

        App.getMainThreadHandler().removeMessages(GuiThreadHandler.SERVICE_DELAYED_CHECK_REACHABILITY);
        App.getMainThreadHandler().sendMessageDelayed(
                App.getMainThreadHandler().obtainMessage(GuiThreadHandler.SERVICE_DELAYED_CHECK_REACHABILITY, 0), 2000);
    }

    public void unregister(Service service) {
        // Stop listening for network changes
        if (isNetworkChangedListener) {
            isNetworkChangedListener = false;
            service.unregisterReceiver(this);
        }
    }
}
