package oly.netpowerctrl.outletsview;

import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.data.onDataQueryCompleted;
import oly.netpowerctrl.device_ports.DevicePort;
import oly.netpowerctrl.device_ports.DevicePortAdapterItem;
import oly.netpowerctrl.device_ports.DevicePortSourceConfigured;
import oly.netpowerctrl.device_ports.DevicePortsExecuteAdapter;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.devices.DevicesAutomaticFragment;
import oly.netpowerctrl.devices.DevicesFragment;
import oly.netpowerctrl.groups.GroupUtilities;
import oly.netpowerctrl.listen_service.ListenService;
import oly.netpowerctrl.listen_service.onServiceModeChanged;
import oly.netpowerctrl.listen_service.onServiceRefreshQuery;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.network.onNewDevice;
import oly.netpowerctrl.network.onNotReachableUpdate;
import oly.netpowerctrl.utils.AnimationController;
import oly.netpowerctrl.utils.ShowToast;
import oly.netpowerctrl.utils.SortCriteriaDialog;
import oly.netpowerctrl.utils.actionbar.ActionBarWithGroups;
import oly.netpowerctrl.utils.controls.ActivityWithIconCache;
import oly.netpowerctrl.utils.controls.SwipeDismissListViewTouchListener;
import oly.netpowerctrl.utils.controls.onListItemElementClicked;
import oly.netpowerctrl.utils.fragments.onFragmentChangeArguments;

/**
 */
