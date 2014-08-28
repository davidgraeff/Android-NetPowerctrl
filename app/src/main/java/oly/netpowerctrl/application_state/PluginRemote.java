package oly.netpowerctrl.application_state;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import oly.netpowerctrl.R;
import oly.netpowerctrl.device_ports.DevicePort;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.devices.DeviceConnection;
import oly.netpowerctrl.devices.DeviceConnectionHTTP;
import oly.netpowerctrl.network.AsyncRunnerResult;
import oly.netpowerctrl.network.ExecutionFinished;
import oly.netpowerctrl.plugins.INetPwrCtrlPlugin;
import oly.netpowerctrl.plugins.INetPwrCtrlPluginResult;
import oly.netpowerctrl.timer.Timer;
import oly.netpowerctrl.timer.TimerController;
import oly.netpowerctrl.utils_gui.ShowToast;

/**
 * Establish connection to plugin service, contains adapter with
 * plugin service values.
 */
public class PluginRemote implements PluginInterface {
    private final Context context;
    private final String localized_name;
    private final String packageName;
    public String serviceName;
    private String TAG = "PluginRemote";
    //public PluginValuesAdapter valuesAdapter;
    private Device di = null;
    private Set<Integer> devicePortIDs;
    private boolean receiveFinished = false;
    private List<String> remote_states;
    private int remote_success_state;
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
            NetpowerctrlService c = NetpowerctrlService.getService();
            if (c != null)
                c.removeExtension(PluginRemote.this);

