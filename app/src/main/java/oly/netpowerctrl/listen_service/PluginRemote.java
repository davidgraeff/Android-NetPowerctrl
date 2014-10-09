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
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.JSONHelper;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.device_ports.DevicePort;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.devices.DeviceCollection;
import oly.netpowerctrl.devices.DeviceConnection;
import oly.netpowerctrl.devices.EditDeviceInterface;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.network.onExecutionFinished;
import oly.netpowerctrl.network.onHttpRequestResult;
import oly.netpowerctrl.plugins.INetPwrCtrlPlugin;
import oly.netpowerctrl.plugins.INetPwrCtrlPluginResult;
import oly.netpowerctrl.timer.Timer;
import oly.netpowerctrl.timer.TimerController;
import oly.netpowerctrl.utils.Logging;
import oly.netpowerctrl.utils.notifications.InAppNotifications;

/**
 * Establish connection to plugin service, contains adapter with
 * plugin service values.
 */
public class PluginRemote implements PluginInterface {
    private final Context context;
    private final String localized_name;
    private final String packageName;
    public String serviceName;
    private final INetPwrCtrlPluginResult.Stub callback = new INetPwrCtrlPluginResult.Stub() {
        @Override
        public void finished() {
            DeviceCollection deviceConnection = AppData.getInstance().deviceCollection;
            for (Device device : deviceConnection.getItems()) {
                if (device.getPluginInterface() == PluginRemote.this) {
                    device.setNotReachableAll(context.getString(R.string.error_plugin_no_service_connection));
                }
            }
        }

        @Override
        public void stateChanged(String state, boolean isError) throws RemoteException {
            if (SharedPrefs.getInstance().logExtensions()) {
                Logging.appendLog(context, "Extension " + serviceName + " state: " + state);
            }
        }

        @Override
        public void devicePortsChanged(List<String> devicePorts_json) throws RemoteException {

        }

        @Override
        public void deviceChanged(String device_json) throws RemoteException {
            final Device device = new Device();
            try {
                device.load(JSONHelper.getReader(device_json));
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                //TODO
                return;
            }
            device.setPluginInterface(PluginRemote.this);
            device.configured = false;
            device.setReachable(0);
            device.setUpdatedNow();
            App.getMainThreadHandler().post(new Runnable() {
                public void run() {
                    AppData.getInstance().onDeviceUpdated(device);
                }
            });
        }
    };
    private String TAG = "PluginRemote";
    private boolean isInitialized = false;
    private INetPwrCtrlPlugin service = null;
    private final ServiceConnection svcConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            serviceName = className.getClassName();
            service = INetPwrCtrlPlugin.Stub.asInterface(binder);
            init();
        }

        public void onServiceDisconnected(ComponentName className) {
            service = null;
            ListenService c = ListenService.getService();
            if (c != null)
                c.removeExtension(PluginRemote.this);

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
            InAppNotifications.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name));
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
    void init() {
        try {
            if (service != null) {
                Log.w(TAG, "init");
                service.init(callback);
                isInitialized = true;
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

    /**
     * Request new data from plugin
     */
    @Override
    public void requestData() {
        if (!isInitialized) {
            init();
            return;
        }
        try {
            if (service != null) {
                Log.w(TAG, "refresh");
                service.requestDevices(0);
            } else
                InAppNotifications.FromOtherThread(context, context.getString(R.string.error_plugin_no_service_connection, localized_name));
        } catch (RemoteException e) {
            InAppNotifications.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name) + " " + e.getMessage());
        } catch (Exception e) {
            InAppNotifications.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name) + "(g) " + e.getMessage());
            Log.e(TAG, e.getMessage() == null ? "" : e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void requestData(DeviceConnection ci) {
        requestData();
    }

    @Override
    public void rename(DevicePort port, String new_name, onHttpRequestResult callback) {
        try {
            if (service != null) {
                try {
                    JSONHelper h = new JSONHelper();
                    port.toJSON(h.createWriter(), true);
                    service.rename(h.getString(), new_name);
                } catch (IOException ignored) {
                }
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
        return serviceName;
    }

    @Override
    public void enterFullNetworkState(Context context, Device device) {

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
            JSONHelper h = new JSONHelper();
            port.toJSON(h.createWriter(), true);
            service.execute(h.getString(), command);
        } catch (NullPointerException | RemoteException | IOException e) {
            InAppNotifications.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name) + " " + e.getMessage());
            e.printStackTrace();
        }

        if (callback != null)
            callback.onExecutionFinished(1);
    }

    @Override
    public void requestAlarms(DevicePort port, TimerController timerController) {

    }

    @Override
    public void showConfigureDeviceScreen(Device device) {
        AppData.getInstance().addToConfiguredDevices(context, device);
    }

    @Override
    public EditDeviceInterface openEditDevice(Device device) {
        //TODO
        return null;
    }

}