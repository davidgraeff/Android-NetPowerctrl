package oly.netpowerctrl.plugins;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import oly.netpowerctrl.listadapter.DrawerAdapter;
import oly.netpowerctrl.main.NetpowerctrlActivity;
import oly.netpowerctrl.main.NetpowerctrlApplication;
import oly.netpowerctrl.preferences.SharedPrefs;

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

    private Context context;
    private DrawerAdapter mDrawerAdapter;
    private List<PluginRemote> plugins = new ArrayList<PluginRemote>();

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

    public PluginController(DrawerAdapter drawerAdapter) {
        this.context = NetpowerctrlApplication.instance;
        context.registerReceiver(onBroadcast, new IntentFilter(PLUGIN_RESPONSE_ACTION));
        mDrawerAdapter = drawerAdapter;

        recreate();
    }

    public void destroy() {
        // Unregister receiver
        try {
            context.unregisterReceiver(onBroadcast);
        } catch (IllegalArgumentException ignored) {
            // We ignore failures of type "Receiver not registered"
        }

        // Save plugin service names (we use this cache on reload)
        // Destroy plugins
        Set<String> pluginServiceNameList = new TreeSet<String>();
        for (PluginRemote r : plugins) {
            pluginServiceNameList.add(r.serviceName);
            r.destroy();
        }
        plugins.clear();
        SharedPrefs.savePlugins(pluginServiceNameList, context);
    }

    public PluginRemote getPlugin(int pluginId) {
        return plugins.get(pluginId);
    }

    private void initialPluginData(String serviceName,
                                   String localized_name) {

        for (PluginRemote existing_plugin : plugins) {
            if (existing_plugin.serviceName.equals(serviceName)) {
                existing_plugin.localized_name = localized_name;
                mDrawerAdapter.updatePluginItem(existing_plugin.localized_name, "", existing_plugin.pluginId);
                return;
            }
        }

        PluginRemote plugin = PluginRemote.createPluginRemote(context, plugins.size(), serviceName, localized_name);

        if (plugin == null) {
            return;
        }

        plugins.add(plugin);
        mDrawerAdapter.updatePluginItem(plugin.localized_name, "", plugin.pluginId);
    }

    public void recreate() {
        // Use cache to try to bind to already found plugins
        if (plugins.isEmpty()) {
            Set<String> pluginServiceNameList = SharedPrefs.readPlugins(context);
            if (pluginServiceNameList != null) {
                for (String serviceName : pluginServiceNameList) {
                    initialPluginData(serviceName, serviceName);
                }
            }
        } else {
            for (PluginRemote r : plugins) {
                mDrawerAdapter.updatePluginItem(r.localized_name, "", r.pluginId);
            }
        }

        // Discover plugins
        Intent i = new Intent(PLUGIN_QUERY_ACTION);
        i.putExtra(PAYLOAD_SERVICENAME, NetpowerctrlActivity.class.getCanonicalName());
        context.sendBroadcast(i);
    }

    public void remove(PluginRemote plugin) {
        plugins.remove(plugin);
        mDrawerAdapter.removePluginItem(plugin.pluginId);
    }
}
