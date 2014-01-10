package oly.netpowerctrl.main;

import android.app.ListFragment;
import android.os.Bundle;
import android.view.View;

import oly.netpowerctrl.plugins.PluginRemote;

public class PluginFragment extends ListFragment {
    private PluginRemote plugin;

    public PluginFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            int pluginId = getArguments().getInt("extra");
            plugin = NetpowerctrlApplication.instance.getPluginController().getPlugin(pluginId);
            setListAdapter(plugin.valuesAdapter);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setDivider(null);
    }
}
