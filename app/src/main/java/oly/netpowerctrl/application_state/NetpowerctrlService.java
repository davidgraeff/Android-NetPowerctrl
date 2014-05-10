package oly.netpowerctrl.application_state;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anel.AnelPlugin;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.main.EnergySaveLogFragment;
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.network.DeviceObserverResult;
import oly.netpowerctrl.network.DeviceQuery;
import oly.netpowerctrl.network.UDPSending;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.ShowToast;

/**
 * Look for and load plugins. After network change: Rescan for reachable devices.
 */
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
    private final BroadcastReceiver pluginBroadcastListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctxt, Intent i) {
            if (NetpowerctrlApplication.getService() == null) {
                ShowToast.FromOtherThread(ctxt, "Power-Control error: pluginBroadcastListener still registered!");
                return;
            }
            if (i.getIntExtra(RESULT_CODE, -1) == INITIAL_VALUES)
                createRemotePlugin(i.getStringExtra(PAYLOAD_SERVICENAME),
                        i.getStringExtra(PAYLOAD_LOCALIZED_NAME), i.getStringExtra(PAYLOAD_PACKAGENAME));
            else
                Log.w(LOGNAME, i.getStringExtra(PAYLOAD_LOCALIZED_NAME) + "failed");
        }
    };
    private final IBinder mBinder = new LocalBinder();
    private UDPSending udpSending;

    /**
     * If the listen and send thread are shutdown because the devices destination networks are
     * not in range, this variable is set to true.
     */
    private boolean isNetworkReducedMode;
    private final BroadcastReceiver networkChangedListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            @SuppressWarnings("ConstantConditions")
            ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected()) {
                if (SharedPrefs.notifyOnStop()) {
                    ShowToast.FromOtherThread(NetpowerctrlService.this, getString(R.string.network_restarted));
                }
                if (SharedPrefs.logEnergySaveMode())
                    EnergySaveLogFragment.appendLog("Energiesparen aus: Netzwechsel erkannt");
                start();
            } else {
                if (SharedPrefs.logEnergySaveMode())
                    EnergySaveLogFragment.appendLog("Energiesparen an: Kein Netzwerk");
                finish();
                if (SharedPrefs.notifyOnStop()) {
                    ShowToast.FromOtherThread(NetpowerctrlService.this, getString(R.string.network_unreachable));
                }
            }
        }
    };
    /**
     * Will be set if no one of the network DeviceInfos is reachable at the moment.
     */

    private boolean isNetworkChangedListener = false;
    private final List<PluginInterface> plugins = new ArrayList<PluginInterface>();
    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            if (SharedPrefs.isPreferenceNameLogEnergySaveMode(s) && isNetworkReducedMode) {
                if (SharedPrefs.logEnergySaveMode())
                    EnergySaveLogFragment.appendLog("Energiesparen abgeschaltet");
                start();
                if (SharedPrefs.notifyOnStop()) {
                    ShowToast.FromOtherThread(NetpowerctrlService.this, getString(R.string.network_restarted));
                }
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (isNetworkChangedListener) {
            isNetworkChangedListener = false;
            unregisterReceiver(networkChangedListener);
        }
        if (SharedPrefs.logEnergySaveMode())
            EnergySaveLogFragment.appendLog("ENDE: Hintergrunddienste aus");
        finish();
        return super.onUnbind(intent);
    }

    private void finish() {
        // Clear all except the anel support
        PluginInterface first = plugins.get(0);
        plugins.clear();
        plugins.add(first);
        ((AnelPlugin) first).stopDiscoveryThreads(this);

        isNetworkReducedMode = true;
        // Stop send and listen threads
        boolean running = udpSending != null && udpSending.isRunning();
        if (running) {
            udpSending.interrupt();
            udpSending = null;
        }

        // Unregister plugin receiver
        if (isBroadcastListener) {
            isBroadcastListener = false;
            try {
                unregisterReceiver(pluginBroadcastListener);
            } catch (IllegalArgumentException ignored) {
            }
        }

        // Stop listening for network changes
        if (isNetworkChangedListener && !SharedPrefs.getWakeUp_energy_saving_mode()) {
            if (SharedPrefs.logEnergySaveMode())
                EnergySaveLogFragment.appendLog("Netzwerkwechsel nicht mehr überwacht. Manuelle Suche erforderlich.");
            isNetworkChangedListener = false;
            unregisterReceiver(networkChangedListener);
        }
    }

    public void start() {
        if (plugins.size() == 0) {
            plugins.add(new AnelPlugin());
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            sp.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        }

        if (!isNetworkChangedListener && SharedPrefs.getUse_energy_saving_mode()) {
            if (SharedPrefs.logEnergySaveMode())
                EnergySaveLogFragment.appendLog("Netzwerkwechsel überwacht");
            isNetworkChangedListener = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            registerReceiver(networkChangedListener, filter);
        }

        // Start anel listener
        ((AnelPlugin) plugins.get(0)).startDiscoveryThreads(0);

        isNetworkReducedMode = false;

        // Start send thread
        if (udpSending == null)
            udpSending = new UDPSending(false);
        boolean alreadyRunning = udpSending.isRunning();
        if (!alreadyRunning) {
            udpSending.start();
        }

        findDevices(null);
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
        i.putExtra(PAYLOAD_SERVICENAME, MainActivity.class.getCanonicalName());
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


    /**
     * Detect new devices and check reach-ability of configured devices.
     */
    private boolean isDetecting = false;

    public void findDevices(final DeviceObserverResult callback) {
        if (isNetworkReducedMode) {
            ShowToast.FromOtherThread(NetpowerctrlService.this, getString(R.string.network_restarted));
            if (SharedPrefs.logEnergySaveMode())
                EnergySaveLogFragment.appendLog("Energiesparen aus: Suche Geräte");
            start();
            return;
        }
        // The following mechanism allows only one update request within a
        // 1sec timeframe.
        if (isDetecting)
            return;
        isDetecting = true;
        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                isDetecting = false;
            }
        }, 1000);

        // First try a broadcast
        NetpowerctrlApplication.getDataController().clearNewDevices();
        new DeviceQuery(new DeviceObserverResult() {
            @Override
            public void onDeviceTimeout(DeviceInfo di) {
            }

            @Override
            public void onDeviceUpdated(DeviceInfo di) {
            }

            @Override
            public void onDeviceError(DeviceInfo di) {
            }

            @Override
            public void onObserverJobFinished(List<DeviceInfo> timeout_devices) {
                NetpowerctrlApplication.getDataController().notifyStateQueryFinished();
                if (callback != null)
                    callback.onObserverJobFinished(timeout_devices);

                if (timeout_devices.size() == 0)
                    return;

                // Do we need to go into network reduced mode?
                if (timeout_devices.size() == NetpowerctrlApplication.getDataController().countNetworkDevices()) {
                    if (SharedPrefs.logEnergySaveMode())
                        EnergySaveLogFragment.appendLog("Energiesparen an: Keine Geräte gefunden");
                    finish();
                    if (SharedPrefs.notifyOnStop()) {
                        ShowToast.FromOtherThread(NetpowerctrlService.this, getString(R.string.network_no_devices));
                    }
                }
            }
        });
    }

    public UDPSending getUDPSending() {
        return udpSending;
    }


    public class LocalBinder extends Binder {
        public NetpowerctrlService getService() {
            // Return this instance of LocalService so clients can call public methods
            return NetpowerctrlService.this;
        }
    }
}
