package oly.netpowerctrl.main;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.listadapter.DeviceListAdapter;
import oly.netpowerctrl.preferences.DevicePreferencesFragment;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.GridOrListFragment;
import oly.netpowerctrl.utils.ListItemMenu;

/**
 */
public class ConfiguredDevicesListFragment extends GridOrListFragment implements PopupMenu.OnMenuItemClickListener, ListItemMenu {
    private DeviceListAdapter adapter;

    public ConfiguredDevicesListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.configured_device, menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        adapter = NetpowerctrlActivity._this.adapterUpdateManger.adpConfiguredDevices;
        adapter.setListItemMenu(this);

        mListView.setAdapter(adapter);
        setAutoCheckDataAvailable(true);
        return view;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_delete_all_devices: {
                //noinspection ConstantConditions
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.delete_all_devices)
                        .setMessage(R.string.confirmation_delete_all_devices)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Delete all devices
                                NetpowerctrlApplication.instance.deleteAllConfiguredDevices();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();

                return true;
            }

        }
        return false;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        final int position = (Integer) mListView.getTag();
        DeviceInfo current_device = adapter.getDevices().get(position);

        switch (menuItem.getItemId()) {
            case R.id.menu_configure_device: {
                SharedPrefs.SaveTempDevice(getActivity(), current_device);

                Fragment fragment = DevicePreferencesFragment.instantiate(getActivity());
                FragmentManager fragmentManager = getFragmentManager();
                assert fragmentManager != null;
                fragmentManager.beginTransaction().addToBackStack(null).replace(R.id.content_frame, fragment).commit();
                return true;
            }

            case R.id.menu_delete_device: {
                if (!current_device.isConfigured())
                    return true;

                //noinspection ConstantConditions
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.delete_device)
                        .setMessage(R.string.confirmation_delete_device)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                NetpowerctrlApplication.instance.deleteConfiguredDevice(position);
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();
                return true;
            }

            default:
                return false;
        }
    }

    @Override
    public void onMenuItemClicked(View v, int position) {
        mListView.setTag(position);
        @SuppressWarnings("ConstantConditions")
        PopupMenu popup = new PopupMenu(getActivity(), v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.configured_device_item, popup.getMenu());

        popup.setOnMenuItemClickListener(this);
        popup.show();
    }
}
