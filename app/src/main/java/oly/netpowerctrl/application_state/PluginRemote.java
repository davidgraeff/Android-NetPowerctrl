package oly.netpowerctrl.application_state;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.DevicePort;
import oly.netpowerctrl.network.DevicePortRenamed;
import oly.netpowerctrl.network.ExecutionFinished;
import oly.netpowerctrl.plugins.INetPwrCtrlPlugin;
import oly.netpowerctrl.plugins.INetPwrCtrlPluginResult;
import oly.netpowerctrl.utils.ShowToast;

/**
 * Establish connection to plugin service, contains adapter with
 * plugin service values.
 */
public class PluginRemote implements PluginInterface {
    private String TAG = "PluginRemote";
    private static final String INetPwrCtrlPlugin_NAME = "oly.netpowerctrl.plugins.INetPwrCtrlPlugin";
    private final Context context;
    public String serviceName;
    public String localized_name;
    //public PluginValuesAdapter valuesAdapter;
    private DeviceInfo di = null;
    private Map<Integer, DevicePort> actions_int = new TreeMap<Integer, DevicePort>();
    private Map<Integer, DevicePort> actions_boolean = new TreeMap<Integer, DevicePort>();
    private Map<Integer, DevicePort> actions_trigger = new TreeMap<Integer, DevicePort>();

    private Map<Integer, DevicePort> pre_actions_int = new TreeMap<Integer, DevicePort>();
    private Map<Integer, DevicePort> pre_actions_boolean = new TreeMap<Integer, DevicePort>();
    private Map<Integer, DevicePort> pre_actions_trigger = new TreeMap<Integer, DevicePort>();
    private boolean receiveFinished = false;

    private List<String> remote_states;
    private int remote_success_state;

