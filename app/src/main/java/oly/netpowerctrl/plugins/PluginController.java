package oly.netpowerctrl.plugins;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.Map;
import java.util.TreeMap;

import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.DevicePort;
import oly.netpowerctrl.main.NetpowerctrlActivity;

/**
 * Created by david on 03.01.14.
 */
public class PluginController {
    private static final String LOGNAME = "Plugins";
    private static final String PLUGIN_RESPONSE_ACTION = "oly.netpowerctrl.plugins.PLUGIN_RESPONSE_ACTION";
    private static final String PLUGIN_QUERY_ACTION = "oly.netpowerctrl.plugins.action.QUERY_CONDITION";
    private static final String PAYLOAD_SERVICENAME = "SERVICENAME";
    private static final String PAYLOAD_LOCALIZED_NAME = "LOCALIZED_NAME";
    private static final String RESULT_CODE = "RESULT_CODE";
    private static final int INITIAL_VALUES = 1337;

    private Map<DeviceInfo, PluginRemote> plugin_by_deviceInfo = new TreeMap<DeviceInfo, PluginRemote>();

    private BroadcastReceiver onBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctxt, Intent i) {
            if (i.getIntExtra(RESULT_CODE, -1) == INITIAL_VALUES)
                initialPluginData(i.getStringExtra(PAYLOAD_SERVICENAME),
                        i.getStringExtra(PAYLOAD_LOCALIZED_NAME));
            else
                Log.w(LOGNAME, i.getStringExtra(PAYLOAD_LOCALIZED_NAME) + "failed");
        }
    };

    public PluginController() {
        NetpowerctrlApplication.instance.registerReceiver(onBroadcast,
                new IntentFilter(PLUGIN_RESPONSE_ACTION));
    }

    public void finish() {
        // Unregister receiver
        try {
            NetpowerctrlApplication.instance.unregisterReceiver(onBroadcast);
        } catch (IllegalArgumentException ignored) {
            // We ignore failures of type "Receiver not registered"
        }
        plugin_by_deviceInfo.clear();
    }

    private void initialPluginData(String serviceName,
                                   String localized_name) {

        /**
         * We received a message from a plugin, we already know: ignore
         */
        for (Map.Entry<DeviceInfo, PluginRemote> entry : plugin_by_deviceInfo.entrySet()) {
            if (entry.getValue().serviceName.equals(serviceName)) {
                return;
            }
        }

        PluginRemote plugin = PluginRemote.createPluginRemote(serviceName, localized_name);

        if (plugin == null) {
            return;
        }

        plugin_by_deviceInfo.put(plugin.di, plugin);
    }

    public void discover() {
        Intent i = new Intent(PLUGIN_QUERY_ACTION);
        i.putExtra(PAYLOAD_SERVICENAME, NetpowerctrlActivity.class.getCanonicalName());
        NetpowerctrlApplication.instance.sendBroadcast(i);
    }

    public void remove(PluginRemote plugin) {
        // Remove plugin from all PluginController lists.
        for (Map.Entry<DeviceInfo, PluginRemote> entry : plugin_by_deviceInfo.entrySet()) {
            entry.getValue().finish();
        }
        plugin_by_deviceInfo.remove(plugin.di);
        // Set non reachable and notify
        plugin.di.reachable = false;
        NetpowerctrlApplication.instance.getService().notifyObservers(plugin.di);
    }

    public void sendBroadcastQuery() {
        for (Map.Entry<DeviceInfo, PluginRemote> entry : plugin_by_deviceInfo.entrySet()) {
            NetpowerctrlApplication.instance.getService().notifyObservers(entry.getKey());
            PluginRemote remote = entry.getValue();
            if (remote != null)
                remote.requestData();
        }
        discover();
    }

    public void sendQuery(DeviceInfo di) {
        PluginRemote remote = plugin_by_deviceInfo.get(di);
        di.reachable = remote != null;
        if (remote != null)
            remote.requestData();
        NetpowerctrlApplication.instance.getService().notifyObservers(di);
    }

    public void execute(DevicePort port, int command) {
        PluginRemote remote = plugin_by_deviceInfo.get(port.device);
        if (remote == null)
            return;

        remote.setValue(port, command);
    }
}
