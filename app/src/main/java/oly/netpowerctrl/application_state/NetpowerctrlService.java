package oly.netpowerctrl.application_state;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anel.AnelPlugin;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.devices.DeviceCollection;
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.network.DeviceObserverFinishedResult;
import oly.netpowerctrl.network.DeviceObserverResult;
import oly.netpowerctrl.network.DeviceQuery;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.Logging;

/**
 * Look for and load plugins. After network change: Rescan for reachable devices.
 */
public class NetpowerctrlService extends Service {
    public static final ServiceReadyObserver observersServiceReady = new ServiceReadyObserver();
    public static final RefreshStartedStoppedObserver observersStartStopRefresh = new RefreshStartedStoppedObserver();
    public static final ServiceModeChangedObserver observersServiceModeChanged = new ServiceModeChangedObserver();
    private static final String TAG = "NetpowerctrlService";
    private static final String PLUGIN_RESPONSE_ACTION = "oly.netpowerctrl.plugins.PLUGIN_RESPONSE_ACTION";
    private static final String PLUGIN_QUERY_ACTION = "oly.netpowerctrl.plugins.action.QUERY_CONDITION";
    private static final String PAYLOAD_SERVICENAME = "SERVICENAME";
    private static final String PAYLOAD_PACKAGENAME = "PACKAGENAME";
    private static final String PAYLOAD_LOCALIZED_NAME = "LOCALIZED_NAME";
    private static final String RESULT_CODE = "RESULT_CODE";
    private static final int INITIAL_VALUES = 1337;
    ///////////////// Service start/stop listener /////////////////
    static private int mDiscoverServiceRefCount = 0;
    static private NetpowerctrlService mDiscoverService;
    static private boolean mWaitForService;
    private final Runnable stopRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (mDiscoverService != null)
                    mDiscoverService.stopSelf();
                mWaitForService = false;
            } catch (IllegalArgumentException ignored) {
            }
        }
    };
    private final Handler stopServiceHandler = new Handler();
    private final BroadcastReceiver extensionsListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ignored, Intent i) {
            extensionDiscovered(i);
        }
    };
    private final BroadcastReceiver networkChangedListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            @SuppressWarnings("ConstantConditions")
            ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected()) {
                if (SharedPrefs.getInstance().logEnergySaveMode())
                    Logging.appendLog(NetpowerctrlService.this, "Energiesparen aus: Netzwechsel erkannt");
                enterFullNetworkMode(true, false);
            } else {
                if (SharedPrefs.getInstance().logEnergySaveMode())
                    Logging.appendLog(NetpowerctrlService.this, "Energiesparen an: Kein Netzwerk");

                enterNetworkReducedMode();
            }
        }
    };
    private final List<PluginInterface> plugins = new ArrayList<>();
    private boolean isExtensionsListener = false;
    /**
     * If the listen and send thread are shutdown because the devices destination networks are
     * not in range, this variable is set to true.
     */
    private boolean mNetworkReducedMode;
    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            if (SharedPrefs.getInstance().isPreferenceNameLogEnergySaveMode(s) && mNetworkReducedMode) {
                if (SharedPrefs.getInstance().logEnergySaveMode())
                    Logging.appendLog(NetpowerctrlService.this, "Energiesparen abgeschaltet");
                enterFullNetworkMode(true, false);
            }
        }
    };

    private boolean isNetworkChangedListener = false;

    /**
     * Detect new devices and check reach-ability of configured devices.
     */
    private boolean isDetecting = false;

    public static boolean isWirelessLanConnected(Context context) {
        @SuppressWarnings("ConstantConditions")
        WifiManager cm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return cm.isWifiEnabled() && cm.getConnectionInfo() != null;
    }

    static public boolean isServiceReady() {
        return (mDiscoverService != null);
    }

    /**
     * Call this in onResume if you need any of the service functionality.
     *
     * @param refreshDevices   Refresh devices immediately or after the service
     *                         is ready if it is not started yet.
     * @param showNotification
     */
    public static void useService(Context context, boolean refreshDevices, boolean showNotification) {
        ++mDiscoverServiceRefCount;
        // Stop delayed stop-service
        // Service is not running anymore, restart it
        if (mDiscoverService == null) {
            if (mWaitForService)
                return;
            mWaitForService = true;
            // start service
            Intent intent = new Intent(context, NetpowerctrlService.class);
            intent.putExtra("refreshDevices", refreshDevices);
            intent.putExtra("showNotification", showNotification);
            context.startService(intent);
        } else { // service already running. refresh devices?
            mDiscoverService.stopServiceHandler.removeCallbacks(mDiscoverService.stopRunnable);
            if (refreshDevices)
                mDiscoverService.findDevices(showNotification, null);
        }
    }

    public static void stopUseService() {
        if (mDiscoverServiceRefCount > 0) {
            mDiscoverServiceRefCount--;
        }
        if (mDiscoverServiceRefCount == 0 && mDiscoverService != null) {
            mDiscoverService.stopServiceHandler.postDelayed(mDiscoverService.stopRunnable, 2000);
        }
    }

    public static int getUsedCount() {
        return mDiscoverServiceRefCount;
    }

    public static NetpowerctrlService getService() {
        return mDiscoverService;
    }

    public static void debug_toggle_network_reduced() {
        if (mDiscoverService.isNetworkReducedMode())
            mDiscoverService.enterFullNetworkMode(true, true);
        else {
            RuntimeDataController c = RuntimeDataController.getDataController();
            for (Device d : c.deviceCollection.devices) {
                d.setNotReachableAll("Debug force off");
                c.onDeviceUpdated(d);
            }
            mDiscoverService.enterNetworkReducedMode();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mDiscoverService != null)
            return super.onStartCommand(intent, flags, startId);

        if (mDiscoverServiceRefCount == 0)
            mDiscoverServiceRefCount = 1;
        mWaitForService = false;
        mDiscoverService = this;

        // Add anel plugin
        {
            PluginInterface pluginInterface = new AnelPlugin();
            plugins.add(pluginInterface);
            updatePluginReferencesInDevices(pluginInterface);
        }

        // Listen to preferences changes
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        Bundle b = (intent != null) ? intent.getExtras() : null;

        // Service start code
        if (b != null)
            enterFullNetworkMode(b.getBoolean("refreshDevices"), b.getBoolean("showNotification"));
        else
            enterFullNetworkMode(true, false);

        observersServiceReady.onServiceReady(this);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if (isNetworkChangedListener) {
            isNetworkChangedListener = false;
            unregisterReceiver(networkChangedListener);
        }

        // Unregister extensions receiver
        if (isExtensionsListener) {
            isExtensionsListener = false;
            try {
                unregisterReceiver(extensionsListener);
            } catch (IllegalArgumentException ignored) {
            }
        }

        // Unregister from preferences changes
        try {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            sp.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        } catch (IllegalArgumentException ignored) {
        }

        // Logging
        if (SharedPrefs.getInstance().logEnergySaveMode())
            Logging.appendLog(this, "ENDE: Hintergrunddienste aus");

        enterNetworkReducedMode();

        observersServiceReady.onServiceFinished();

        // Clean up
        plugins.clear();
        removePluginReferencesInDevices(null);

        mDiscoverServiceRefCount = 0;
        mDiscoverService = null;
        mWaitForService = false;

    }

    ///////////////// Service start/stop /////////////////
    private void enterNetworkReducedMode() {
        mNetworkReducedMode = true;

        observersServiceModeChanged.onServiceModeChanged(true);

        for (PluginInterface pluginInterface : plugins)
            pluginInterface.enterNetworkReducedState(this);

        // Stop listening for network changes
        if (isNetworkChangedListener && !SharedPrefs.getInstance().isWakeUpFromEnergySaving()) {
            if (SharedPrefs.getInstance().logEnergySaveMode())
                Logging.appendLog(this, "Netzwerkwechsel nicht mehr überwacht. Manuelle Suche erforderlich.");
            isNetworkChangedListener = false;
            unregisterReceiver(networkChangedListener);
        }
    }

    public void enterFullNetworkMode(boolean refreshDevices, boolean showNotification) {
        mNetworkReducedMode = false;

        observersServiceModeChanged.onServiceModeChanged(false);

        if (SharedPrefs.getInstance().logEnergySaveMode())
            Logging.appendLog(this, "Hintergrunddienst gestartet");

        for (PluginInterface pluginInterface : plugins)
            pluginInterface.enterFullNetworkState(this, null);

        if (!isNetworkChangedListener) {
            if (SharedPrefs.getInstance().logEnergySaveMode())
                Logging.appendLog(this, "Netzwerkwechsel überwacht");
            isNetworkChangedListener = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            registerReceiver(networkChangedListener, filter);
        }

        // refresh devices after service start
        if (refreshDevices)
            findDevices(showNotification, null);
    }

    private void extensionDiscovered(Intent i) {
        final String serviceName = i.getStringExtra(PAYLOAD_SERVICENAME);
        final String localized_name = i.getStringExtra(PAYLOAD_LOCALIZED_NAME);
        final String packageName = i.getStringExtra(PAYLOAD_PACKAGENAME);

        if (i.getIntExtra(RESULT_CODE, -1) != INITIAL_VALUES) {
            if (SharedPrefs.getInstance().logEnergySaveMode())
                Logging.appendLog(this, localized_name + "failed");
            Log.e(TAG, localized_name + "failed");
            return;
        }

        if (serviceName == null || serviceName.isEmpty() || packageName == null || packageName.isEmpty())
            return;

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
        updatePluginReferencesInDevices(plugin);
    }

    private void updatePluginReferencesInDevices(PluginInterface plugin) {
        DeviceCollection deviceCollection = RuntimeDataController.getDataController().deviceCollection;
        for (Device device : deviceCollection.devices) {
            if (device.pluginID.equals(plugin.getPluginID())) {
                device.setPluginInterface(plugin);
                device.setHasChanged();
                deviceCollection.updateExisting(device);
            }
        }
    }

    public PluginInterface getPluginByID(String pluginID) {
        for (PluginInterface pluginInterface : plugins)
            if (pluginInterface.getPluginID().equals(pluginID))
                return pluginInterface;
        return null;
    }

    public void removeExtension(PluginRemote plugin) {
        plugins.remove(plugin);
        removePluginReferencesInDevices(plugin);
    }

    private void removePluginReferencesInDevices(PluginInterface plugin) {
        // Remove all references in Device objects.
        DeviceCollection deviceCollection = RuntimeDataController.getDataController().deviceCollection;
        for (Device device : deviceCollection.devices) {
            if (plugin == null || device.getPluginInterface() == plugin) {
                device.setPluginInterface(null);
                device.setHasChanged();
                deviceCollection.update(device);
            }
        }
    }

    public void sendBroadcastQuery() {
        for (PluginInterface pluginInterface : plugins) {
            pluginInterface.requestData();
        }

        if (SharedPrefs.getInstance().getLoadExtensions()) {
            if (!isExtensionsListener) {
                isExtensionsListener = true;
                registerReceiver(extensionsListener,
                        new IntentFilter(PLUGIN_RESPONSE_ACTION));
            }

            Intent i = new Intent(PLUGIN_QUERY_ACTION);
            i.putExtra(PAYLOAD_SERVICENAME, MainActivity.class.getCanonicalName());
            sendBroadcast(i);
        }
    }

    public void findDevices(final boolean showNotification, final DeviceObserverFinishedResult callback) {
        if (mNetworkReducedMode) {
            if (SharedPrefs.getInstance().logEnergySaveMode())
                Logging.appendLog(this, "Energiesparen aus: Suche Geräte");
            // Restart all listener services and try again
            Handler h = new Handler();
            h.post(new Runnable() {
                @Override
                public void run() {
                    enterFullNetworkMode(true, showNotification);
                }
            });
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

        observersStartStopRefresh.onRefreshStateChanged(true);

        // First try a broadcast
        RuntimeDataController.getDataController().clearNewDevices();
        new DeviceQuery(this, new DeviceObserverResult() {
            @Override
            public void onDeviceUpdated(Device di) {
            }

            @Override
            public void onObserverJobFinished(List<Device> timeout_devices) {
                RuntimeDataController.observersDataQueryCompleted.onDataQueryFinished();
                if (callback != null)
                    callback.onObserverJobFinished(timeout_devices);

                observersStartStopRefresh.onRefreshStateChanged(false);

                if (showNotification) {
                    // Show notification 500ms later, to also aggregate new devices for the message
                    NetpowerctrlApplication.getMainThreadHandler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //noinspection ConstantConditions
                            Toast.makeText(NetpowerctrlService.this,
                                    NetpowerctrlService.this.getString(R.string.devices_refreshed,
                                            RuntimeDataController.getDataController().getReachableConfiguredDevices(),
                                            RuntimeDataController.getDataController().newDevices.size()),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }, 500);
                }

                if (timeout_devices.size() == 0)
                    return;

                // Do we need to go into network reduced mode?
                if (timeout_devices.size() == RuntimeDataController.getDataController().countNetworkDevices()) {
                    if (SharedPrefs.getInstance().logEnergySaveMode())
                        Logging.appendLog(NetpowerctrlService.this, "Energiesparen an: Keine Geräte gefunden");
                    enterNetworkReducedMode();
                }
            }
        });
    }

    public boolean isNetworkReducedMode() {
        return mNetworkReducedMode;
    }
}
