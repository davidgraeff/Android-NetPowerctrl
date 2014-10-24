package oly.netpowerctrl.outletsview;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.Executable;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.data.onDataQueryCompleted;
import oly.netpowerctrl.device_ports.DevicePort;
import oly.netpowerctrl.device_ports.DevicePortSourceConfigured;
import oly.netpowerctrl.device_ports.DevicePortViewHolder;
import oly.netpowerctrl.device_ports.ExecutableAdapterItem;
import oly.netpowerctrl.device_ports.ExecuteAdapter;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.devices.DevicesAutomaticFragment;
import oly.netpowerctrl.devices.DevicesFragment;
import oly.netpowerctrl.devices.UnconfiguredDeviceCollection;
import oly.netpowerctrl.groups.GroupUtilities;
import oly.netpowerctrl.listen_service.ListenService;
import oly.netpowerctrl.listen_service.onServiceModeChanged;
import oly.netpowerctrl.listen_service.onServiceRefreshQuery;
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.network.onNotReachableUpdate;
import oly.netpowerctrl.scenes.EditSceneActivity;
import oly.netpowerctrl.scenes.Scene;
import oly.netpowerctrl.utils.AnimationController;
import oly.netpowerctrl.utils.RecyclerItemClickListener;
import oly.netpowerctrl.utils.actionbar.ActionBarWithGroups;
import oly.netpowerctrl.utils.controls.ActivityWithIconCache;
import oly.netpowerctrl.utils.controls.FloatingActionButton;
import oly.netpowerctrl.utils.fragments.onFragmentChangeArguments;
import oly.netpowerctrl.utils.notifications.InAppNotifications;
import oly.netpowerctrl.utils.notifications.TextNotification;

/**
 */
