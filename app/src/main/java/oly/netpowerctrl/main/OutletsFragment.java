package oly.netpowerctrl.main;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.DevicePort;
import oly.netpowerctrl.datastructure.Groups;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.dynamicgid.DynamicGridView;
import oly.netpowerctrl.listadapter.DevicePortsExecuteAdapter;
import oly.netpowerctrl.listadapter.NotReachableUpdate;
import oly.netpowerctrl.network.DeviceQuery;
import oly.netpowerctrl.network.DeviceUpdateStateOrTimeout;
import oly.netpowerctrl.shortcut.EditShortcutActivity;
import oly.netpowerctrl.utils.ChangeArgumentsFragment;
import oly.netpowerctrl.utils.ListItemMenu;
import oly.netpowerctrl.utils.OnBackButton;

/**
 */
public class OutletsFragment extends Fragment implements PopupMenu.OnMenuItemClickListener,
        NotReachableUpdate, OnBackButton, ListItemMenu, ChangeArgumentsFragment {
    private DevicePortsExecuteAdapter adapter;
    private TextView hintText;
    private TextView emptyText;
    private Button btnEmpty;
    UUID groupFilter = null;

    public OutletsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public void changeArguments(Bundle mExtra) {

        if (mExtra != null && mExtra.containsKey("filter"))
            groupFilter = UUID.fromString(mExtra.getString("filter"));
        else
            groupFilter = null;

        if (isAdded()) {
            if (btnEmpty != null)
                btnEmpty.setVisibility(groupFilter == null ? View.VISIBLE : View.GONE);
            if (emptyText != null)
                emptyText.setText(groupFilter == null ? getString(R.string.empty_no_outlets) : getString(R.string.empty_group));
        }
        if (adapter != null) {
            adapter.setGroupFilter(groupFilter);
        }
    }

    @Override
    public void onDestroy() {
        if (adapter != null)
            adapter.finish();
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.outlets, menu);
        boolean hiddenShown = false;
        if (adapter != null) {
            hiddenShown = adapter.getIsShowingHidden();
        }
        //noinspection ConstantConditions
        menu.findItem(R.id.menu_showhidden).setVisible(!hiddenShown);
        //noinspection ConstantConditions
        menu.findItem(R.id.menu_hidehidden).setVisible(hiddenShown);
        //noinspection ConstantConditions
        menu.findItem(R.id.menu_removeGroup).setVisible(groupFilter != null);
        //noinspection ConstantConditions
        menu.findItem(R.id.menu_renameGroup).setVisible(groupFilter != null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_removeGroup: {
                if (!NetpowerctrlApplication.getDataController().groups.remove(groupFilter)) {
                    return true;
                }
                NetpowerctrlActivity.instance.changeToFragment(OutletsFragment.class.getName());
                return true;
            }
            case R.id.menu_renameGroup: {
                renameGroup();
                return true;
            }
            case R.id.menu_requery: {
                if (NetpowerctrlApplication.instance.getService().isNetworkReducedMode) {
                    //noinspection ConstantConditions
                    Toast.makeText(getActivity(),
                            getActivity().getString(R.string.energy_saving_mode),
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
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
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
                return true;
            }
            case R.id.menu_add_scene:
                Scene scene = new Scene();
                scene.sceneItems = adapter.getScene();
                final String sceneJSON = scene.toJSON();
                Intent it = new Intent(getActivity(), EditShortcutActivity.class);
                it.putExtra(EditShortcutActivity.EDIT_SCENE_NOT_SHORTCUT, true);
                it.putExtra(EditShortcutActivity.LOAD_SCENE, sceneJSON);
                startActivity(it);
                return true;
            case R.id.menu_showhidden: {
                adapter.setShowHidden(true);
                //noinspection ConstantConditions
                getActivity().invalidateOptionsMenu();
                return true;
            }
            case R.id.menu_hidehidden: {
                adapter.setShowHidden(false);
                //noinspection ConstantConditions
                getActivity().invalidateOptionsMenu();
                return true;
            }

            case R.id.menu_sortAlphabetically: {
                adapter.sortAlphabetically();
                NetpowerctrlApplication.getDataController().saveConfiguredDevices(false);
            }
        }
        return false;
    }

    private DynamicGridView mListView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Create adapter first
        if (adapter == null) {
            adapter = new DevicePortsExecuteAdapter(getActivity(), this, groupFilter);
        }

        final View view = inflater.inflate(R.layout.fragment_outlets, container, false);
        mListView = (DynamicGridView) view.findViewById(android.R.id.list);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                adapter.handleClick(position);
            }
        });
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(getActivity(), getActivity().getString(R.string.hint_stop_edit), Toast.LENGTH_SHORT).show();
                mListView.startEditMode();
                return false;
            }
        });
        mListView.setMinimumColumnWidth(280);
        mListView.setNumColumns(GridView.AUTO_FIT, container.getWidth());
        hintText = (TextView) view.findViewById(R.id.hintText);
        adapter.setNotReachableObserver(this);
        mListView.setEmptyView(view.findViewById(R.id.loading));
        // We assign the empty view after a short delay time,
        // to reduce visual flicker on app start, where data
        // is loaded with a high chance within the first 500ms.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isAdded())
                    return;
                mListView.getEmptyView().setVisibility(View.GONE);
                mListView.setEmptyView(view.findViewById(android.R.id.empty));
                emptyText = (TextView) view.findViewById(R.id.empty_text);
                emptyText.setText(groupFilter == null ? getString(R.string.empty_no_outlets) : getString(R.string.empty_group));

            }
        }, 1000);
        mListView.setAdapter(adapter);

        btnEmpty = (Button) view.findViewById(R.id.btnChangeToDevices);
        btnEmpty.setVisibility(groupFilter == null ? View.VISIBLE : View.GONE);
        btnEmpty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NetpowerctrlActivity.instance.changeToFragment(DevicesListFragment.class.getName());
            }
        });

        return view;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        final int position = (Integer) mListView.getTag();
        final DevicePort oi = adapter.getItem(position);

        switch (menuItem.getItemId()) {
            case R.id.menu_outlet_rename: {
                //noinspection ConstantConditions
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

                alert.setTitle(getString(R.string.outlet_rename_title));
                alert.setMessage(getString(R.string.outlet_rename_message, oi.getDeviceDescription()));

                final EditText input = new EditText(alert.getContext());
                input.setText(oi.getDescription());
                alert.setView(input);

                alert.setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //noinspection ConstantConditions
                        oi.setDescriptionByUser(input.getText().toString());
                        NetpowerctrlApplication.getDataController().saveConfiguredDevices(true);
                    }
                });

                alert.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });

                alert.show();
                return true;
            }
            case R.id.menu_outlet_removeFromGroup: {
                // Remove from group. Do nothing if item is not in that group
                if (!oi.groups.remove(groupFilter))
                    return true;

                // Save devices and devicePorts
                NetpowerctrlApplication.getDataController().saveConfiguredDevices(false);

                // update adapter
                adapter.setGroupFilter(groupFilter);
                return true;
            }
            case R.id.menu_outlet_createGroup: {
                final Groups groups = NetpowerctrlApplication.getDataController().groups;

                // No groups? Ask the user to create one
                if (groups.length() == 0) {
                    createGroupForDevicePort(oi);
                    return true;
                }

                CharSequence[] items = groups.getGroupsArray();
                final boolean[] checked = new boolean[items.length];

                // Sync checked array with items array
                for (int i = 0; i < checked.length; ++i) {
                    if (groups.equalsAtIndex(i, oi.groups))
                        checked[i] = true;
                }

                //noinspection ConstantConditions
                new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.outlet_to_group_title, oi.getDescription()))
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setMultiChoiceItems(items, checked, new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                                checked[i] = b;
                            }
                        })
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                oi.groups.clear();
                                int counter = 0;
                                for (int i = 0; i < checked.length; ++i) {
                                    if (!checked[i]) {
                                        continue;
                                    }
                                    oi.groups.add(groups.groupItems.get(i).uuid);
                                    ++counter;
                                }
                                NetpowerctrlApplication.getDataController().saveConfiguredDevices(false);
                                Toast.makeText(getActivity(), getString(R.string.outlet_added_to_groups, counter), Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNeutralButton(R.string.createGroup, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                createGroupForDevicePort(oi);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null).show();
                return true;
            }
            case R.id.menu_outlet_hide: {
                oi.Hidden = true;
                NetpowerctrlApplication.getDataController().saveConfiguredDevices(true);
                return true;
            }
            case R.id.menu_outlet_unhide: {
                oi.Hidden = false;
                NetpowerctrlApplication.getDataController().saveConfiguredDevices(true);
                return true;
            }
        }
        return false;
    }

    private void renameGroup() {
        final Groups.GroupItem groupItem = NetpowerctrlApplication.getDataController().groups.get(groupFilter);
        if (groupItem == null)
            return;

        //noinspection ConstantConditions
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        alert.setTitle(getString(R.string.menu_rename_group));

        final EditText input = new EditText(alert.getContext());
        input.setText(groupItem.name);
        alert.setView(input);
        alert.setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //noinspection ConstantConditions
                String name = input.getText().toString().trim();
                if (name.isEmpty())
                    return;
                NetpowerctrlApplication.getDataController().groups.edit(groupItem.uuid, name);
            }
        });

        alert.setNegativeButton(getString(android.R.string.cancel), null);
        alert.show();
    }

    private void createGroupForDevicePort(final DevicePort port) {
        //noinspection ConstantConditions
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        alert.setTitle(getString(R.string.outlet_to_group_title));
        alert.setMessage(getString(R.string.group_create));

        final EditText input = new EditText(alert.getContext());
        input.setText(port.getDescription());
        alert.setView(input);
        alert.setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //noinspection ConstantConditions
                String name = input.getText().toString().trim();
                if (name.isEmpty())
                    return;
                UUID group_uuid = NetpowerctrlApplication.getDataController().groups.add(name);
                port.addToGroup(group_uuid);
            }
        });

        alert.setNegativeButton(getString(android.R.string.cancel), null);
        alert.show();
    }

    @Override
    public void onNotReachableUpdate(List<DeviceInfo> not_reachable) {
        if (not_reachable.size() > 0) {
            String devices = "";
            for (DeviceInfo di : not_reachable) {
                devices += di.DeviceName + " ";
            }
            hintText.setText(NetpowerctrlApplication.instance.getString(R.string.error_not_reachable) + ": " + devices);
            hintText.setVisibility(View.VISIBLE);
        } else {
            hintText.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onBackButton() {
        if (mListView.isEditMode()) {
            mListView.stopEditMode();
            return true;
        }
        return false;
    }

    @Override
    public void onMenuItemClicked(View view, int position) {
        mListView.setTag(position);
        DevicePort oi = adapter.getItem(position);

        //noinspection ConstantConditions
        PopupMenu popup = new PopupMenu(getActivity(), view);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.outlets_item, popup.getMenu());

        Menu menu = popup.getMenu();

        //noinspection ConstantConditions
        menu.findItem(R.id.menu_outlet_hide).setVisible(!oi.Hidden);
        //noinspection ConstantConditions
        menu.findItem(R.id.menu_outlet_unhide).setVisible(oi.Hidden);
        //noinspection ConstantConditions
        menu.findItem(R.id.menu_outlet_createGroup).setVisible(groupFilter == null);
        //noinspection ConstantConditions
        menu.findItem(R.id.menu_outlet_removeFromGroup).setVisible(groupFilter != null);

        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

}
