package oly.netpowerctrl.main;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
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
import oly.netpowerctrl.application_state.NetpowerctrlService;
import oly.netpowerctrl.backup.drive.GDriveFragment;
import oly.netpowerctrl.device_ports.DevicePortSourceConfigured;
import oly.netpowerctrl.device_ports.DevicePortsExecuteAdapter;
import oly.netpowerctrl.devices.DeviceInfo;
import oly.netpowerctrl.devices.DevicePort;
import oly.netpowerctrl.devices.DevicesFragment;
import oly.netpowerctrl.devices.NotReachableUpdate;
import oly.netpowerctrl.groups.GroupUtilities;
import oly.netpowerctrl.network.AsyncRunnerResult;
import oly.netpowerctrl.network.DeviceObserverFinishedResult;
import oly.netpowerctrl.network.DeviceQuery;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.scenes.EditSceneActivity;
import oly.netpowerctrl.scenes.Scene;
import oly.netpowerctrl.utils.ActivityWithIconCache;
import oly.netpowerctrl.utils.Icons;
import oly.netpowerctrl.utils.ListItemMenu;
import oly.netpowerctrl.utils.Shortcuts;
import oly.netpowerctrl.utils.ShowToast;
import oly.netpowerctrl.utils.gui.ChangeArgumentsFragment;
import oly.netpowerctrl.utils.gui.RemoveAnimation;
import oly.netpowerctrl.utils.gui.SwipeDismissListViewTouchListener;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

/**
 */