public class OutletsFragment extends Fragment implements PopupMenu.OnMenuItemClickListener,
        onNotReachableUpdate, onFragmentChangeArguments, SwipeRefreshLayout.OnRefreshListener,
        onServiceRefreshQuery, SharedPrefs.IHideNotReachable, onServiceModeChanged,
        DevicePortSourceConfigured.onChange, onDataQueryCompleted, onCollectionUpdated<UnconfiguredDeviceCollection, Device> {

    public static final int VIEW_AS_LIST = 0;
    public static final int VIEW_AS_GRID = 1;
    public static final int VIEW_AS_COMPACT = 2;

    private final ActionBarWithGroups actionBarWithGroups = new ActionBarWithGroups();
    int requestedColumnWidth;
    private ExecuteAdapter adapter;
    private DevicePortSourceConfigured adapterSource;
    private TextView emptyText;
    private Button btnChangeToDevices;
    private Button btnAutomaticConfiguration;
    private UUID groupFilter = null;
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mPullToRefreshLayout;
    private int emptyInit = 0;
    private checkEmptyState lastState = checkEmptyState.UNKNOWN;

    public OutletsFragment() {
    }

    public static Bundle createBundleForView(int viewType) {
        Bundle bundle = new Bundle();
        bundle.putInt("viewtype", viewType);
        return bundle;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (mRecyclerView != null) {
            mRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(mListViewNumColumnsChangeListener);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPrefs.getInstance().registerHideNotReachable(this);
        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        actionBarWithGroups.initNavigation(((ActionBarActivity) getActivity()).getSupportActionBar(),
                MainActivity.getNavigationController());
        actionBarWithGroups.showNavigation();
    }

    @Override
    public void onStop() {
        super.onStop();
        actionBarWithGroups.finishNavigation();
    }

    @Override
    public void onPause() {
        ListenService.observersStartStopRefresh.unregister(this);
        ListenService.observersServiceModeChanged.unregister(this);
        AppData.getInstance().unconfiguredDeviceCollection.unregisterObserver(this);
        super.onPause();
        if (adapterSource != null)
            adapterSource.onPause();
    }

    @Override
    public void onResume() {
        ListenService.observersStartStopRefresh.register(this);
        ListenService.observersServiceModeChanged.register(this);
        AppData.getInstance().unconfiguredDeviceCollection.registerObserver(this);

        if (adapterSource != null)
            adapterSource.onResume();

        InAppNotifications.showPermanentNotifications(getActivity());

        super.onResume();
    }

    @Override
    public void changeArguments(Bundle mExtra) {
        if (mExtra != null && mExtra.containsKey("viewtype") && adapter != null) {
            setViewType(mExtra.getInt("viewtype"));
            return;
        }

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

    private final ViewTreeObserver.OnGlobalLayoutListener mListViewNumColumnsChangeListener =
            new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    //noinspection deprecation
                    mRecyclerView.getViewTreeObserver().removeGlobalOnLayoutListener(mListViewNumColumnsChangeListener);
                    //getActivity().findViewById(R.id.content_frame).getWidth();
                    //Log.w("width", String.valueOf(mListView.getMeasuredWidth()));
                    int i = mRecyclerView.getWidth() / requestedColumnWidth;
                    adapter.setItemsInRow(i);
//                    SpannableGridLayoutManager spannableGridLayoutManager = new SpannableGridLayoutManager(getActivity());
//                    spannableGridLayoutManager.setNumColumns(i);
//                    spannableGridLayoutManager.setNumRows(1);
                    GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), i);
                    gridLayoutManager.setSpanSizeLookup(adapter.getSpanSizeLookup());
                    mRecyclerView.setHasFixedSize(false);
                    mRecyclerView.setLayoutManager(gridLayoutManager);
                    mRecyclerView.setAdapter(adapter);

                }
            };

    private void setViewType(int viewType) {
        SharedPrefs.getInstance().setOutletsViewType(viewType);

        switch (viewType) {
            case VIEW_AS_COMPACT:
                adapter.setLayoutRes(R.layout.grid_item_icon_center);
                requestedColumnWidth = (int) getResources().getDimension(R.dimen.min_grid_item_width);
                break;
            case VIEW_AS_GRID:
                adapter.setLayoutRes(R.layout.grid_item_icon);
                requestedColumnWidth = (int) getResources().getDimension(R.dimen.min_grid_item_width);
                break;
            case VIEW_AS_LIST:
            default:
                adapter.setLayoutRes(R.layout.list_item_icon);
                requestedColumnWidth = (int) getResources().getDimension(R.dimen.min_list_item_width);
                break;
        }

        mRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(mListViewNumColumnsChangeListener);
        adapter.setEnableEditing(SharedPrefs.getInstance().isOutletEditingEnabled());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.outlets, menu);

        boolean hasDevices = AppData.getInstance().deviceCollection.hasDevices();
        menu.findItem(R.id.refresh).setVisible(hasDevices);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh: {
                onRefresh();
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
        mRecyclerView = (RecyclerView) view.findViewById(android.R.id.list);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        ///// For pull to refresh
        mPullToRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.ptr_layout);
        mPullToRefreshLayout.setOnRefreshListener(this);
        mPullToRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        ///// END: For pull to refresh

        // Empty text and hint text
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

        if (SharedPrefs.getInstance().isFullscreen()) {
            FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fabNext);
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    actionBarWithGroups.next();
                }
            });
            AnimationController.animateFloatingButton(fab);
            fab = (FloatingActionButton) view.findViewById(R.id.fabPrevious);
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    actionBarWithGroups.previous();
                }
            });
            AnimationController.animateFloatingButton(fab);
        }

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
        adapter = new ExecuteAdapter(adapterSource,
                ((ActivityWithIconCache) getActivity()).getIconCache());
        if (groupFilter != null) {
            if (adapter.setGroupFilter(groupFilter))
                adapterSource.updateNow();
        }

        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                ExecutableAdapterItem item = adapter.getItem(position);
                if (!(item.groupType() == ExecutableAdapterItem.groupTypeEnum.NOGROUP_TYPE)) {
                    mRecyclerView.setTag(position);
                    //noinspection ConstantConditions
                    PopupMenu popup = new PopupMenu(getActivity(), view);
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.group, popup.getMenu());
                    popup.setOnMenuItemClickListener(OutletsFragment.this);
                    popup.show();
                    return;
                }

                if (view.getId() == R.id.icon_edit) {
                    onEditViewClicked(view, position);
                    return;
                }

                boolean restrict = SharedPrefs.getInstance().getSmallerClickExecuteArea();
                if (restrict && view.getId() != R.id.title)
                    return;

                // We animate the click event. This is done by calling animate on the
                // viewHolder of the current position.
                ((DevicePortViewHolder) mRecyclerView.findViewHolderForPosition(position)).animate();
                adapter.notifyItemChanged(position);

                AppData.getInstance().executeToggle(item.getExecutable(), null);
            }
        }, new RecyclerItemClickListener.OnItemFlingListener() {
            @Override
            public void onFling(View view, int position, float distance) {
                //ExecutableAdapterItem item = adapter.getItem(position);
                //Toast.makeText(getActivity(), item.getExecutable().getTitle() +" " + String.valueOf(distance), Toast.LENGTH_SHORT).show();
            }
        }));

        checkEmptyChanged(checkEmptyAction.INIT_AFTER_VIEW);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        setViewType(SharedPrefs.getInstance().getOutletsViewType());

        AppData.observersDataQueryCompleted.register(this);

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public boolean onDataQueryFinished() {
        checkEmptyChanged(checkEmptyAction.FIRSTDATA);
        return false;
    }

    @Override
    public boolean updated(UnconfiguredDeviceCollection unconfiguredDeviceCollection, Device device, ObserverUpdateActions action, int position) {
        checkEmptyChanged(checkEmptyAction.NEWDEVICE);
        return true;
    }

    private void checkEmptyChanged(checkEmptyAction newState) {
        if (mRecyclerView == null)
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
            case SERVICE:
                if (emptyInit == 2) {
                    checkEmpty();
                    checkService();
                }
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

        if (adapter.getItemCount() == 0) {
            lastState = checkEmptyState.EMPTY;

            boolean hasDevices = AppData.getInstance().deviceCollection.hasDevices();
            boolean hasNewDevices = AppData.getInstance().unconfiguredDeviceCollection.size() > 0;
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

            getActivity().findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
        } else {
            getActivity().findViewById(android.R.id.empty).setVisibility(View.GONE);
            lastState = checkEmptyState.FILLED;
        }

        getActivity().invalidateOptionsMenu();
    }

    void checkService() {
        boolean en = (!ListenService.isServiceReady() || ListenService.getService().isNetworkReducedMode());
        if (en)
            InAppNotifications.addPermanentNotification(getActivity(), new TextNotification("energy", getString(R.string.device_energysave_mode), false));
        else
            InAppNotifications.removePermanentNotification(getActivity(), "energy");
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        final int position = (Integer) mRecyclerView.getTag();

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
        }
        return false;
    }

