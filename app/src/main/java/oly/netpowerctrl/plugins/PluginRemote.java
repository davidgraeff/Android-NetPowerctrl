package oly.netpowerctrl.plugins;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;

import oly.netpowerctrl.R;
import oly.netpowerctrl.listadapter.PluginValuesAdapter;
import oly.netpowerctrl.utils.ShowToast;

/**
 * Establish connection to plugin service, contains adapter with
 * plugin service values.
 */
public class PluginRemote implements PluginValuesAdapter.OnValueChanged {
    private static final String LOGNAME = "PluginRemote";
    private static final String INetPwrCtrlPlugin_NAME = "oly.netpowerctrl.plugins.INetPwrCtrlPlugin";
    private final Context context;
    public int pluginId;
    public String serviceName;
    public String localized_name;
    public PluginValuesAdapter valuesAdapter;

    private INetPwrCtrlPlugin service = null;
    private ServiceConnection svcConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            service = INetPwrCtrlPlugin.Stub.asInterface(binder);
            try {
                service.requestValues(callback);
            } catch (android.os.RemoteException e) {
                ShowToast.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name));
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            service = null;
        }
    };

    private PluginRemote(Context context, int pluginId, String serviceName, String localized_name) {
        this.context = context;
        this.pluginId = pluginId;
        this.localized_name = localized_name;
        this.serviceName = serviceName;
        valuesAdapter = new PluginValuesAdapter(context);
        valuesAdapter.setOnValueChanged(this);
    }

    static PluginRemote createPluginRemote(Context context, int pluginId, String serviceName, String localized_name) {
        PluginRemote r = new PluginRemote(context, pluginId, serviceName, localized_name);
        if (!context.bindService(new Intent(INetPwrCtrlPlugin_NAME), r.svcConn, Context.BIND_AUTO_CREATE)) {
            ShowToast.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name));
            return null;
        } else {
            ShowToast.FromOtherThread(context, context.getString(R.string.plugin_loaded, localized_name));
            return r;
        }
    }

    public void destroy() {
        context.unbindService(svcConn);
    }

    @Override
    public void onIntValueChanged(int id, int value) {
        try {
            service.updateIntValue(id, value, callback);
        } catch (android.os.RemoteException e) {
            ShowToast.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name));
        }
    }

    @Override
    public void onBooleanValueChanged(int id, boolean value) {
        try {
            service.updateBooleanValue(id, value, callback);
        } catch (android.os.RemoteException e) {
            ShowToast.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name));
        }
    }

    @Override
    public void onAction(int id) {
        try {
            service.executeAction(id, callback);
        } catch (android.os.RemoteException e) {
            ShowToast.FromOtherThread(context, context.getString(R.string.error_plugin_failed, localized_name));
        }
    }

    private final INetPwrCtrlPluginResult.Stub callback = new INetPwrCtrlPluginResult.Stub() {
        @Override
        public void intValue(final int id, final String name, final int min, final int max, final int value) throws RemoteException {
            valuesAdapter.addRemoteIntValue(id, name, min, max, value);
        }

        @Override
        public void booleanValue(final int id, final String name, final boolean value) throws RemoteException {
            valuesAdapter.addRemoteBooleanValue(id, name, value);
        }

        @Override
        public void action(final int id, final String name) throws RemoteException {
            valuesAdapter.addRemoteAction(id, name);
        }

        @Override
        public void header(final int id, final String name) throws RemoteException {
            valuesAdapter.addHeader(id, name);
        }

        @Override
        public void finished() throws RemoteException {
            Handler h = new Handler(context.getMainLooper());
            h.post(new Runnable() {
                public void run() {
                    valuesAdapter.addDataFinished();
                }
            });
        }
    };

}