public class OutletsFragment extends Fragment implements PopupMenu.OnMenuItemClickListener,
        onNotReachableUpdate, onListItemElementClicked, onFragmentChangeArguments, SwipeRefreshLayout.OnRefreshListener, SwipeDismissListViewTouchListener.DismissCallbacks,
        onServiceRefreshQuery, SharedPrefs.IHideNotReachable, onServiceModeChanged,
        DevicePortSourceConfigured.onChange, onDataQueryCompleted, onNewDevice {
    private final ActionBarWithGroups actionBarWithGroups = new ActionBarWithGroups();
    int requestedColumnWidth;
    private DevicePortsExecuteAdapter adapter;
    private DevicePortSourceConfigured adapterSource;
    private TextView hintText;
    private TextView emptyText;
    private Button btnChangeToDevices;
    private Button btnAutomaticConfiguration;
    private UUID groupFilter = null;
    private GridView mListView;
    private SwipeRefreshLayout mPullToRefreshLayout;
    private AnimationController animationController;
    private int emptyInit = 0;
    private checkEmptyState lastState = checkEmptyState.UNKNOWN;

    public OutletsFragment() {
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (mListView != null) {
            mListView.getViewTreeObserver().addOnGlobalLayoutListener(mListViewNumColumnsChangeListener);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        animationController = new AnimationController(getActivity());
        SharedPrefs.getInstance().registerHideNotReachable(this);
        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        actionBarWithGroups.initNavigation(getActivity().getActionBar(), MainActivity.getNavigationController());
        actionBarWithGroups.showNavigation();
    }

    @Override
    public void onStop() {
        super.onStop();
        actionBarWithGroups.finishNavigation();
    }

    private final ViewTreeObserver.OnGlobalLayoutListener mListViewNumColumnsChangeListener =
            new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    //noinspection deprecation
                    mListView.getViewTreeObserver().removeGlobalOnLayoutListener(mListViewNumColumnsChangeListener);
                    //getActivity().findViewById(R.id.content_frame).getWidth();
                    //Log.w("width", String.valueOf(mListView.getMeasuredWidth()));
                    int i = mListView.getWidth() / requestedColumnWidth;
                    adapter.setItemsInRow(i);

                }
            };

    @Override
    public void onPause() {
        ListenService.observersStartStopRefresh.unregister(this);
        ListenService.observersServiceModeChanged.unregister(this);
        AppData.observersNew.unregister(this);
        super.onPause();
        if (adapterSource != null)
            adapterSource.onPause();
    }

    @Override
    public void onResume() {
        ListenService.observersStartStopRefresh.register(this);
        ListenService.observersServiceModeChanged.register(this);
        AppData.observersNew.register(this);

        if (adapterSource != null)
            adapterSource.onResume();
        super.onResume();
    }

    @Override
    public void changeArguments(Bundle mExtra) {
        UUID groupFilterBefore = groupFilter;

        if (mExtra != null && mExtra.containsKey("filter"))
            groupFilter = UUID.fromString(mExtra.getString("filter"));
        else
            groupFilter = null;

        if ((groupFilterBefore == null && groupFilter != null) || groupFilterBefore != null && !groupFilterBefore.equals(groupFilter))
            actionBarWithGroups.setCurrentIndex(groupFilter);

        if (adapter != null) {
            if (adapter.setGroupFilter(groupFilter))
                adapterSource.updateNow();
        }

        checkEmptyChanged(checkEmptyAction.ADDREMOVE);
    }

    private void setListOrGrid(boolean grid) {
        SharedPrefs.getInstance().setOutletsGrid(grid);

        if (!grid) {
            adapter.setLayoutRes(R.layout.list_item_icon);
            requestedColumnWidth = (int) getResources().getDimension(R.dimen.min_list_item_width);
        } else {
            adapter.setLayoutRes(R.layout.grid_item_icon);
            requestedColumnWidth = (int) getResources().getDimension(R.dimen.min_grid_item_width);
        }

        mListView.setColumnWidth(requestedColumnWidth);
        mListView.setNumColumns(GridView.AUTO_FIT);
        mListView.getViewTreeObserver().addOnGlobalLayoutListener(mListViewNumColumnsChangeListener);
        mListView.setAdapter(adapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.outlets, menu);

        if (!AppData.getInstance().deviceCollection.hasDevices()) {
            //noinspection ConstantConditions
            menu.findItem(R.id.menu_showhidden).setVisible(false);
            //noinspection ConstantConditions
            menu.findItem(R.id.menu_hidehidden).setVisible(false);
            //noinspection ConstantConditions
            menu.findItem(R.id.menu_view_list).setVisible(false);
            //noinspection ConstantConditions
            menu.findItem(R.id.menu_view_grid).setVisible(false);
            //noinspection ConstantConditions
            menu.findItem(R.id.menu_sort).setVisible(false);
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

        boolean isList = adapter == null || adapter.getLayoutRes() == R.layout.list_item_icon;
        //noinspection ConstantConditions
        menu.findItem(R.id.menu_view_list).setVisible(!isList);
        //noinspection ConstantConditions
        menu.findItem(R.id.menu_view_grid).setVisible(isList);

        menu.findItem(R.id.menu_debug_toggle_network_reduced).setVisible(App.isDebug());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh: {
                ListenService service = ListenService.getService();
                if (service != null)
                    service.findDevices(true, null);
                else {
                    ShowToast.showException(getActivity(), "Unexpected state: Service is down. Restart the app");
                }
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
                DialogFragment fragment = SortCriteriaDialog.instantiate(getActivity(), adapter);
                MainActivity.getNavigationController().changeToDialog(getActivity(), fragment);
                return true;
            }

            case R.id.menu_group_add: {
                GroupUtilities.createGroupForDevicePort(getActivity(), null);
                return true;
            }

            case R.id.menu_debug_toggle_network_reduced: {
                ListenService.debug_toggle_network_reduced();
                return true;
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

        ///// For swiping elements out (hiding)
        SwipeDismissListViewTouchListener touchListener =
                new SwipeDismissListViewTouchListener(
                        mListView, this);
        mListView.setOnTouchListener(touchListener);
        mListView.setOnScrollListener(touchListener.makeScrollListener());
        ///// END: For swiping elements out (hiding)

        ///// For pull to refresh
        mPullToRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.ptr_layout);
        mPullToRefreshLayout.setOnRefreshListener(this);
        mPullToRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        ///// END: For pull to refresh

        // Empty text and hint text
        hintText = (TextView) view.findViewById(R.id.hintText);
        emptyText = (TextView) view.findViewById(R.id.empty_text);

        // Empty list: Buttons
        btnAutomaticConfiguration = (Button) view.findViewById(R.id.btnAutomaticConfiguration);
        btnAutomaticConfiguration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.getNavigationController().changeToDialog(getActivity(), DevicesAutomaticFragment.class.getName());
            }
        });

        btnChangeToDevices = (Button) view.findViewById(R.id.btnChangeToDevices);
        btnChangeToDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.getNavigationController().changeToFragment(DevicesFragment.class.getName());
            }
        });

        Button btnWirelessLan = (Button) view.findViewById(R.id.btnWirelessSettings);
        btnWirelessLan.setVisibility(ListenService.isWirelessLanConnected(getActivity()) ? View.GONE : View.VISIBLE);
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
        adapterSource.setHideNotReachable(SharedPrefs.getInstance().isHideNotReachable());
        adapterSource.setAutomaticUpdate(true);
        adapterSource.setOnChangeListener(this);
        adapter = new DevicePortsExecuteAdapter(getActivity(), this, adapterSource,
                ((ActivityWithIconCache) getActivity()).getIconCache());
        if (groupFilter != null) {
            if (adapter.setGroupFilter(groupFilter))
                adapterSource.updateNow();
        }
        animationController.setAdapter(adapter);
        animationController.setListView(mListView);
        adapter.setAnimationController(animationController);

        // Click listener
        adapter.titleClick = new DevicePortsExecuteAdapter.TitleClick() {
            @Override
            public void onTitleClick(int position) {
                mListView.performItemClick(null, position, mListView.getItemIdAtPosition(position));
            }
        };
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                DevicePortAdapterItem item = adapter.getItem(position);
                if (!(item.groupType() == DevicePortAdapterItem.groupTypeEnum.NOGROUP_TYPE)) {
                    mListView.setTag(position);
                    //noinspection ConstantConditions
                    PopupMenu popup = new PopupMenu(getActivity(), view);
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.group, popup.getMenu());
                    popup.setOnMenuItemClickListener(OutletsFragment.this);
                    popup.show();
                    return;
                }
                boolean restrict = SharedPrefs.getInstance().getSmallerClickExecuteArea();
                if (restrict && view != null)
                    return;
                adapter.handleClick(position, id);
            }
        });

        checkEmptyChanged(checkEmptyAction.INIT_AFTER_VIEW);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        setListOrGrid(SharedPrefs.getInstance().getOutletsGrid());

        AppData.observersDataQueryCompleted.register(this);

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public boolean onDataQueryFinished() {
        checkEmptyChanged(checkEmptyAction.FIRSTDATA);
        return false;
    }

    @Override
    public void onNewDevice(Device device) {
        checkEmptyChanged(checkEmptyAction.NEWDEVICE);
    }

    private void checkEmptyChanged(checkEmptyAction newState) {
        if (mListView == null)
            return;

        if (newState == checkEmptyAction.INIT_AFTER_VIEW)
            emptyInit = 1;

        if (emptyInit == 0)
            return;

        switch (newState) {
            case ADDREMOVE:
                if (emptyInit == 2 && lastState == checkEmptyState.EMPTY)
                    checkEmpty();
                break;
            case FIRSTDATA:
                emptyInit = 2;
                checkEmpty();
                checkService();
                break;
            case NEWDEVICE:
                if (lastState == checkEmptyState.EMPTY)
                    checkEmpty();
                break;

        }
    }

    /**
     * Do not call this directly! Use checkEmptyChanged instead.
     */
    private void checkEmpty() {
        lastState = checkEmptyState.UNKNOWN;

        if (!isAdded())
            return;

        if (mListView.getCount() == 0) {
            lastState = checkEmptyState.EMPTY;

            boolean hasDevices = AppData.getInstance().deviceCollection.hasDevices();
            boolean hasNewDevices = AppData.getInstance().newDevices.size() > 0;
            if (!hasDevices) {
                emptyText.setText(R.string.empty_no_outlets_no_devices);
            } else if (groupFilter != null) {
                emptyText.setText(R.string.empty_group);
            } else {
                emptyText.setText(R.string.empty_no_outlets);
            }

            if (btnChangeToDevices != null)
                btnChangeToDevices.setVisibility(groupFilter == null ? View.VISIBLE : View.GONE);
            if (btnAutomaticConfiguration != null)
                btnAutomaticConfiguration.setVisibility(groupFilter == null && !hasDevices && hasNewDevices ? View.VISIBLE : View.GONE);

            mListView.setEmptyView(getActivity().findViewById(android.R.id.empty));
        } else
            lastState = checkEmptyState.FILLED;

        getActivity().invalidateOptionsMenu();
    }

    void checkService() {
        boolean en = (!ListenService.isServiceReady() || ListenService.getService().isNetworkReducedMode());
        if (en)
            hintText.setText(getString(R.string.device_energysave_mode));
        hintText.setVisibility(en ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        final int position = (Integer) mListView.getTag();
        final DevicePort devicePort = adapter.getDevicePort(position);

        switch (menuItem.getItemId()) {
            case R.id.menu_removeGroup: {
                UUID uuid = adapter.getItem(position).groupID();
                if (!AppData.getInstance().groupCollection.remove(uuid)) {
                    return true;
                }
                // change to overview if the removed group is the current group
                if (uuid.equals(groupFilter))
                    MainActivity.getNavigationController().changeToFragment(OutletsFragment.class.getName());
                return true;
            }
            case R.id.menu_renameGroup: {
                GroupUtilities.renameGroup(getActivity(), adapter.getItem(position).groupID());
                return true;
            }
            case R.id.menu_outlet_edit: {
                OutletEditDialog dialog = (OutletEditDialog) Fragment.instantiate(getActivity(), OutletEditDialog.class.getName());
                dialog.setDevicePort(devicePort);
                dialog.setAdapter(adapter);
                MainActivity.getNavigationController().changeToDialog(getActivity(), dialog);
                return true;
            }

            case R.id.menu_outlet_unhide: {
                devicePort.Hidden = false;
                adapter.notifyDataSetChanged();
                // Only change the view, if we are actually showing an item and do not show hidden items.
                if (!adapter.isShowingHidden()) {
                    adapter.addItem(devicePort, devicePort.getCurrentValueToggled(), true);
                } else // just update the alpha value of the newly hidden item
                    adapter.notifyDataSetChanged();
                AppData.getInstance().deviceCollection.save(devicePort.device);
                return true;
            }
            case R.id.menu_outlet_master_slave:
                //noinspection ConstantConditions
                Bundle b = new Bundle();
                b.putString("master_uuid", devicePort.uuid.toString());
                MainActivity.getNavigationController().changeToFragment(MasterSlaveFragment.class.getName(), b, true);
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
        AppData.getInstance().deviceCollection.save(oi.device);
        Toast.makeText(getActivity(), R.string.outlets_hidden, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNotReachableUpdate(List<Device> not_reachable) {
        Context context = getActivity();
        if (context == null)
            return;

        if (not_reachable.size() > 0) {
            String devices = "";
            for (Device di : not_reachable) {
                devices += di.DeviceName + " ";
            }
            hintText.setText(context.getString(R.string.error_not_reachable) + ": " + devices);
            hintText.setVisibility(View.VISIBLE);
        } else {
            hintText.setVisibility(View.GONE);
        }
    }

    @Override
    public void onListItemElementClicked(View view, int position) {
        // Animate press
        Animation a = new ScaleAnimation(1.0f, 0.8f, 1.0f, 0.8f, view.getWidth() / 2, view.getHeight() / 2);
        a.setRepeatMode(Animation.REVERSE);
        a.setRepeatCount(1);
        a.setDuration(300);
        view.startAnimation(a);

        mListView.setTag(position);
        DevicePort oi = adapter.getDevicePort(position);

        //noinspection ConstantConditions
        PopupMenu popup = new PopupMenu(getActivity(), view);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.outlets_item, popup.getMenu());

        Menu menu = popup.getMenu();

        //noinspection ConstantConditions
        menu.findItem(R.id.menu_outlet_unhide).setVisible(oi.Hidden);

        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

    @Override
    public void onRefreshStateChanged(boolean isRefreshing) {
        mPullToRefreshLayout.setRefreshing(isRefreshing);
    }

    @Override
    public boolean canDismiss(int position) {
        return !adapter.isShowingHidden() && adapter.getDevicePort(position) != null;
    }

    @Override
    public void onDismiss(int dismissedPosition) {
        final DevicePort oi = adapter.getDevicePort(dismissedPosition);
        if (oi != null)
            hideItem(oi);
    }

    @Override
    public void hideNotReachable(boolean hideNotReachable) {
        adapterSource.setHideNotReachable(hideNotReachable);
        adapterSource.updateNow();
    }

    @Override
    public void onServiceModeChanged(boolean isNetworkDown) {
        checkService();
    }

    @Override
    public void devicePortSourceChanged() {
        checkEmptyChanged(checkEmptyAction.ADDREMOVE);
    }

    @Override
    public void onRefresh() {
        ListenService.getService().findDevices(true, null);
    }

    private enum checkEmptyState {UNKNOWN, EMPTY, FILLED}

    private enum checkEmptyAction {SERVICE, FIRSTDATA, NEWDEVICE, INIT_AFTER_VIEW, ADDREMOVE}


}