public class OutletsFragment extends Fragment implements PopupMenu.OnMenuItemClickListener,
        NotReachableUpdate, ListItemMenu, ChangeArgumentsFragment, AsyncRunnerResult, Icons.IconSelected, OnRefreshListener, SwipeDismissListViewTouchListener.DismissCallbacks {
    private DevicePortsExecuteAdapter adapter;
    private DevicePortSourceConfigured adapterSource;
    private TextView hintText;
    private TextView emptyText;
    private Button btnChangeToDevices;
    private Button btnChangeToBackup;
    private UUID groupFilter = null;
    private GridView mListView;
    private PullToRefreshLayout mPullToRefreshLayout;
    private ProgressDialog progressDialog;
    private RemoveAnimation removeAnimation = new RemoveAnimation();
    private ViewTreeObserver.OnGlobalLayoutListener mListViewNumColumsChangeListener =
            new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    int i = mListView.getNumColumns();
                    if (i != GridView.AUTO_FIT)
                        adapter.setItemsInRow(i);
                }
            };

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
        if (adapterSource != null)
            adapterSource.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapterSource != null)
            adapterSource.onResume();
    }

    @Override
    public void changeArguments(Bundle mExtra) {

        if (mExtra != null && mExtra.containsKey("filter"))
            groupFilter = UUID.fromString(mExtra.getString("filter"));
        else
            groupFilter = null;

        if (adapter != null) {
            adapter.setGroupFilter(groupFilter);
            adapterSource.updateNow();
        }

        if (isAdded()) {
            if (btnChangeToDevices != null)
                btnChangeToDevices.setVisibility(groupFilter == null ? View.VISIBLE : View.GONE);
            if (btnChangeToBackup != null)
                btnChangeToBackup.setVisibility(groupFilter == null ? View.VISIBLE : View.GONE);
            if (emptyText != null)
                emptyText.setText(groupFilter == null ? getString(R.string.empty_no_outlets) : getString(R.string.empty_group));
        }
    }

    private void setListOrGrid(boolean grid) {
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
        mListView.getViewTreeObserver().addOnGlobalLayoutListener(mListViewNumColumsChangeListener);

        adapter.setGroupFilter(groupFilter);
        mListView.setAdapter(adapter);
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
            menu.findItem(R.id.refresh).setVisible(false);
            return;
        }

        boolean hiddenShown = false;
        if (adapter != null) {
            hiddenShown = adapter.isShowingHidden();
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

            case R.id.refresh: {
                refresh(false);
                return true;
            }
            case R.id.menu_view_list: {
                setListOrGrid(false);
                //noinspection ConstantConditions
                getActivity().invalidateOptionsMenu();
                return true;
            }

            case R.id.menu_view_grid: {
                setListOrGrid(true);
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

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_outlets, container, false);
        assert view != null;
        mListView = (GridView) view.findViewById(android.R.id.list);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                adapter.handleClick(position, id);
            }
        });

        ///// For swiping elements out (hiding)
        SwipeDismissListViewTouchListener touchListener =
                new SwipeDismissListViewTouchListener(
                        mListView, this);
        mListView.setOnTouchListener(touchListener);
        mListView.setOnScrollListener(touchListener.makeScrollListener());
        ///// END: For swiping elements out (hiding)

        ///// For pull to refresh
        mPullToRefreshLayout = (PullToRefreshLayout) view.findViewById(R.id.ptr_layout);
        ActionBarPullToRefresh.from(getActivity())
                .allChildrenArePullable()
                .listener(this)
                .setup(mPullToRefreshLayout);
        ///// END: For pull to refresh

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

        Button btnWirelessLan = (Button) view.findViewById(R.id.btnWirelessSettings);
        btnWirelessLan.setVisibility(NetpowerctrlService.isWirelessLanConnected() ? View.GONE : View.VISIBLE);
        btnWirelessLan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                //intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        });

        // Create adapter first
        adapterSource = new DevicePortSourceConfigured();
        adapterSource.setAutomaticUpdate(true);
        adapter = new DevicePortsExecuteAdapter(getActivity(), this, adapterSource,
                ((ActivityWithIconCache) getActivity()).getIconCache());
        removeAnimation.setAdapter(adapter);
        removeAnimation.setListView(mListView);
        adapter.setRemoveAnimation(removeAnimation);
        setListOrGrid(SharedPrefs.getOutletsGrid());

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

            case R.id.menu_outlet_changeGroups: {
                GroupUtilities.createGroup(getActivity(), oi, new GroupUtilities.onGroupsChanged() {
                    @Override
                    public void onGroupsChanged(DevicePort port) {
                        adapterSource.updateNow();
                    }
                });
                return true;
            }
            case R.id.menu_outlet_hide: {
                hideItem(oi);
                return true;
            }
            case R.id.menu_outlet_unhide: {
                oi.Hidden = false;
                adapter.notifyDataSetChanged();
                // Only change the view, if we are actually showing an item and do not show hidden items.
                if (!adapter.isShowingHidden()) {
                    adapter.addItem(oi, oi.getCurrentValueToggled(), true);
                } else // just update the alpha value of the newly hidden item
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

    private void hideItem(DevicePort oi) {
        oi.Hidden = true;
        // Only change the view, if we are actually hiding an item and do not show hidden items.
        if (!adapter.isShowingHidden()) {
            adapter.remove(oi, true);
        }
        adapter.notifyDataSetChanged();
        NetpowerctrlApplication.getDataController().deviceCollection.save();
        Toast.makeText(getActivity(), R.string.outlets_hidden, Toast.LENGTH_SHORT).show();
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

        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

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

    private void refresh(boolean ignoreFlag) {
        if (!ignoreFlag && mPullToRefreshLayout.isRefreshing())
            return;

        mPullToRefreshLayout.setRefreshing(true);
        NetpowerctrlService.getService().findDevices(new DeviceObserverFinishedResult() {
            @Override
            public void onObserverJobFinished(List<DeviceInfo> timeout_devices) {
                mPullToRefreshLayout.setRefreshComplete();
                //noinspection ConstantConditions
                Toast.makeText(getActivity(),
                        getActivity().getString(R.string.devices_refreshed,
                                NetpowerctrlApplication.getDataController().getReachableConfiguredDevices(),
                                NetpowerctrlApplication.getDataController().newDevices.size()),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    @Override
    public void onRefreshStarted(View view) {
        refresh(true);
    }

    @Override
    public boolean canDismiss(int position) {
        return !adapter.isShowingHidden();
    }

    @Override
    public void onDismiss(int dismissedPosition) {
        final DevicePort oi = adapter.getItem(dismissedPosition);
        hideItem(oi);
    }
}