    private INetPwrCtrlPlugin service = null;
    private ServiceConnection svcConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            serviceName = className.getClassName();
            service = INetPwrCtrlPlugin.Stub.asInterface(binder);
            requestData();
        }

        public void onServiceDisconnected(ComponentName className) {
            service = null;
            NetpowerctrlService c = NetpowerctrlApplication.getService();
            if (c != null)
                c.remove(PluginRemote.this);

            // Set non reachable and notify
            if (di != null) {
                di.setNotReachable("service disconnected!");
                NetpowerctrlApplication.getDataController().onDeviceUpdated(di);
            }
        }
    };

    /**
     * Initialize a plugin. Try to load an already configured DeviceInfo for this plugin
     * or create a new DeviceInfo object.
     *
     * @param context
     * @param serviceName
     * @param localized_name
     */
    private PluginRemote(Context context, String serviceName, String localized_name) {
        String UniqueDeviceID = "plugin" + serviceName;
        TAG += " " + serviceName;
        this.context = context;
        this.localized_name = localized_name;
        this.serviceName = serviceName;
        di = NetpowerctrlApplication.getDataController().findDeviceByMac(UniqueDeviceID);
        if (di == null)
            di = DeviceInfo.createNewDevice(getPluginID());
        else {
            // An already configured DeviceInfo has been found. Extract all devicePorts into the
            // local variables. The devicePorts are readded to the DeviceInfo when we receive
            // all available actions from the remote plugin.
            for (DevicePort port : di.DevicePorts) {
                switch (port.getType()) {
                    case TypeToggle:
                        pre_actions_boolean.put((int) port.id, port);
                        break;
                    case TypeButton:
                        pre_actions_trigger.put((int) port.id, port);
                        break;
                    case TypeRangedValue:
                        pre_actions_int.put((int) port.id, port);
                        break;
                }
            }
            di.DevicePorts.clear();
        }
        di.UniqueDeviceID = UniqueDeviceID;
        di.setNotReachable("init...");
        di.DeviceName = localized_name;
        di.HostName = serviceName;
    }

    public static PluginRemote create(String serviceName, String localized_name, String packageName) {
        Context context = NetpowerctrlApplication.instance;
        PluginRemote r = new PluginRemote(context, serviceName, localized_name);
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
    public void finish() {
        context.unbindService(svcConn);
    }

    /**
     * Request new data from plugin
     */
    @Override
    public void requestData() {
        try {
            if (service != null) {
                Log.w(TAG, "init");
                service.init(callback);
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
    public void requestData(DeviceInfo di) {

    }

    @Override
    public void rename(DevicePort port, String new_name, DevicePortRenamed callback) {
        try {
            if (service != null) {
                service.rename((int) port.id, new_name);
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

    private int transaction_counter = 0;

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
    public void prepareForDevices(DeviceInfo device) {

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
                    service.updateIntValue((int) port.id, command);
                    break;
                case TypeButton:
                    service.executeAction((int) port.id);
                    break;
                case TypeToggle:
                    service.updateBooleanValue((int) port.id, command == DevicePort.ON);
                    break;
            }
        } catch (NullPointerException e) {
            ShowToast.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name) + " " + e.getMessage());
        } catch (android.os.RemoteException e) {
            ShowToast.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name) + " " + e.getMessage());
        }

        if (callback != null)
            callback.onExecutionFinished(1);
    }

    private final INetPwrCtrlPluginResult.Stub callback = new INetPwrCtrlPluginResult.Stub() {
        private String current_header = "";
        private boolean isRequesting = false;

        @Override
        public void initDone(List<String> states, int success_state) throws RemoteException {
            remote_states = states;
            remote_success_state = success_state;
            di.HostName = serviceName;
        }

        @Override
        public void pluginState(int state) {
            if (state != remote_success_state || service == null) {
                di.setNotReachable(remote_states.get(state));
                Handler h = new Handler(context.getMainLooper());
                h.post(new Runnable() {
                    public void run() {
                        Log.w(TAG, "new state: " + di.not_reachable_reason);
                        NetpowerctrlApplication.getDataController().onDeviceUpdated(di);
                    }
                });
            }

            if (isRequesting)
                return;

            isRequesting = true;
            receiveFinished = false;

            Log.w(TAG, "plugin ready. request data.");

            try {
                service.requestValues();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void intValue(final int id, String name, final int min, final int max, final int value) throws RemoteException {
            DevicePort action;
            if (!actions_int.containsKey(id)) {
                action = pre_actions_int.get(id);
                if (action == null)
                    action = new DevicePort(di, DevicePort.DevicePortType.TypeRangedValue);
                action.id = id;
                di.DevicePorts.add(action);
                actions_int.put(id, action);
            } else {
                action = actions_int.get(id);
            }
            action.current_value = value;
            action.min_value = min;
            action.max_value = max;

            if (current_header.length() > 0)
                name = current_header + " " + name;
            action.setDescription(name);
            post();
        }

        @Override
        public void booleanValue(final int id, String name, final boolean value) throws RemoteException {
            DevicePort action;
            if (!actions_boolean.containsKey(id)) {
                action = pre_actions_boolean.get(id);
                if (action == null)
                    action = new DevicePort(di, DevicePort.DevicePortType.TypeToggle);
                action.id = id;
                di.DevicePorts.add(action);
                actions_boolean.put(id, action);
            } else
                action = actions_boolean.get(id);
            action.current_value = value ? DevicePort.ON : DevicePort.OFF;

            if (current_header.length() > 0)
                name = current_header + " " + name;
            action.setDescription(name);
            post();
        }

        @Override
        public void action(final int id, int groupID, String name) throws RemoteException {
            DevicePort action;
            if (!actions_trigger.containsKey(id)) {
                action = pre_actions_trigger.get(id);
                if (action == null)
                    action = new DevicePort(di, DevicePort.DevicePortType.TypeButton);
                action.id = id;
                di.DevicePorts.add(action);
                actions_trigger.put(id, action);
            } else
                action = actions_trigger.get(id);

            //Log.w("plugin:" + serviceName, "action " + name);

            if (current_header.length() > 0)
                name = current_header + " " + name;

            action.setDescription(name);
            post();
        }

        @Override
        public void header(final String name) throws RemoteException {
            current_header = name;
        }

        @Override
        public void finished() throws RemoteException {
            if (receiveFinished)
                return;
            isRequesting = false;
            receiveFinished = true;
            pre_actions_int.clear();
            pre_actions_boolean.clear();
            pre_actions_trigger.clear();
            post();
        }
    };

    private void post() {
        if (!receiveFinished)
            return;
        Log.w(TAG, "receiving done");
        di.setReachable();
        Handler h = new Handler(context.getMainLooper());
        h.post(new Runnable() {
            public void run() {
                Log.w(TAG, "receiving done main loop");
                NetpowerctrlApplication.getDataController().onDeviceUpdated(di);
            }
        });
    }
}