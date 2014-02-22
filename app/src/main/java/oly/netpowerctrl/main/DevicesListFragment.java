package oly.netpowerctrl.main;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anel.ConfigureDeviceFragment;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.listadapter.DeviceListAdapter;
import oly.netpowerctrl.network.DeviceQuery;
import oly.netpowerctrl.network.DeviceUpdateStateOrTimeout;
import oly.netpowerctrl.preferences.PreferencesFragment;

/**
 */
public class DevicesListFragment extends Fragment implements PopupMenu.OnMenuItemClickListener, ExpandableListView.OnChildClickListener {
    private DeviceListAdapter adapter;

    public DevicesListFragment() {
    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.devices, menu);
    }

    @Override
    public void onDestroy() {
        adapter.finish();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add_device: {
                // At the moment we always create an anel device
                DeviceInfo di = DeviceInfo.createNewDevice(DeviceInfo.DeviceType.AnelDevice);
                show_configure_device_dialog(di);
                return true;
            }

            case R.id.menu_delete_all_devices: {
                //noinspection ConstantConditions
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.delete_all_devices)
                        .setMessage(R.string.confirmation_delete_all_devices)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Delete all scenes
                                NetpowerctrlApplication.getDataController().deleteAllConfiguredDevices();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();

                return true;
            }

            case R.id.menu_requery: {
                new DeviceQuery(new DeviceUpdateStateOrTimeout() {
                    @Override
                    public void onDeviceTimeout(DeviceInfo di) {
                    }

                    @Override
                    public void onDeviceUpdated(DeviceInfo di) {
                    }

                    @Override
                    public void onDeviceQueryFinished(List<DeviceInfo> timeout_devices) {
                        //noinspection ConstantConditions
                        Toast.makeText(getActivity(),
                                getActivity().getString(R.string.devices_refreshed,
                                        NetpowerctrlApplication.getDataController().getReachableConfiguredDevices(),
                                        NetpowerctrlApplication.getDataController().newDevices.size()),
                                Toast.LENGTH_SHORT).show();
                    }
                });
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

    private ExpandableListView mListView;
    private int currentGroup = 0;
    private int currentGroupDevice = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_devices, container, false);
        mListView = (ExpandableListView) view.findViewById(android.R.id.list);
        mListView.setGroupIndicator(null);
        mListView.setOnChildClickListener(this);
        mListView.setAdapter(adapter);
        mListView.setEmptyView(view.findViewById(android.R.id.empty));
        Button btn = (Button) view.findViewById(R.id.btnChangeToPreferences);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NetpowerctrlActivity.instance.changeToFragment(PreferencesFragment.class.getName());
            }
        });

        return view;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        DeviceInfo current_device = (DeviceInfo) adapter.getChild(currentGroup, currentGroupDevice);

        switch (menuItem.getItemId()) {
            case R.id.menu_configure_device: {
                Fragment fragment = ConfigureDeviceFragment.instantiate(getActivity(), current_device);
                FragmentManager fragmentManager = getFragmentManager();
                assert fragmentManager != null;
                FragmentTransaction ft = fragmentManager.beginTransaction();
                Fragment prev = fragmentManager.findFragmentByTag("dialog");
                if (prev != null) {
                    ft.remove(prev);
                }
                ft.addToBackStack(null);
                ((DialogFragment) fragment).show(ft, "dialog");
                //fragmentManager.beginTransaction().addToBackStack(null).replace(R.id.content_frame, fragment).commit();
                return true;
            }

            case R.id.menu_delete_device: {
                //noinspection ConstantConditions
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.delete_device)
                        .setMessage(R.string.confirmation_delete_device)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                NetpowerctrlApplication.getDataController().deleteConfiguredDevice(currentGroupDevice);
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();
                return true;
            }

            case R.id.menu_device_website:
                Intent browse = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://" + current_device.HostName + ":" + Integer.valueOf(current_device.HttpPort).toString()));
                getActivity().startActivity(browse);

            default:
                return false;
        }
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

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        currentGroup = groupPosition;
        currentGroupDevice = childPosition;
        DeviceInfo di = (DeviceInfo) adapter.getChild(currentGroup, currentGroupDevice);
        if (di.configured) {
            @SuppressWarnings("ConstantConditions")
            PopupMenu popup = new PopupMenu(getActivity(), v);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.configured_device_item, popup.getMenu());

            popup.setOnMenuItemClickListener(this);
            popup.show();
        } else {
            // Set default values for anel devices
            di.UserName = "admin";
            di.Password = "anel";
            show_configure_device_dialog(di);
        }
        return true;
    }
}