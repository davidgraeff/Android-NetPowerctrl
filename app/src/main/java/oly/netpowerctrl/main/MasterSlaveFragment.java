package oly.netpowerctrl.main;

import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.DevicePort;
import oly.netpowerctrl.listadapter.DevicePortsSelectAdapter;

class MasterSlaveFragment extends ListFragment implements AdapterView.OnItemClickListener {
    private DevicePort master;
    private DevicePortsSelectAdapter adapter;

    public MasterSlaveFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle b = getArguments();
        if (b == null)
            return;
        String master_uuid = b.getString("master_uuid");
        if (master_uuid == null)
            return;

        master = NetpowerctrlApplication.getDataController().findDevicePort(UUID.fromString(master_uuid));
        if (master == null)
            return;

        // Add all device ports that are not equal to master and type of toggle.
        adapter = new DevicePortsSelectAdapter(getActivity());
        List<DeviceInfo> configuredDevices = NetpowerctrlApplication.getDataController().configuredDevices;
        for (DeviceInfo device : configuredDevices) {
            device.lockDevicePorts();
            Iterator<DevicePort> it_port = device.getDevicePortIterator();
            while (it_port.hasNext()) {
                DevicePort oi = it_port.next();
                if (!oi.equals(master) && oi.getType() == DevicePort.DevicePortType.TypeToggle)
                    adapter.addItem(oi, oi.current_value);
            }
            device.releaseDevicePorts();
        }
        adapter.setChecked(master.getSlaves());

        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy() {
        if (master == null)
            return;


        master.setSlaves(adapter.getCheckedUUids());
        NetpowerctrlApplication.getDataController().saveConfiguredDevices(false);

        //noinspection ConstantConditions
        getActivity().setTitle(R.string.drawer_overview);
        super.onDestroy();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(adapter);
        ListView l = getListView();
        assert l != null;
        l.setOnItemClickListener(this);
        if (master != null)
            //noinspection ConstantConditions
            getActivity().setTitle(getString(R.string.outlet_master_slave_title, master.getDescription()));
    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater) {
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        adapter.toggleItemChecked(i);
        Log.w("click", String.valueOf(i));
    }
}
