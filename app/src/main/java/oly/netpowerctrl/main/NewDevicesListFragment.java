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

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.listadapter.DeviceListAdapter;
import oly.netpowerctrl.utils.GridOrListFragment;
import oly.netpowerctrl.utils.ListItemMenu;

/**
 */
public class NewDevicesListFragment extends GridOrListFragment implements ListItemMenu {
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
                DeviceInfo di = new DeviceInfo(getActivity());
                Fragment fragment = ConfigureDeviceFragment.instantiate(getActivity(), di);
                FragmentManager fragmentManager = getFragmentManager();
                assert fragmentManager != null;
                fragmentManager.beginTransaction().addToBackStack(null).replace(R.id.content_frame, fragment).commit();
                return true;
            }

            case R.id.menu_requery: {
                NetpowerctrlApplication.instance.detectNewDevicesAndReachability();
                return true;
            }

        }
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = NetpowerctrlActivity.instance.adpNewDevices;
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        adapter.setListItemMenu(this);
        mListView.setAdapter(adapter);
        setAutoCheckDataAvailable(true);
        return view;
    }

    @Override
    public void onMenuItemClicked(View v, int position) {
        DeviceInfo di = adapter.getItem(position);
        // Set default values for anel devices
        di.UserName = "admin";
        di.Password = "anel";

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