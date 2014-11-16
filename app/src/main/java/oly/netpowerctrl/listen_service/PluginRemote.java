package oly.netpowerctrl.listen_service;

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
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.device_base.data.JSONHelper;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DeviceConnection;
import oly.netpowerctrl.device_base.device.DeviceConnectionFabric;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.devices.EditDeviceInterface;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.network.onExecutionFinished;
import oly.netpowerctrl.network.onHttpRequestResult;
import oly.netpowerctrl.plugins.INetPwrCtrlPlugin;
import oly.netpowerctrl.plugins.INetPwrCtrlPluginResult;
import oly.netpowerctrl.timer.Timer;
import oly.netpowerctrl.timer.TimerCollection;
import oly.netpowerctrl.ui.notifications.InAppNotifications;
import oly.netpowerctrl.utils.Logging;

/**
 * Establish connection to plugin service, contains adapter with
 * plugin service values.
 */
public class PluginRemote implements PluginInterface {
    private static final int api_version = 1;
    private final Context context;
    private final String localized_name;
    private final String packageName;
    public String serviceName;
    // TAG is a class member variable, because we want to append the remote service name for debug output.
    private String TAG = "PluginRemote";
    private final INetPwrCtrlPluginResult.Stub callback = new INetPwrCtrlPluginResult.Stub() {
        @Override
        public void finished(boolean api_version_missmatch) {
            for (Device device : AppData.getInstance().findDevices(PluginRemote.this)) {
                if (api_version_missmatch)
                    device.setStatusMessageAllConnections(context.getString(R.string.error_plugin_api_version_missmatch));
                else
                    device.setStatusMessageAllConnections(context.getString(R.string.error_plugin_no_service_connection));
            }
        }

        @Override
        public void deviceConnectionChanged(String device_unique_id, int connection_id, String connection_json) throws RemoteException {
            final Device device = AppData.getInstance().findDevice(device_unique_id);
            if (device == null) {
                if (SharedPrefs.getInstance().logExtensions()) {
                    Logging.appendLog(context, "Extension " + serviceName + " stateChanged: no device found!");
                }
                Log.w(TAG, "devicePortsChanged, no device found for " + device_unique_id);
                return;
            }

            DeviceConnection deviceConnection = device.getConnectionByID(connection_id);
            if (deviceConnection == null) {
                if (SharedPrefs.getInstance().logExtensions()) {
                    Logging.appendLog(context, "Extension " + serviceName + " stateChanged: no deviceConnection found!");
                }
                Log.w(TAG, "stateChanged, no deviceConnection found for " + connection_json);
                return;
            }
            try {
                device.updateConnection(connection_id, DeviceConnectionFabric.fromJSON(JSONHelper.getReader(connection_json), device));
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }

            device.connectionUsed(deviceConnection);
            AppData.getInstance().updateExistingDevice(device);
        }

        @Override
        public void devicePortChanged(String device_unique_id, String device_port_json) throws RemoteException {
            Device device = AppData.getInstance().findDevice(device_unique_id);
            if (device == null) {
                if (SharedPrefs.getInstance().logExtensions()) {
                    Logging.appendLog(context, "Extension " + serviceName + " devicePortsChanged: no device found!");
                }
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
                if (SharedPrefs.getInstance().logExtensions()) {
                    Logging.appendLog(context, "Extension " + serviceName + " devicePortsChanged: no device found!");
                }
                Log.w(TAG, "devicePortsChanged, no device found for " + device_port_json);
                return;
            }
            device.updatePort(devicePort);
            AppData.getInstance().updateExistingDevice(device);
        }

        @Override
        public void deviceChanged(String device_json) throws RemoteException {
            final Device device = new Device(true);
            try {
                device.load(JSONHelper.getReader(device_json));
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                return;
            }
            device.setPluginInterface(PluginRemote.this);
            App.getMainThreadHandler().post(new Runnable() {
                public void run() {
                    AppData.getInstance().updateDevice(device);
                }
            });
        }
    };
    private boolean isInitialized = false;
    private INetPwrCtrlPlugin service = null;
    private final ServiceConnection svcConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            serviceName = className.getClassName();
            service = INetPwrCtrlPlugin.Stub.asInterface(binder);
            init();
            enterFullNetworkState(ListenService.getService(), null);
        }

        public void onServiceDisconnected(ComponentName className) {
            service = null;
            ListenService c = ListenService.getService();
            if (c != null)
                c.removeExtension(PluginRemote.this);

            context.unbindService(this);
            // Set non reachable and notify
            if (SharedPrefs.getInstance().logExtensions()) {
                Logging.appendLog(context, "Extension " + serviceName + " disconnected");
            }
        }
    };
    private int transaction_counter = 0;

    /**
     * Initialize a plugin. Try to load an already configured DeviceInfo for this plugin
     * or create a new DeviceInfo object.
     *
     * @param context        Context
     * @param serviceName    Android Service Name
     * @param localized_name Translated plugin name
     * @param packageName    Package name
     */
    private PluginRemote(Context context, String serviceName, String localized_name, String packageName) {
        TAG += " " + serviceName;
        this.context = context;
        this.localized_name = localized_name;
        this.serviceName = serviceName;
        this.packageName = packageName;
    }

    public static PluginRemote create(String serviceName, String localized_name, String packageName) {
        Context context = ListenService.getService();
        PluginRemote r = new PluginRemote(context, serviceName, localized_name, packageName);
        Intent in = new Intent();
        in.setClassName(packageName, serviceName);
        if (!context.bindService(in, r.svcConn, Context.BIND_AUTO_CREATE)) {
            InAppNotifications.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name) + " BIND");
            return null;
        } else {
            //ShowToast.FromOtherThread(context, context.getString(R.string.plugin_loaded, localized_name));
            return r;
        }
    }

    @Override
    public void onDestroy() {
        context.unbindService(svcConn);
    }

    @Override
    public void onStart(ListenService service) {

    }

    /**
     * init
     */
    private void init() {
        try {
            if (service != null) {
                Log.w(TAG, "init");
                service.init(callback, api_version);
                isInitialized = true;
            } else {
                Log.w(TAG, "init failed: service connection not established!");
            }
        } catch (RemoteException e) {
            InAppNotifications.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name) + " INIT " + e.getMessage());
        } catch (Exception e) {
            InAppNotifications.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name) + " INIT " + e.getMessage());
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
            return;
        }
        try {
            if (service != null) {
                Log.w(TAG, "refresh");
                service.requestData();
            } else
                InAppNotifications.FromOtherThread(context, context.getString(R.string.error_plugin_no_service_connection, localized_name));
        } catch (RemoteException e) {
            InAppNotifications.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name) + " REQUEST " + e.getMessage());
        } catch (Exception e) {
            InAppNotifications.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name) + " REQUEST " + e.getMessage());
            Log.e(TAG, e.getMessage() == null ? "" : e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void requestData(Device device, int device_connection_id) {
        if (!isInitialized) {
            return;
        }
        try {
            if (service != null) {
                Log.w(TAG, "refresh");
                service.requestDataByConnection(device.getUniqueDeviceID(), device_connection_id);
            } else
                InAppNotifications.FromOtherThread(context, context.getString(R.string.error_plugin_no_service_connection, localized_name));
        } catch (RemoteException e) {
            InAppNotifications.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name) + " REQUEST " + e.getMessage());
        } catch (Exception e) {
            InAppNotifications.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name) + " REQUEST " + e.getMessage());
            Log.e(TAG, e.getMessage() == null ? "" : e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void rename(DevicePort port, String new_name, onHttpRequestResult callback) {
        try {
            if (service != null) {
                service.rename(port.device.getUniqueDeviceID(), port.getUid(), new_name);
            } else
                InAppNotifications.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name));
        } catch (RemoteException e) {
            InAppNotifications.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name) + " " + e.getMessage());
        } catch (Exception e) {
            InAppNotifications.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name) + "(g) " + e.getMessage());
            Log.e(TAG, e.getMessage() == null ? "" : e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void addToTransaction(DevicePort port, int command) {
        ++transaction_counter;
        execute(port, command, null);
    }

    @Override
    public void executeTransaction(onExecutionFinished callback) {
        if (callback != null)
            callback.onExecutionFinished(transaction_counter);

        transaction_counter = 0;
    }

    @Override
    public String getPluginID() {
        return packageName;
    }

    @Override
    public void enterFullNetworkState(Context context, Device device) {
        requestData();
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
    public Timer getNextFreeAlarm(DevicePort port, int type) {
        return null;
    }

    @Override
    public void saveAlarm(Timer timer, final onHttpRequestResult callback) {

    }

    @Override
    public void removeAlarm(Timer timer, final onHttpRequestResult callback) {

    }

    @Override
    public void execute(DevicePort port, int command, onExecutionFinished callback) {
        if (service == null) {
            if (callback != null)
                callback.onExecutionFinished(1);
            return;
        }

        try {
            service.execute(port.device.getUniqueDeviceID(), port.getUid(), command);
        } catch (RemoteException e) {
            InAppNotifications.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name) + " EXECUTE " + e.getClass().getName() + " " + e.getMessage());
            e.printStackTrace();
        }

        if (callback != null)
            callback.onExecutionFinished(1);
    }

    @Override
    public void requestAlarms(DevicePort port, TimerCollection timerCollection) {

    }

    @Override
    public void showConfigureDeviceScreen(Device device) {
        AppData.getInstance().addToConfiguredDevices(context, device);
    }

    @Override
    public EditDeviceInterface openEditDevice(Device device) {
        return null;
    }
}