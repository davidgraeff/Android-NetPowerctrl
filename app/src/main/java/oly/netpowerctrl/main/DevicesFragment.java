package oly.netpowerctrl.main;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.nhaarman.listviewanimations.swinginadapters.prepared.ScaleInAnimationAdapter;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anel.AnelPlugin;
import oly.netpowerctrl.anel.ConfigureDeviceFragment;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.DevicePort;
import oly.netpowerctrl.listadapter.DevicesAdapter;
import oly.netpowerctrl.network.DeviceQueryResult;
import oly.netpowerctrl.preferences.PreferencesFragment;
import oly.netpowerctrl.preferences.SharedPrefs;

/**
 */
public class DevicesFragment extends Fragment implements PopupMenu.OnMenuItemClickListener, AdapterView.OnItemClickListener {
    private DevicesAdapter adapter;

    public DevicesFragment() {
    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.devices, menu);
        //noinspection ConstantConditions
        menu.findItem(R.id.menu_delete_all_devices).setVisible(NetpowerctrlApplication.getDataController().configuredDevices.size() > 0);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (adapter != null)
            adapter.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null)
            adapter.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add_device: {
                // At the moment we always create an anel device
                DeviceInfo di = DeviceInfo.createNewDevice(AnelPlugin.PLUGIN_ID);
                show_configure_device_dialog(di);
                return true;
            }

            case R.id.menu_help: {
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.menu_help)
                        .setMessage(R.string.help_devices)
                        .setIcon(android.R.drawable.ic_dialog_alert).show();
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
                NetpowerctrlApplication.instance.findDevices(new DeviceQueryResult() {
                    @Override
                    public void onDeviceError(DeviceInfo di, String error_message) {
                    }

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
                                Toast.LENGTH_SHORT
                        ).show();
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
        adapter = new DevicesAdapter(NetpowerctrlActivity.instance, true);
        setHasOptionsMenu(true);
    }

    private ListView mListView;

    private void assignAdapter() {
        if (SharedPrefs.getAnimationEnabled()) {
            // Add animation to the list
            ScaleInAnimationAdapter animatedAdapter = new ScaleInAnimationAdapter(adapter);
            animatedAdapter.setAbsListView(mListView);
            mListView.setAdapter(animatedAdapter);
        } else {
            mListView.setAdapter(adapter);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_devices, container, false);
        mListView = (ListView) view.findViewById(android.R.id.list);
        mListView.setOnItemClickListener(this);
        assignAdapter();
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
        final DeviceInfo current_device = (DeviceInfo) adapter.getItem((Integer) mListView.getTag());

        switch (menuItem.getItemId()) {
            case R.id.menu_device_configure: {
                show_configure_device_dialog(current_device);
                return true;
            }

            case R.id.menu_device_createGroup: {
                //noinspection ConstantConditions
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.device_createGroup)
                        .setMessage(R.string.confirmation_device_createGroup)
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                assert current_device != null;
                                current_device.lockDevicePorts();
                                Iterator<DevicePort> it = current_device.getDevicePortIterator();
                                UUID uuidOfDevice = UUID.nameUUIDFromBytes(current_device.UniqueDeviceID.getBytes());
                                while (it.hasNext()) {
                                    it.next().addToGroup(uuidOfDevice);
                                }
                                current_device.releaseDevicePorts();
                                NetpowerctrlApplication.getDataController().saveConfiguredDevices(false);
                                NetpowerctrlApplication.getDataController().groups.edit(uuidOfDevice, current_device.DeviceName);
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();
                return true;
            }

            case R.id.menu_device_delete: {
                //noinspection ConstantConditions
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.delete_device)
                        .setMessage(R.string.confirmation_delete_device)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                NetpowerctrlApplication.getDataController().deleteConfiguredDevice(current_device);
                                NetpowerctrlApplication.instance.findDevices(null);
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();
                return true;
            }

            case R.id.menu_device_configuration_page:
                current_device.getPluginInterface(NetpowerctrlApplication.getService())
                        .openConfigurationPage(current_device, getActivity());
            default:
                return false;
        }
    }

    private void show_configure_device_dialog(DeviceInfo di) {
        if (di.pluginID.equals(AnelPlugin.PLUGIN_ID)) {
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
        } else { // for now: We just add the device to the configured devices
            NetpowerctrlApplication.getDataController().addToConfiguredDevices(di, true);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Object item = adapter.getItem(i);
        if (!(item instanceof DeviceInfo))
            return;

        mListView.setTag(i);
        DeviceInfo di = (DeviceInfo) item;
        if (di.configured) {
            @SuppressWarnings("ConstantConditions")
            PopupMenu popup = new PopupMenu(getActivity(), view);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.configured_device_item, popup.getMenu());

            popup.setOnMenuItemClickListener(this);
            popup.show();
        } else {
            // Set default values for anel devices
            show_configure_device_dialog(di);
        }
    }
}