            // Set non reachable and notify
            if (di != null) {
                di.setNotReachable(0, "service disconnected!");
                RuntimeDataController.getDataController().onDeviceUpdated(di);
            }
        }
    };
    private final INetPwrCtrlPluginResult.Stub callback = new INetPwrCtrlPluginResult.Stub() {
        private String current_header = "";

        @Override
        public void initDone(List<String> states, int success_state) throws RemoteException {
            remote_states = states;
            remote_success_state = success_state;
            di.setHasChanged();
            // Copy of current device port IDs
            devicePortIDs = new TreeSet<>(di.getDevicePortIDs());
        }

        @Override
        public void pluginState(int state) {
            di.setUpdatedNow();
            if (state != remote_success_state || service == null) {
                di.setNotReachable(0, remote_states.get(state));
            } else
                di.setReachable(0);
            di.setHasChanged();

            NetpowerctrlApplication.getMainThreadHandler().post(new Runnable() {
                public void run() {
                    RuntimeDataController.getDataController().onDeviceUpdated(di);
                }
            });
        }

        @Override
        public void intValue(final int id, String name, final int min, final int max, final int value) {
            devicePortIDs.remove(id);
            DevicePort action = di.getByID(id);
            if (action == null) {
                action = new DevicePort(di, DevicePort.DevicePortType.TypeRangedValue);
                action.id = id;
                di.addSafe(action);
            }
            action.current_value = value;
            action.min_value = min;
            action.max_value = max;

            action.setDescription(name);

            di.setHasChanged();

            post();
        }

        @Override
        public void booleanValue(final int id, String name, final boolean value) {
            devicePortIDs.remove(id);
            DevicePort action = di.getByID(id);
            if (action == null) {
                action = new DevicePort(di, DevicePort.DevicePortType.TypeToggle);
                action.id = id;
                di.addSafe(action);
            }
            action.current_value = value ? DevicePort.ON : DevicePort.OFF;

            action.setDescription(name);

            di.setHasChanged();

            post();
        }

        @Override
        public void action(final int id, int groupID, String name) {
            devicePortIDs.remove(id);
            DevicePort action = di.getByID(id);
            if (action == null) {
                action = new DevicePort(di, DevicePort.DevicePortType.TypeButton);
                action.id = id;
                di.addSafe(action);
            }

            action.setDescription(name);

            di.setHasChanged();

            post();
        }

        @Override
        public void finished() {
            if (receiveFinished)
                return;
            // Remove old DevicePorts
            if (devicePortIDs.size() > 0) {
                di.setHasChanged();
                for (int id : devicePortIDs) {
                    Log.w("removeOldPort", di.getByID(id).debugOut());
                    di.remove(id);
                }
                // Save ports
                RuntimeDataController.getDataController().deviceCollection.save();
            }

            receiveFinished = true;
            post();
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
        String UniqueDeviceID = "plugin" + serviceName;
        TAG += " " + serviceName;
        this.context = context;
        this.localized_name = localized_name;
        this.serviceName = serviceName;
        this.packageName = packageName;
        di = RuntimeDataController.getDataController().findDeviceByUniqueID(UniqueDeviceID);
        if (di == null)
            di = new Device(getPluginID());
        if (di.DeviceConnections.isEmpty())
            di.DeviceConnections.add(new DeviceConnectionHTTP(di, serviceName, 0));
        di.UniqueDeviceID = UniqueDeviceID;
        di.setNotReachable(0, "init...");
        di.DeviceName = localized_name;
    }

    public static PluginRemote create(String serviceName, String localized_name, String packageName) {
        Context context = NetpowerctrlService.getService();
        PluginRemote r = new PluginRemote(context, serviceName, localized_name, packageName);
        Intent in = new Intent();
        in.setClassName(packageName, serviceName);
        if (!context.bindService(in, r.svcConn, Context.BIND_AUTO_CREATE)) {
            ShowToast.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name));
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
    public void onStart(NetpowerctrlService service) {

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
                ShowToast.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name));
        } catch (RemoteException e) {
            ShowToast.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name) + " " + e.getMessage());
        } catch (Exception e) {
            ShowToast.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name) + "(g) " + e.getMessage());
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
                service.requestValues();
            } else
                ShowToast.FromOtherThread(context, context.getString(R.string.error_plugin_no_service_connection, localized_name));
        } catch (RemoteException e) {
            ShowToast.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name) + " " + e.getMessage());
        } catch (Exception e) {
            ShowToast.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name) + "(g) " + e.getMessage());
            Log.e(TAG, e.getMessage() == null ? "" : e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void requestData(DeviceConnection ci) {

    }

    @Override
    public void rename(DevicePort port, String new_name, AsyncRunnerResult callback) {
        try {
            if (service != null) {
                service.rename(port.id, new_name);
            } else
                ShowToast.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name));
        } catch (RemoteException e) {
            ShowToast.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name) + " " + e.getMessage());
        } catch (Exception e) {
            ShowToast.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name) + "(g) " + e.getMessage());
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
    public void executeTransaction(ExecutionFinished callback) {
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
    public void saveAlarm(Timer timer, final AsyncRunnerResult callback) {

    }

    @Override
    public void removeAlarm(Timer timer, final AsyncRunnerResult callback) {

    }

    @Override
    public void execute(DevicePort port, int command, ExecutionFinished callback) {
        if (service == null) {
            if (callback != null)
                callback.onExecutionFinished(1);
            return;
        }

        try {
            switch (port.getType()) {
                case TypeRangedValue:
                    if (command == DevicePort.TOGGLE)
                        command = port.current_value > 0 ? port.min_value : port.max_value;
                    service.updateIntValue(port.id, command);
                    break;
                case TypeButton:
                    service.executeAction(port.id);
                    break;
                case TypeToggle:
                    service.updateBooleanValue(port.id, command == DevicePort.ON);
                    break;
            }
        } catch (NullPointerException | RemoteException e) {
            ShowToast.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name) + " " + e.getMessage());
        }

        if (callback != null)
            callback.onExecutionFinished(1);
    }

    @Override
    public void requestAlarms(DevicePort port, TimerController timerController) {

    }

    @Override
    public void showConfigureDeviceScreen(Device device) {
        //TODO
    }

    private void post() {
        if (!receiveFinished)
            return;
        di.setReachable(0);
        di.setUpdatedNow();
        NetpowerctrlApplication.getMainThreadHandler().post(new Runnable() {
            public void run() {
                RuntimeDataController.getDataController().onDeviceUpdated(di);
            }
        });
    }
}