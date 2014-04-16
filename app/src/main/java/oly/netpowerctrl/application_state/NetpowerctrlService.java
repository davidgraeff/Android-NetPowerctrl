package oly.netpowerctrl.application_state;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anel.AnelPlugin;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.main.NetpowerctrlActivity;
import oly.netpowerctrl.network.DeviceSend;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.ShowToast;

public class NetpowerctrlService extends Service {
    private static final String LOGNAME = "Plugins";
    private static final String PLUGIN_RESPONSE_ACTION = "oly.netpowerctrl.plugins.PLUGIN_RESPONSE_ACTION";
    private static final String PLUGIN_QUERY_ACTION = "oly.netpowerctrl.plugins.action.QUERY_CONDITION";
    private static final String PAYLOAD_SERVICENAME = "SERVICENAME";
    private static final String PAYLOAD_PACKAGENAME = "PACKAGENAME";
    private static final String PAYLOAD_LOCALIZED_NAME = "LOCALIZED_NAME";
    private static final String RESULT_CODE = "RESULT_CODE";
    private static final int INITIAL_VALUES = 1337;
    private boolean isBroadcastListener = false;
    private BroadcastReceiver pluginBroadcastListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctxt, Intent i) {
            if (i.getIntExtra(RESULT_CODE, -1) == INITIAL_VALUES)
                createRemotePlugin(i.getStringExtra(PAYLOAD_SERVICENAME),
                        i.getStringExtra(PAYLOAD_LOCALIZED_NAME), i.getStringExtra(PAYLOAD_PACKAGENAME));
            else
                Log.w(LOGNAME, i.getStringExtra(PAYLOAD_LOCALIZED_NAME) + "failed");
        }
    };
    private final IBinder mBinder = new LocalBinder();
    /**
     * If the listen and send thread are shutdown because the devices destination networks are
     * not in range, this variable is set to true.
     */
    public boolean isNetworkReducedMode;
    BroadcastReceiver networkChangedListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            //int type = (cm.getActiveNetworkInfo() == null) ? 0 : cm.getActiveNetworkInfo().getType();

            List<DeviceInfo> devices = NetpowerctrlApplication.getDataController().configuredDevices;
            for (DeviceInfo di : devices) {
                di.setUpdatedNever();
            }

            if (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected())
                start();
            else
                finish();
//            if (type == lastNetworkType)
//                return;
//            lastNetworkType = type;

            //Log.w("NETWORK",Integer.valueOf(lastNetworkType).toString());

        }
    };
    /**
     * Will be set if no one of the network DeviceInfos is reachable at the moment.
     */

    private boolean isNetworkChangedListener = false;
    private List<PluginInterface> plugins = new ArrayList<PluginInterface>();

    @Override
    public IBinder onBind(Intent intent) {
        start();
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (isNetworkChangedListener) {
            isNetworkChangedListener = false;
            unregisterReceiver(networkChangedListener);
        }
        finish();
        stopSelf();
        return super.onUnbind(intent);
    }

    public void finish() {
        // Clear all except the anel support
        PluginInterface first = plugins.get(0);
        plugins.clear();
        plugins.add(first);
        ((AnelPlugin) first).stopDiscoveryThreads();

        isNetworkReducedMode = true;
        // Stop send and listen threads
        boolean running = DeviceSend.instance().isRunning();
        if (running)
            DeviceSend.instance().interrupt();

        // Unregister plugin receiver
        if (isBroadcastListener) {
            isBroadcastListener = false;
            try {
                unregisterReceiver(pluginBroadcastListener);
            } catch (IllegalArgumentException ignored) {
            }
        }

        // Stop listening for network changes
        if (isNetworkChangedListener) {
            isNetworkChangedListener = false;
            unregisterReceiver(networkChangedListener);
        }

        if (SharedPrefs.notifyOnStop()) {
            ShowToast.FromOtherThread(this, getString(R.string.energy_saving_mode));
        }
    }

    public void start() {
        if (plugins.size() == 0)
            plugins.add(new AnelPlugin());

        if (!isNetworkChangedListener && SharedPrefs.getUse_energy_saving_mode()) {
            isNetworkChangedListener = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            registerReceiver(networkChangedListener, filter);
        }

        ((AnelPlugin) plugins.get(0)).startDiscoveryThreads(0);

        isNetworkReducedMode = false;
        boolean alreadyRunning = DeviceSend.instance().isRunning();
        if (!alreadyRunning) { // Start send and listen threads
            DeviceSend.instance().start();
        }
        NetpowerctrlApplication.instance.detectNewDevicesAndReachability(null);


    }

    private void createRemotePlugin(String serviceName,
                                    String localized_name, String packageName) {

        /**
         * We received a message from a plugin, we already know: ignore
         */
        for (PluginInterface pi : plugins) {
            if (pi instanceof PluginRemote && ((PluginRemote) pi).serviceName.equals(serviceName)) {
                return;
            }
        }

        PluginRemote plugin = PluginRemote.create(serviceName, localized_name, packageName);

        if (plugin == null) {
            return;
        }

        plugins.add(plugin);
    }

    private void discover() {
        if (!SharedPrefs.getLoadExtensions())
            return;

        if (!isBroadcastListener) {
            isBroadcastListener = true;
            NetpowerctrlApplication.instance.registerReceiver(pluginBroadcastListener,
                    new IntentFilter(PLUGIN_RESPONSE_ACTION));
        }

        Intent i = new Intent(PLUGIN_QUERY_ACTION);
        i.putExtra(PAYLOAD_SERVICENAME, NetpowerctrlActivity.class.getCanonicalName());
        NetpowerctrlApplication.instance.sendBroadcast(i);
    }

    public void remove(PluginRemote plugin) {
        plugins.remove(plugin);
    }

    public void sendBroadcastQuery() {
        for (PluginInterface pi : plugins) {
            pi.requestData();
        }

        if (!SharedPrefs.getLoadExtensions())
            return;

        discover();
    }

    public PluginInterface getPluginInterface(DeviceInfo deviceInfo) {
        for (PluginInterface pi : plugins) {
            if (pi.getPluginID().equals(deviceInfo.pluginID)) {
                return pi;
            }
        }
        return null;
    }

    public class LocalBinder extends Binder {
        public NetpowerctrlService getService() {
            // Return this instance of LocalService so clients can call public methods
            return NetpowerctrlService.this;
        }
    }
}
