package oly.netpowerctrl.plugins;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.listadapter.DrawerAdapter;

/**
 * Created by david on 03.01.14.
 */
public class PluginController {
    private static final String LOGNAME = "Plugins";
    private static final String SCRIPT = "com.commonsware.SCRIPT";
    private static final String BROADCAST_ACTION = "com.commonsware.BROADCAST_ACTION";
    private static final String BROADCAST_PACKAGE = "com.commonsware.BROADCAST_PACKAGE";
    private static final String PRIVATE_ACTION = "com.commonsware.PRIVATE_BROADCAST_ACTION";
    private static final String PENDING_RESULT = "com.commonsware.PENDING_RESULT";
    private static final String PAYLOAD_SERVICENAME = "com.commonsware.PAYLOAD_SERVICENAME";
    private static final String PAYLOAD_LOCALIZED_NAME = "com.commonsware.PAYLOAD_LOCALIZED_NAME";
    private static final String PAYLOAD_VALUENAMES = "com.commonsware.PAYLOAD_VALUENAMES";
    private static final String PAYLOAD_INT_MIN_VALUES = "com.commonsware.PAYLOAD_INT_MIN_VALUES";
    private static final String PAYLOAD_INT_MAX_VALUES = "com.commonsware.PAYLOAD_INT_MAX_VALUES";
    private static final String PAYLOAD_INT_CURRENT_VALUES = "com.commonsware.PAYLOAD_INT_CURRENT_VALUES";
    private static final String PAYLOAD_BOOLVALUES = "com.commonsware.PAYLOAD_BOOLVALUES";
    private static final String RESULT_CODE = "com.commonsware.RESULT_CODE";
    private static final int INITIAL_VALUES = 1337;
    private static final int UPDATE_INT_VALUES = 1338;
    private static final int UPDATE_BOOL_VALUES = 1339;

    private Context context;
    private DrawerAdapter mDrawerAdapter;
    private List<PluginRemote> plugins = new ArrayList<PluginRemote>();

    private BroadcastReceiver onBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctxt, Intent i) {
            int resultCode = i.getIntExtra(RESULT_CODE, -1);
            if (resultCode == INITIAL_VALUES)
                initialPluginData(i.getStringExtra(PAYLOAD_SERVICENAME),
                        i.getStringExtra(PAYLOAD_LOCALIZED_NAME),
                        i.getStringArrayExtra(PAYLOAD_VALUENAMES),
                        i.getIntArrayExtra(PAYLOAD_INT_MIN_VALUES),
                        i.getIntArrayExtra(PAYLOAD_INT_MAX_VALUES),
                        i.getIntArrayExtra(PAYLOAD_INT_CURRENT_VALUES),
                        i.getBooleanArrayExtra(PAYLOAD_BOOLVALUES));
            else
                Log.w(LOGNAME, i.getStringExtra(PAYLOAD_LOCALIZED_NAME) + "failed");
        }
    };

    public PluginController(Context context, DrawerAdapter drawerAdapter) {
        this.context = context;
        context.registerReceiver(onBroadcast, new IntentFilter(PRIVATE_ACTION));
        mDrawerAdapter = drawerAdapter;
    }

    public void destroy() {
        context.unregisterReceiver(onBroadcast);
    }

    private void removePlugin() {

    }

    public PluginRemote getPlugin(int pluginId) {
        return plugins.get(pluginId);
    }

    private void initialPluginData(String serviceName,
                                   String localized_name,
                                   String[] value_names,
                                   int[] int_min_values,
                                   int[] int_max_values,
                                   int[] int_current_values,
                                   boolean[] bool_values) {
        Log.w(LOGNAME, "RESPONSE: " + serviceName);
        PluginRemote plugin = new PluginRemote(plugins.size(), serviceName, localized_name);
        if (plugins.isEmpty()) {
            mDrawerAdapter.addPluginHeader(context.getString(R.string.plugin_drawer_title));
        }
        plugins.add(plugin);
        mDrawerAdapter.addPluginItem(localized_name, "", plugin.pluginId);
    }
}
