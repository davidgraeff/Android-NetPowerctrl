package oly.netpowerctrl.pluginservice;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;

import oly.netpowerctrl.R;
import oly.netpowerctrl.consistency_tests.device_tests;
;
import oly.netpowerctrl.data.JSONHelper;
import oly.netpowerctrl.devices.Credentials;
import oly.netpowerctrl.ioconnection.DeviceConnection;
import oly.netpowerctrl.ioconnection.DeviceConnectionFabric;
import oly.netpowerctrl.devices.Executable;
import oly.netpowerctrl.devices.EditDeviceInterface;
import oly.netpowerctrl.network.onExecutionFinished;
import oly.netpowerctrl.network.onHttpRequestResult;
import oly.netpowerctrl.plugins.INetPwrCtrlPlugin;
import oly.netpowerctrl.plugins.INetPwrCtrlPluginResult;
import oly.netpowerctrl.timer.Timer;
import oly.netpowerctrl.timer.TimerCollection;
import oly.netpowerctrl.ui.notifications.InAppNotifications;
import oly.netpowerctrl.utils.Logging;

/**
 * Establish connection to plugin plugin, contains adapter with
 * plugin plugin values.
 */
public class PluginRemote extends AbstractBasePlugin {
    private static final int api_version = 1;
    private final String localized_name;
    private final String packageName;
    public String serviceName;
    // TAG is a class member variable, because we want to append the remote plugin name for debug output.
    private String TAG = "PluginRemote";
    private boolean isInitialized = false;
    private INetPwrCtrlPlugin plugin = null;
    private final ServiceConnection svcConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            serviceName = className.getClassName();
            plugin = INetPwrCtrlPlugin.Stub.asInterface(binder);
            init();
            if (pluginReady != null)
                pluginReady.onPluginReady(PluginRemote.this, false);
        }

        public void onServiceDisconnected(ComponentName className) {
            plugin = null;
            pluginService.unbindService(this);
            if (pluginFinished.get() != null)
                pluginFinished.get().onPluginFinished(PluginRemote.this);
        }
    };
    private final INetPwrCtrlPluginResult.Stub callback = new INetPwrCtrlPluginResult.Stub() {
        @Override
        public void finished(boolean api_version_missmatch) {
            for (Credentials credentials : pluginService.findDevices(PluginRemote.this)) {
                credentials.lockDevice();
                if (api_version_missmatch)
                    credentials.setStatusMessageAllConnections(pluginService.getString(R.string.error_plugin_api_version_missmatch));
                else
                    credentials.setStatusMessageAllConnections(pluginService.getString(R.string.error_plugin_no_service_connection));
                credentials.releaseDevice();
            }
            if (plugin != null)
                pluginService.unbindService(svcConn);
        }

        @Override
        public void deviceConnectionChanged(String device_unique_id, int connection_id, String connection_json) {
            final Credentials credentials = pluginService.findDevice(device_unique_id);
            if (credentials == null) {
                Logging.getInstance().logExtensions(serviceName + " stateChanged: no device found!");
                Log.e(TAG, "devicePortsChanged, no device found for " + device_unique_id);
                return;
            }


            credentials.lockDevice();
            DeviceConnection deviceConnection = credentials.getConnectionByID(connection_id);
            if (deviceConnection == null) {
                credentials.releaseDevice();
                Logging.getInstance().logExtensions(serviceName + " stateChanged: no deviceConnection found!");
                Log.e(TAG, "stateChanged, no deviceConnection found for " + connection_json);
                return;
            }
            device_tests.test_connection_reachable_consistency(credentials);

            try {
                credentials.updateConnection(connection_id, DeviceConnectionFabric.fromJSON(JSONHelper.getReader(connection_json), credentials));
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            device_tests.test_connection_reachable_consistency(credentials);
            Log.w(TAG, "deviceConnectionChanged, State: " + credentials.getConnectionByID(0).reachableState().name());
            credentials.releaseDevice();

            pluginService.updateCredentialsFromOtherThread(credentials);
        }

        @Override
        public void devicePortChanged(String device_unique_id, String device_port_json) {
            Credentials credentials = pluginService.findDevice(device_unique_id);
            if (credentials == null) {
                Logging.getInstance().logExtensions(serviceName + " devicePortsChanged: no device found!");
                Log.e(TAG, "devicePortsChanged, no device found for " + device_unique_id);
                return;
            }
            Executable Executable = null;
            try {
                Executable = Executable.fromJSON(JSONHelper.getReader(device_port_json), credentials);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (Executable == null) {
                Logging.getInstance().logExtensions("Extension " + serviceName + " devicePortsChanged: no device found!");
                Log.e(TAG, "devicePortsChanged, no device found for " + device_port_json);
                return;
            }
            credentials.lockDevicePorts();
            credentials.updatePort(Executable);
            credentials.releaseDevicePorts();
            pluginService.updateCredentialsFromOtherThread(credentials);
        }

        @Override
        public void deviceChanged(String device_json) {
            Credentials credentials = new Credentials(true);
            try {
                // This will set the configured flag!
                credentials.load(JSONHelper.getReader(device_json));
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                return;
            }
            credentials.setPluginInterface(PluginRemote.this);
             PluginService pluginService = pluginService;
            // Correct configured flag
            credentials.setConfigured(PluginService.credentialsCollection.getPosition(credentials) != -1);
            PluginService.updateCredentialsFromOtherThread(credentials);
        }
    };
    private int transaction_counter_success = 0;
    private int transaction_counter_failures = 0;

    /**
     * Initialize a plugin. Try to load an already configured DeviceInfo for this plugin
     * or create a new DeviceInfo object.
     *
     * @param pluginService  Context
     * @param serviceName    Android Service Name
     * @param localized_name Translated plugin name
     * @param packageName    Package name
     */
    PluginRemote( PluginService pluginService, String serviceName, String localized_name, String packageName) {
        super(pluginService);
        TAG += " " + serviceName;
        this.localized_name = localized_name;
        this.serviceName = serviceName;
        this.packageName = packageName;
    }

    @Override
    protected void checkReady() {
        if (plugin != null && pluginReady != null)
            pluginReady.onPluginReady(this, false);
    }

    @Override
    public void onDestroy() {
        pluginService.unbindService(svcConn);
    }

    @Override
    public void onStart(Context context) {
        if (plugin != null) return;

        Intent in = new Intent();
        in.setClassName(packageName, serviceName);
        if (!context.bindService(in, svcConn, android.content.Context.BIND_AUTO_CREATE)) {
            InAppNotifications.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name) + " BIND");
            pluginReady.onPluginReady(this, true);
        }
    }

    /**
     * onCreate
     */
    private void init() {
        try {
            if (plugin != null) {
                plugin.init(callback, api_version);
                isInitialized = true;
            } else {
                Log.w(TAG, "init failed: plugin connection not established!");
            }
        } catch (RemoteException e) {
            InAppNotifications.FromOtherThread(pluginService, pluginService.getString(R.string.error_plugin_failed, localized_name) + " INIT " + e.getMessage());
        } catch (Exception e) {
            InAppNotifications.FromOtherThread(pluginService, pluginService.getString(R.string.error_plugin_failed, localized_name) + " INIT " + e.getMessage());
            Log.e(TAG, e.getMessage() == null ? "" : e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Request new data from plugin
     *
     * @param deviceQuery
     */
    @Override
    public void requestData(DeviceQuery deviceQuery) {
        if (!isInitialized) {
            Log.e(TAG, "requestData: not initialized!");
            return;
        }
        try {
            if (plugin != null) {
                //Log.w(TAG, "refresh: devices");
                plugin.requestData();
            } else
                InAppNotifications.FromOtherThread(pluginService, pluginService.getString(R.string.error_plugin_no_service_connection, localized_name));
        } catch (RemoteException e) {
            InAppNotifications.FromOtherThread(pluginService, pluginService.getString(R.string.error_plugin_failed, localized_name) + " REQUEST " + e.getMessage());
        } catch (Exception e) {
            InAppNotifications.FromOtherThread(pluginService, pluginService.getString(R.string.error_plugin_failed, localized_name) + " REQUEST " + e.getMessage());
            Log.e(TAG, e.getMessage() == null ? "" : e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void requestData(Credentials credentials, int device_connection_id) {
        if (!isInitialized) {
            Log.e(TAG, "requestData: not initialized!");
            return;
        }
        try {
            if (plugin != null) {
                //Log.w(TAG, "refresh: device connection");
                plugin.requestDataByConnection(credentials.getUniqueDeviceID(), device_connection_id);
            } else
                InAppNotifications.FromOtherThread(pluginService, pluginService.getString(R.string.error_plugin_no_service_connection, localized_name));
        } catch (RemoteException e) {
            InAppNotifications.FromOtherThread(pluginService, pluginService.getString(R.string.error_plugin_failed, localized_name) + " REQUEST " + e.getMessage());
        } catch (Exception e) {
            InAppNotifications.FromOtherThread(pluginService, pluginService.getString(R.string.error_plugin_failed, localized_name) + " REQUEST " + e.getMessage());
            Log.e(TAG, e.getMessage() == null ? "" : e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void rename(Executable port, String new_name, onHttpRequestResult callback) {
        try {
            if (plugin != null) {
                plugin.rename(port.executableIO.getUniqueDeviceID(), port.getUid(), new_name);
            } else
                InAppNotifications.FromOtherThread(pluginService, pluginService.getString(R.string.error_plugin_failed, localized_name));
        } catch (RemoteException e) {
            InAppNotifications.FromOtherThread(pluginService, pluginService.getString(R.string.error_plugin_failed, localized_name) + " " + e.getMessage());
        } catch (Exception e) {
            InAppNotifications.FromOtherThread(pluginService, pluginService.getString(R.string.error_plugin_failed, localized_name) + "(g) " + e.getMessage());
            Log.e(TAG, e.getMessage() == null ? "" : e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void addToTransaction(Executable port, int command) {
        if (execute(port, command, null))
            ++transaction_counter_success;
        else
            ++transaction_counter_failures;
    }

    @Override
    public void executeTransaction(onExecutionFinished callback) {
        if (callback != null)
            callback.onExecutionProgress(transaction_counter_success, transaction_counter_failures,
                    transaction_counter_failures + transaction_counter_success);
        transaction_counter_success = 0;
        transaction_counter_failures = 0;
    }

    @Override
    public String getPluginID() {
        return packageName;
    }

    @Override
    public String getLocalizedName() {
        return serviceName;
    }

    @Override
    public void openConfigurationPage(Credentials credentials, Context context) {
        Intent i;
        PackageManager manager = context.getPackageManager();
        try {
            assert manager != null;
            i = manager.getLaunchIntentForPackage(packageName);
            if (i == null)
                throw new PackageManager.NameNotFoundException();
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            context.startActivity(i);
        } catch (PackageManager.NameNotFoundException ignored) {

        }
    }

    @Override
    public Timer getNextFreeAlarm(Executable port, int type, int command) {
        return null;
    }

    @Override
    public void saveAlarm(Timer timer, final onHttpRequestResult callback) {

    }

    @Override
    public void removeAlarm(Timer timer, final onHttpRequestResult callback) {

    }

    @Override
    public boolean execute(Executable port, int command, onExecutionFinished callback) {
        if (plugin != null) {
            try {
                plugin.execute(port.executableIO.getUniqueDeviceID(), port.getUid(), command);
                if (callback != null) callback.onExecutionProgress(1, 0, 1);
                return true;
            } catch (RemoteException e) {
                InAppNotifications.FromOtherThread(pluginService, pluginService.getString(R.string.error_plugin_failed, localized_name) + " EXECUTE " + e.getClass().getName() + " " + e.getMessage());
                e.printStackTrace();
            }
        }
        if (callback != null) callback.onExecutionProgress(0, 1, 1);
        return false;
    }

    @Override
    public void requestAlarms(Executable port, TimerCollection timerCollection) {
        timerCollection.alarmsFromPluginOtherThread(null);
    }

    @Override
    public void showConfigureConnectionScreen(Activity activity, Credentials credentials) {
        pluginService.addToConfiguredDevices(credentials);
    }

    @Override
    public EditDeviceInterface openEditDevice(Credentials credentials) {
        return null;
    }

    @Override
    public void devicesChanged() {

    }

    @Override
    public boolean isStarted() {
        return plugin != null;
    }
}