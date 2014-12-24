package oly.netpowerctrl.pluginservice;

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
import oly.netpowerctrl.device_base.data.JSONHelper;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DeviceConnection;
import oly.netpowerctrl.device_base.device.DeviceConnectionFabric;
import oly.netpowerctrl.device_base.device.DevicePort;
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
    private int transaction_counter_success = 0;
    private int transaction_counter_failures = 0;
    private onPluginReady pluginReady = null;
    private final ServiceConnection svcConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            serviceName = className.getClassName();
            plugin = INetPwrCtrlPlugin.Stub.asInterface(binder);
            init();
            if (pluginReady != null)
                pluginReady.onPluginReady(PluginRemote.this);
        }

        public void onServiceDisconnected(ComponentName className) {
            plugin = null;
            pluginService.unbindService(this);
            if (pluginReady != null)
                pluginReady.onPluginFinished(PluginRemote.this);
        }
    };
    private final INetPwrCtrlPluginResult.Stub callback = new INetPwrCtrlPluginResult.Stub() {
        @Override
        public void finished(boolean api_version_missmatch) {
            for (Device device : pluginService.getAppData().findDevices(PluginRemote.this)) {
                if (api_version_missmatch)
                    device.setStatusMessageAllConnections(pluginService.getString(R.string.error_plugin_api_version_missmatch));
                else
                    device.setStatusMessageAllConnections(pluginService.getString(R.string.error_plugin_no_service_connection));
            }
            if (plugin != null)
                pluginService.unbindService(svcConn);
        }

        @Override
        public void deviceConnectionChanged(String device_unique_id, int connection_id, String connection_json) throws RemoteException {
            final Device device = pluginService.getAppData().findDevice(device_unique_id);
            if (device == null) {
                Logging.getInstance().logExtensions(serviceName + " stateChanged: no device found!");
                Log.w(TAG, "devicePortsChanged, no device found for " + device_unique_id);
                return;
            }

            device.lockDevice();
            DeviceConnection deviceConnection = device.getConnectionByID(connection_id);
            if (deviceConnection == null) {
                device.releaseDevice();
                Logging.getInstance().logExtensions(serviceName + " stateChanged: no deviceConnection found!");
                Log.w(TAG, "stateChanged, no deviceConnection found for " + connection_json);
                return;
            }
            try {
                device.updateConnection(connection_id, DeviceConnectionFabric.fromJSON(JSONHelper.getReader(connection_json), device));
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            device.releaseDevice();

            device.connectionUsed(deviceConnection);
            pluginService.getAppData().updateExistingDeviceFromOtherThread(device);
        }

        @Override
        public void devicePortChanged(String device_unique_id, String device_port_json) throws RemoteException {
            Device device = pluginService.getAppData().findDevice(device_unique_id);
            if (device == null) {
                Logging.getInstance().logExtensions(serviceName + " devicePortsChanged: no device found!");
                Log.w(TAG, "devicePortsChanged, no device found for " + device_unique_id);
                return;
            }
            DevicePort devicePort = null;
            try {
                devicePort = DevicePort.fromJSON(JSONHelper.getReader(device_port_json), device);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (devicePort == null) {
                Logging.getInstance().logExtensions("Extension " + serviceName + " devicePortsChanged: no device found!");
                Log.w(TAG, "devicePortsChanged, no device found for " + device_port_json);
                return;
            }
            device.lockDevicePorts();
            device.updatePort(devicePort);
            device.releaseDevicePorts();
            pluginService.getAppData().updateExistingDeviceFromOtherThread(device);
        }

        @Override
        public void deviceChanged(String device_json) throws RemoteException {
            Device device = new Device(true);
            try {
                device.load(JSONHelper.getReader(device_json));
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                return;
            }
            device.setPluginInterface(PluginRemote.this);
            pluginService.getAppData().updateDeviceFromOtherThread(device);
        }
    };

    /**
     * Initialize a plugin. Try to load an already configured DeviceInfo for this plugin
     * or create a new DeviceInfo object.
     *
     * @param pluginService  Context
     * @param serviceName    Android Service Name
     * @param localized_name Translated plugin name
     * @param packageName    Package name
     */
    PluginRemote(PluginService pluginService, String serviceName, String localized_name, String packageName) {
        super(pluginService);
        TAG += " " + serviceName;
        this.localized_name = localized_name;
        this.serviceName = serviceName;
        this.packageName = packageName;
    }

    @Override
    public void onDestroy() {
        pluginService.unbindService(svcConn);
    }

    @Override
    public void onStart() {
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
     */
    @Override
    public void requestData() {
        if (!isInitialized) {
            Log.e(TAG, "requestData: not initialized!");
            return;
        }
        try {
            if (plugin != null) {
                Log.w(TAG, "refresh: devices");
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
    public void requestData(Device device, int device_connection_id) {
        if (!isInitialized) {
            Log.e(TAG, "requestData: not initialized!");
            return;
        }
        try {
            if (plugin != null) {
                Log.w(TAG, "refresh: device connection");
                plugin.requestDataByConnection(device.getUniqueDeviceID(), device_connection_id);
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
    public void rename(DevicePort port, String new_name, onHttpRequestResult callback) {
        try {
            if (plugin != null) {
                plugin.rename(port.device.getUniqueDeviceID(), port.getUid(), new_name);
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
    public void addToTransaction(DevicePort port, int command) {
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
    public void enterFullNetworkState(Context context, Device device) {
        if (plugin != null) return;

        Intent in = new Intent();
        in.setClassName(packageName, serviceName);
        if (!context.bindService(in, svcConn, android.content.Context.BIND_AUTO_CREATE)) {
            InAppNotifications.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name) + " BIND");
            pluginReady.onPluginFailedToInit(this);
        }
    }

    @Override
    public void enterNetworkReducedState(Context context) {

    }

    @Override
    public boolean isNetworkReducedState() {
        return false;
    }

    @Override
    public void openConfigurationPage(Device device, Context context) {
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
    public boolean isNetworkPlugin() {
        return false;
    }

    @Override
    public Timer getNextFreeAlarm(DevicePort port, int type, int command) {
        return null;
    }

    @Override
    public void saveAlarm(Timer timer, final onHttpRequestResult callback) {

    }

    @Override
    public void removeAlarm(Timer timer, final onHttpRequestResult callback) {

    }

    @Override
    public boolean execute(DevicePort port, int command, onExecutionFinished callback) {
        if (plugin != null) {
            try {
                plugin.execute(port.device.getUniqueDeviceID(), port.getUid(), command);
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
    public void requestAlarms(DevicePort port, TimerCollection timerCollection) {
        timerCollection.alarmsFromPluginOtherThread(null);
    }

    @Override
    public void showConfigureDeviceScreen(Device device) {
        pluginService.getAppData().addToConfiguredDevices(device);
    }

    @Override
    public EditDeviceInterface openEditDevice(Device device) {
        return null;
    }

    public void registerReadyObserver(onPluginReady pluginReady) {
        this.pluginReady = pluginReady;
        if (plugin != null && pluginReady != null)
            pluginReady.onPluginReady(PluginRemote.this);
    }
}