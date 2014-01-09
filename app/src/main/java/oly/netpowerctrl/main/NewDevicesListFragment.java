package oly.netpowerctrl.main;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.listadapter.DeviceListAdapter;
import oly.netpowerctrl.utils.GridOrListFragment;

/**
 */
public class NewDevicesListFragment extends GridOrListFragment implements AdapterView.OnItemClickListener {
    private DeviceListAdapter adapter;

    public NewDevicesListFragment() {
    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.unconfigured_device, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add_device: {
                show_configure_device_dialog(new DeviceInfo(getActivity()));
                return true;
            }

            case R.id.menu_requery: {
                NetpowerctrlApplication.instance.detectNewDevicesAndReachability(false);
                //noinspection ConstantConditions
                Toast.makeText(getActivity(), R.string.devices_refreshed, Toast.LENGTH_SHORT).show();
                return true;
            }

        }
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new DeviceListAdapter(NetpowerctrlActivity.instance, true);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        emptyText.setText(R.string.empty_no_new_devices);
        mListView.setOnItemClickListener(this);
        mListView.setAdapter(adapter);
        setAutoCheckDataAvailable(true);
        return view;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        DeviceInfo di = adapter.getItem(position);
        // Set default values for anel devices
        di.UserName = "admin";
        di.Password = "anel";
        show_configure_device_dialog(di);
    }

    private void show_configure_device_dialog(DeviceInfo di) {
        Fragment fragment = ConfigureDeviceFragment.instantiate(getActivity(), di);
        FragmentManager fragmentManager = getFragmentManager();
        assert fragmentManager != null;
        FragmentTransaction ft = fragmentManager.beginTransaction();
        Fragment prev = fragmentManager.findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        ((DialogFragment) fragment).show(ft, "dialog");
    }
}