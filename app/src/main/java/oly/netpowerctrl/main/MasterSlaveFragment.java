package oly.netpowerctrl.main;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.device_ports.DevicePort;
import oly.netpowerctrl.device_ports.DevicePortSource;
import oly.netpowerctrl.device_ports.DevicePortSourceConfigured;
import oly.netpowerctrl.device_ports.DevicePortsListAdapter;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.utils.actionbar.ActionBarDoneCancel;
import oly.netpowerctrl.utils.controls.ActivityWithIconCache;
import oly.netpowerctrl.utils.fragments.FragmentChangeArguments;

public class MasterSlaveFragment extends ListFragment implements AdapterView.OnItemClickListener, FragmentChangeArguments {
    private final ActionBarDoneCancel actionBarDoneCancel = new ActionBarDoneCancel();
    private DevicePort master;
    private DevicePortsListAdapter adapter;

    public MasterSlaveFragment() {
    }

    @Override
    public void onStart() {
        super.onStart();
        if (master != null)
            actionBarDoneCancel.setTitle(getActivity(),
                    getString(R.string.outlet_master_slave_title, master.getDescription()));
        else
            actionBarDoneCancel.setTitle(getActivity(), R.string.master_slave);
        actionBarDoneCancel.addCancelDone(getActivity(), R.layout.device_done);

        Activity a = getActivity();
        View btnDone = a.findViewById(R.id.action_mode_save_button);
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                save();
            }
        });
        View btnTest = a.findViewById(R.id.action_mode_test_button);
        btnTest.setVisibility(View.GONE);
        View btnCancel = a.findViewById(R.id.action_mode_close_button);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.getNavigationController().onBackPressed();
            }
        });
    }

    @Override
    public void changeArguments(Bundle b) {
        if (b == null)
            return;
        String master_uuid = b.getString("master_uuid");
        if (master_uuid == null)
            return;

        master = AppData.getInstance().findDevicePort(UUID.fromString(master_uuid));
    }

    private void save() {
        if (master == null)
            return;

        master.setSlaves(adapter.getCheckedUUids());
        AppData.getInstance().deviceCollection.save(master.device);

        MainActivity.getNavigationController().onBackPressed();
    }

    @Override
    public void onStop() {
        actionBarDoneCancel.restoreTitle(getActivity());
        actionBarDoneCancel.restoreActionBar(getActivity());
        super.onStop();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (master == null)
            return;

        // Add all device ports that are not equal to master and type of toggle.
        DevicePortSource s = new DevicePortSourceConfigured();
        adapter = new DevicePortsListAdapter(getActivity(), true, s, ((ActivityWithIconCache) getActivity()).getIconCache(), true);
        List<Device> configuredDevices = AppData.getInstance().deviceCollection.getItems();
        for (Device device : configuredDevices) {
            device.lockDevicePorts();
            Iterator<DevicePort> it_port = device.getDevicePortIterator();
            while (it_port.hasNext()) {
                DevicePort oi = it_port.next();
                if (!oi.equals(master) && oi.getType() == DevicePort.DevicePortType.TypeToggle)
                    adapter.addItem(oi, oi.current_value, false);
            }
            device.releaseDevicePorts();
        }
        adapter.computeGroupSpans();
        adapter.setChecked(master.getSlaves());
        adapter.notifyDataSetChanged();
        setListAdapter(adapter);
        ListView l = getListView();
        assert l != null;
        l.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        adapter.toggleItemChecked(i);
    }
}
