package oly.netpowerctrl.main;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
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

import com.nhaarman.listviewanimations.swinginadapters.prepared.AlphaInAnimationAdapter;

import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.backup.drive.GDriveFragment;
import oly.netpowerctrl.devices.DeviceInfo;
import oly.netpowerctrl.devices.DevicePort;
import oly.netpowerctrl.devices.DevicePortsExecuteAdapter;
import oly.netpowerctrl.devices.DevicesFragment;
import oly.netpowerctrl.devices.NotReachableUpdate;
import oly.netpowerctrl.groups.GroupUtilities;
import oly.netpowerctrl.network.AsyncRunnerResult;
import oly.netpowerctrl.network.DeviceObserverResult;
import oly.netpowerctrl.network.DeviceQuery;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.scenes.EditSceneActivity;
import oly.netpowerctrl.scenes.Scene;
import oly.netpowerctrl.utils.Icons;
import oly.netpowerctrl.utils.ListItemMenu;
import oly.netpowerctrl.utils.Shortcuts;
import oly.netpowerctrl.utils.ShowToast;
import oly.netpowerctrl.utils.gui.ChangeArgumentsFragment;

/**
 */
public class OutletsFragment extends Fragment implements PopupMenu.OnMenuItemClickListener,
        NotReachableUpdate, ListItemMenu, ChangeArgumentsFragment, AsyncRunnerResult, Icons.IconSelected {
    private DevicePortsExecuteAdapter adapter;
    private AlphaInAnimationAdapter animatedAdapter;
    private TextView hintText;
    private TextView emptyText;
    private Button btnChangeToDevices;
    private Button btnChangeToBackup;
    private UUID groupFilter = null;

    public OutletsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
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
    public void onStart() {
        super.onStart();
        loadAdapterData();
    }

    @Override
    public void changeArguments(Bundle mExtra) {

        if (mExtra != null && mExtra.containsKey("filter"))
            groupFilter = UUID.fromString(mExtra.getString("filter"));
        else
            groupFilter = null;

        if (isAdded()) {
            if (btnChangeToDevices != null)
                btnChangeToDevices.setVisibility(groupFilter == null ? View.VISIBLE : View.GONE);
            if (btnChangeToBackup != null)
                btnChangeToBackup.setVisibility(groupFilter == null ? View.VISIBLE : View.GONE);
            if (emptyText != null)
                emptyText.setText(groupFilter == null ? getString(R.string.empty_no_outlets) : getString(R.string.empty_group));
        }
    }

    private boolean isLoading = false;

    private void loadAdapterData() {
        if (isLoading)
            return;
        isLoading = true;
        NetpowerctrlApplication.getMainThreadHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                adapter.setGroupFilter(groupFilter);
                isLoading = false;
            }
        }, 100);
    }

    private void setListOrGrid(boolean grid, int widthOfListView) {
        SharedPrefs.setOutletsGrid(grid);

        float width;
        if (!grid) {
            adapter.setLayoutRes(R.layout.list_icon_item);
            width = getResources().getDimension(R.dimen.min_list_item_width);
        } else {
            adapter.setLayoutRes(R.layout.grid_icon_item);
            width = getResources().getDimension(R.dimen.min_grid_item_width);
        }

        mListView.setColumnWidth((int) width);
        mListView.setNumColumns(GridView.AUTO_FIT);

        mListView.setAdapter(animatedAdapter != null ? animatedAdapter : adapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.outlets, menu);

        menu.findItem(R.id.menu_main_donate).setVisible(MainActivity.instance.donate.donatePossible());

        if (!NetpowerctrlApplication.getDataController().deviceCollection.hasDevices()) {
            //noinspection ConstantConditions
            menu.findItem(R.id.menu_showhidden).setVisible(false);
            //noinspection ConstantConditions
            menu.findItem(R.id.menu_hidehidden).setVisible(false);
            //noinspection ConstantConditions
            menu.findItem(R.id.menu_removeGroup).setVisible(false);
            //noinspection ConstantConditions
            menu.findItem(R.id.menu_renameGroup).setVisible(false);
            //noinspection ConstantConditions
            menu.findItem(R.id.menu_view_list).setVisible(false);
            //noinspection ConstantConditions
            menu.findItem(R.id.menu_view_grid).setVisible(false);
            //noinspection ConstantConditions
            menu.findItem(R.id.menu_sort).setVisible(false);
            //noinspection ConstantConditions
            menu.findItem(R.id.menu_add_scene).setVisible(false);
            //noinspection ConstantConditions
            menu.findItem(R.id.menu_requery).setVisible(false);
            return;
        }

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

        boolean isList = adapter == null || adapter.getLayoutRes() == R.layout.list_icon_item;
        //noinspection ConstantConditions
        menu.findItem(R.id.menu_view_list).setVisible(!isList);
        //noinspection ConstantConditions
        menu.findItem(R.id.menu_view_grid).setVisible(isList);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_main_beer:
                MainActivity.instance.donate.buy("beer");
                return true;
            case R.id.menu_main_beer2:
                MainActivity.instance.donate.buy("beer2");
                return true;
            case R.id.menu_main_beer3:
                MainActivity.instance.donate.buy("beer3");
                return true;
            case R.id.menu_main_beer4:
                MainActivity.instance.donate.buy("beer4");
                return true;

            case R.id.menu_removeGroup: {
                if (!NetpowerctrlApplication.getDataController().groupCollection.remove(groupFilter)) {
                    return true;
                }
                MainActivity.instance.changeToFragment(OutletsFragment.class.getName());
                return true;
            }
            case R.id.menu_renameGroup: {
                GroupUtilities.renameGroup(getActivity(), groupFilter);
                return true;
            }

            case R.id.menu_requery: {
                NetpowerctrlApplication.instance.findDevices(new DeviceObserverResult() {
                    @Override
                    public void onDeviceError(DeviceInfo di) {
                    }

                    @Override
                    public void onDeviceTimeout(DeviceInfo di) {
                    }

                    @Override
                    public void onDeviceUpdated(DeviceInfo di) {
                    }

                    @Override
                    public void onObserverJobFinished(List<DeviceInfo> timeout_devices) {
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
            case R.id.menu_view_list: {
                setListOrGrid(false, mListView.getWidth());
                //noinspection ConstantConditions
                getActivity().invalidateOptionsMenu();
                return true;
            }

            case R.id.menu_view_grid: {
                setListOrGrid(true, mListView.getWidth());
                //noinspection ConstantConditions
                getActivity().invalidateOptionsMenu();
                return true;
            }
            case R.id.menu_add_scene:
                Scene scene = new Scene();
                scene.sceneItems = adapter.getScene();
                final String sceneJSON = scene.toJSON();
                Intent it = new Intent(getActivity(), EditSceneActivity.class);
                it.putExtra(EditSceneActivity.EDIT_SCENE_NOT_SHORTCUT, true);
                it.putExtra(EditSceneActivity.LOAD_SCENE, sceneJSON);
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

            case R.id.menu_sort: {
                Fragment fragment = SortCriteriaDialog.instantiate(getActivity(), adapter);
                ShowToast.showDialogFragment(getActivity(), fragment);
            }
        }
        return false;
    }

    private GridView mListView;

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_outlets, container, false);
        assert view != null;
        mListView = (GridView) view.findViewById(android.R.id.list);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                adapter.handleClick(position);
            }
        });
        hintText = (TextView) view.findViewById(R.id.hintText);
        if (!NetpowerctrlApplication.getDataController().deviceCollection.hasDevices()) {
            mListView.setEmptyView(view.findViewById(android.R.id.empty));
            emptyText = (TextView) view.findViewById(R.id.empty_text);
            emptyText.setText(getString(R.string.empty_no_outlets_no_devices));
        } else {
            mListView.setEmptyView(view.findViewById(R.id.loading));
            // We assign the empty view after a short delay time,
            // to reduce visual flicker on app start, where data
            // is loaded with a high chance within the first 500ms.
            NetpowerctrlApplication.getMainThreadHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isAdded())
                        return;

                    View v = mListView.getEmptyView();
                    if (v != null)
                        v.setVisibility(View.GONE);
                    mListView.setEmptyView(view.findViewById(android.R.id.empty));
                    emptyText = (TextView) view.findViewById(R.id.empty_text);
                    emptyText.setText(groupFilter == null ?
                            getString(R.string.empty_no_outlets) : getString(R.string.empty_group));

                }
            }, 1000);
        }

        btnChangeToBackup = (Button) view.findViewById(R.id.btnChangeToBackup);
        btnChangeToBackup.setVisibility(groupFilter == null ? View.VISIBLE : View.GONE);
        btnChangeToBackup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.instance.changeToFragment(GDriveFragment.class.getName());
            }
        });
        btnChangeToDevices = (Button) view.findViewById(R.id.btnChangeToDevices);
        btnChangeToDevices.setVisibility(groupFilter == null ? View.VISIBLE : View.GONE);
        btnChangeToDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.instance.changeToFragment(DevicesFragment.class.getName());
            }
        });

        // Create adapter first
        adapter = new DevicePortsExecuteAdapter(getActivity(), this);
        adapter.setNotReachableObserver(this);

        // Assign adapter, either pure or animated
        if (SharedPrefs.getAnimationEnabled()) {
            animatedAdapter = new AlphaInAnimationAdapter(adapter);
            animatedAdapter.setAbsListView(mListView);
        }
        setListOrGrid(SharedPrefs.getOutletsGrid(), container.getWidth());

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

                alert.setTitle(R.string.outlet_rename_title);
                alert.setMessage(getString(R.string.outlet_rename_message, oi.getDescription()));

                final EditText input = new EditText(alert.getContext());
                input.setText(oi.getDescription());
                alert.setView(input);

                alert.setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //noinspection ConstantConditions
                        NetpowerctrlApplication.getDataController().rename(oi, input.getText().toString().trim(), OutletsFragment.this);
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
                NetpowerctrlApplication.getDataController().deviceCollection.save();

                // update adapter
                adapter.setGroupFilter(groupFilter);
                return true;
            }
            case R.id.menu_outlet_createGroup: {
                GroupUtilities.createGroup(getActivity(), oi);
                return true;
            }
            case R.id.menu_outlet_hide: {
                oi.Hidden = true;
                // onDeviceUpdated will either call notifyDataSetChanged if the second parameter is
                // false or it will reconstruct the entire adapter.
                adapter.onDeviceUpdated(null, !adapter.getIsShowingHidden());
                NetpowerctrlApplication.getDataController().deviceCollection.save();
                return true;
            }
            case R.id.menu_outlet_unhide: {
                oi.Hidden = false;
                adapter.notifyDataSetChanged();
                NetpowerctrlApplication.getDataController().deviceCollection.save();
                return true;
            }
            case R.id.menu_add_homescreen: {
                Scene scene = new Scene();
                scene.sceneName = oi.getDescription();
                scene.add(oi.uuid, DevicePort.TOGGLE);
                //noinspection ConstantConditions
                Shortcuts.createHomeIcon(getActivity().getApplicationContext(), scene);
                return true;
            }
            case R.id.menu_outlet_master_slave:
                //noinspection ConstantConditions
                Bundle b = new Bundle();
                b.putString("master_uuid", oi.uuid.toString());
                @SuppressWarnings("ConstantConditions")
                Fragment fragment = Fragment.instantiate(getActivity(), MasterSlaveFragment.class.getName(), b);
                //noinspection ConstantConditions
                getFragmentManager().beginTransaction().addToBackStack(null).
                        replace(R.id.content_frame, fragment).commit();
                return true;
            case R.id.menu_icon:
                Icons.show_select_icon_dialog(getActivity(), "scene_icons", this, oi);
                return true;
        }
        return false;
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

    private ProgressDialog progressDialog;

    @Override
    public void asyncRunnerResult(DevicePort oi, boolean success, String error_message) {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        if (!success) {
            //noinspection ConstantConditions
            Toast.makeText(getActivity(), getString(R.string.renameFailed, error_message), Toast.LENGTH_SHORT).show();
        } else {
            oi.device.setHasChanged();
            new DeviceQuery(null, oi.device);
        }
    }

    @Override
    public void asyncRunnerStart(DevicePort oi) {
        Context context = getActivity();
        if (context == null)
            return;

        if (progressDialog == null)
            progressDialog = new ProgressDialog(context);

        progressDialog.setTitle(R.string.renameInProgress);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    @Override
    public void setIcon(Object context_object, Bitmap bitmap) {
        if (context_object == null)
            return;
        NetpowerctrlApplication.getDataController().deviceCollection.setDevicePortBitmap(getActivity(),
                (DevicePort) context_object, bitmap);

        adapter.invalidateViewHolders();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        Icons.activityCheckForPickedImage(getActivity(), this, requestCode, resultCode, imageReturnedIntent);
    }
}
