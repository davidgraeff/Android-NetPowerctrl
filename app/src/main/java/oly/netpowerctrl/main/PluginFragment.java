package oly.netpowerctrl.main;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import oly.netpowerctrl.R;
import oly.netpowerctrl.plugins.PluginRemote;

public class PluginFragment extends Fragment {
    private PluginRemote plugin;

    public PluginFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            int pluginId = getArguments().getInt("extra");
            plugin = NetpowerctrlActivity.instance.getPluginController().getPlugin(pluginId);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_item, container);

        return view;
    }
}
