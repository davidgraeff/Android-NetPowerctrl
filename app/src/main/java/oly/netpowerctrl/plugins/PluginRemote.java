package oly.netpowerctrl.plugins;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.Map;
import java.util.TreeMap;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.DevicePort;
import oly.netpowerctrl.utils.ShowToast;

/**
 * Establish connection to plugin service, contains adapter with
 * plugin service values.
 */
public class PluginRemote {
    private static final String TAG = "PluginRemote";
    private static final String INetPwrCtrlPlugin_NAME = "oly.netpowerctrl.plugins.INetPwrCtrlPlugin";
    private final Context context;
    public String serviceName;
    public String localized_name;
    //public PluginValuesAdapter valuesAdapter;
    public DeviceInfo di = null;
    private Map<Integer, DevicePort> actions_int = new TreeMap<Integer, DevicePort>();
    private Map<Integer, DevicePort> actions_boolean = new TreeMap<Integer, DevicePort>();
    private Map<Integer, DevicePort> actions_trigger = new TreeMap<Integer, DevicePort>();

    private Map<Integer, DevicePort> pre_actions_int = new TreeMap<Integer, DevicePort>();
    private Map<Integer, DevicePort> pre_actions_boolean = new TreeMap<Integer, DevicePort>();
    private Map<Integer, DevicePort> pre_actions_trigger = new TreeMap<Integer, DevicePort>();
    private boolean receiveFinished = false;

    private INetPwrCtrlPlugin service = null;
    private ServiceConnection svcConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            service = INetPwrCtrlPlugin.Stub.asInterface(binder);
            requestData();
        }

        public void onServiceDisconnected(ComponentName className) {
            service = null;
            PluginController c = NetpowerctrlApplication.instance.getPluginController();
            if (c == null)
                return;
            c.remove(PluginRemote.this);
        }
    };

    /**
     * Request new data from plugin
     */
    public void requestData() {

        try {
            if (service != null) {
                Log.w(TAG, "init: " + serviceName + " " + service.toString());
                service.init(callback);
            } else
                ShowToast.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name));
        } catch (RemoteException e) {
            ShowToast.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name) + " " + e.getMessage());
        } catch (Exception e) {
            ShowToast.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name) + "(g) " + e.getMessage());
            Log.w("PluginRemote,onServiceConnected", e.getMessage() == null ? "" : e.getMessage());
            e.printStackTrace();
        }
    }

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

        this.context = context;
        this.localized_name = localized_name;
        this.serviceName = serviceName;
        di = NetpowerctrlApplication.getDataController().findDeviceByMac(UniqueDeviceID);
        if (di == null)
            di = DeviceInfo.createNewDevice(DeviceInfo.DeviceType.PluginDevice);
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
        di.reachable = true;
        di.DeviceName = localized_name;
        di.HostName = serviceName;
    }

    static PluginRemote createPluginRemote(String serviceName, String localized_name) {
        Context context = NetpowerctrlApplication.instance;
        PluginRemote r = new PluginRemote(context, serviceName, localized_name);
        if (!context.bindService(new Intent(INetPwrCtrlPlugin_NAME), r.svcConn, Context.BIND_AUTO_CREATE)) {
            ShowToast.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name));
            return null;
        } else {
            //ShowToast.FromOtherThread(context, context.getString(R.string.plugin_loaded, localized_name));
            return r;
        }
    }

    public void finish() {
        context.unbindService(svcConn);
    }

    public void setValue(DevicePort port, int command) {
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
        } catch (android.os.RemoteException e) {
            ShowToast.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name) + " " + e.getMessage());
        }
    }

    private final INetPwrCtrlPluginResult.Stub callback = new INetPwrCtrlPluginResult.Stub() {
        private String current_header = "";

        @Override
        public void pluginState(int state) {
            Log.w("pluginState", "pluginState" + String.valueOf(state));
            di.reachable = state == 1;
            receiveFinished = false;
            if (state == 0 || service == null) {
                di.HostName = "Plugin not operable";
                Handler h = new Handler(context.getMainLooper());
                h.post(new Runnable() {
                    public void run() {
                        NetpowerctrlApplication.instance.getService().notifyObservers(di);
                    }
                });
            } else
                di.HostName = serviceName;

            if (state != 1)
                return;

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
            action.setDescriptionByDevice(name);
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
            action.setDescriptionByDevice(name);
            post();
        }

        @Override
        public void action(final int id, String name) throws RemoteException {
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

            if (current_header.length() > 0)
                name = current_header + " " + name;

            action.setDescriptionByDevice(name);
            post();
        }

        @Override
        public void header(final String name) throws RemoteException {
            current_header = name;
        }

        @Override
        public void finished() throws RemoteException {
            receiveFinished = true;
            pre_actions_int.clear();
            pre_actions_boolean.clear();
            pre_actions_trigger.clear();
            post();
        }
    };

    void post() {
        if (!receiveFinished)
            return;
        Log.w("post", "done");
        Handler h = new Handler(context.getMainLooper());
        h.post(new Runnable() {
            public void run() {
                NetpowerctrlApplication.instance.getService().notifyObservers(di);
            }
        });
    }
}