//    private void showReorder() {
//        //noinspection ConstantConditions
//        Bundle b = new Bundle();
//        b.putString("master_uuid", devicePort.uuid.toString());
//        MainActivity.getNavigationController().changeToFragment(MasterSlaveFragment.class.getName(), b, true);
//
//    DialogFragment fragment = SortCriteriaDialog.instantiate(getActivity(), adapter);
//    MainActivity.getNavigationController().changeToDialog(getActivity(), fragment);
//    }

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
            InAppNotifications.addPermanentNotification(getActivity(), new TextNotification("not_reachable",
                    getString(R.string.error_not_reachable, devices), true));
        } else {
            InAppNotifications.removePermanentNotification(getActivity(), "not_reachable");
        }
    }


    public void onEditViewClicked(View view, int position) {
        // Animate press
        Animation a = new ScaleAnimation(1.0f, 0.8f, 1.0f, 0.8f, view.getWidth() / 2, view.getHeight() / 2);
        a.setRepeatMode(Animation.REVERSE);
        a.setRepeatCount(1);
        a.setDuration(300);
        view.startAnimation(a);

        Executable executable = adapter.getItem(position).getExecutable();
        if (executable instanceof DevicePort) {
            DevicePort devicePort = (DevicePort) executable;

            OutletEditDialog dialog = (OutletEditDialog) Fragment.instantiate(getActivity(), OutletEditDialog.class.getName());
            dialog.setDevicePort(devicePort, (DevicePortViewHolder) mRecyclerView.findViewHolderForPosition(position), adapter);
            MainActivity.getNavigationController().changeToDialog(getActivity(), dialog);
        } else if (executable instanceof Scene) {
            Intent it = new Intent(getActivity(), EditSceneActivity.class);
            it.putExtra(EditSceneActivity.EDIT_SCENE_NOT_SHORTCUT, true);
            it.putExtra(EditSceneActivity.LOAD_SCENE, ((Scene) executable).toString());
            startActivity(it);
        }
    }

    @Override
    public void onRefreshStateChanged(boolean isRefreshing) {
        mPullToRefreshLayout.setRefreshing(isRefreshing);
    }

    @Override
    public void hideNotReachable(boolean hideNotReachable) {
        adapterSource.setHideNotReachable(hideNotReachable);
        adapterSource.updateNow();
    }

    @Override
    public void onServiceModeChanged(boolean isNetworkDown) {
        if (!isNetworkDown)
            checkService();
        else
            checkEmptyChanged(checkEmptyAction.SERVICE);
    }

    @Override
    public void sourceChanged() {
        checkEmptyChanged(checkEmptyAction.ADDREMOVE);
    }

    @Override
    public void onRefresh() {
        ListenService service = ListenService.getService();
        if (service != null)
            service.findDevices(true, null);
        else {
            InAppNotifications.showException(getActivity(), null, "OutletsFragment refresh: Service is down.");
        }
    }

    private enum checkEmptyState {UNKNOWN, EMPTY, FILLED}

    private enum checkEmptyAction {SERVICE, FIRSTDATA, NEWDEVICE, INIT_AFTER_VIEW, ADDREMOVE}


}
