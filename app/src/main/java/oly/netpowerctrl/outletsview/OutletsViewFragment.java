package oly.netpowerctrl.outletsview;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.IconCacheCleared;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.device_base.executables.Executable;
import oly.netpowerctrl.devices.DevicesAutomaticDialog;
import oly.netpowerctrl.executables.ExecutableAdapterItem;
import oly.netpowerctrl.executables.ExecutableViewHolder;
import oly.netpowerctrl.executables.ExecutablesSourceBase;
import oly.netpowerctrl.executables.ExecutablesSourceChain;
import oly.netpowerctrl.executables.ExecutablesSourceDevicePorts;
import oly.netpowerctrl.executables.ExecutablesSourceGroups;
import oly.netpowerctrl.executables.ExecutablesSourceScenes;
import oly.netpowerctrl.executables.ExecuteAdapter;
import oly.netpowerctrl.groups.GroupUtilities;
import oly.netpowerctrl.listen_service.ListenService;
import oly.netpowerctrl.listen_service.onServiceModeChanged;
import oly.netpowerctrl.listen_service.onServiceRefreshQuery;
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.network.onNotReachableUpdate;
import oly.netpowerctrl.scenes.EditSceneActivity;
import oly.netpowerctrl.scenes.Scene;
import oly.netpowerctrl.ui.RecyclerItemClickListener;
import oly.netpowerctrl.ui.SwipeMoveAnimator;
import oly.netpowerctrl.ui.notifications.InAppNotifications;
import oly.netpowerctrl.ui.notifications.TextNotification;
import oly.netpowerctrl.ui.widgets.EnhancedSwipeRefreshLayout;
import oly.netpowerctrl.ui.widgets.FloatingActionButton;
import oly.netpowerctrl.ui.widgets.SlidingTabLayout;
import oly.netpowerctrl.utils.AnimationController;
import oly.netpowerctrl.utils.fragments.onFragmentChangeArguments;

/**
 * This fragment consists of some floating buttons (add + no_wireless) and a view pager. Within the
 * fragments in the view pager is a RecycleView and a placeholder text. The computation for
 */
public class OutletsViewFragment extends Fragment implements PopupMenu.OnMenuItemClickListener,
        onNotReachableUpdate, onFragmentChangeArguments,
        onServiceRefreshQuery, SharedPrefs.IHideNotReachable, onServiceModeChanged,
        SwipeRefreshLayout.OnRefreshListener, IconCacheCleared, RecyclerItemClickListener.OnItemClickListener {

    // Column computations
    public static final int VIEW_AS_LIST = 0;
    public static final int VIEW_AS_GRID = 1;
    public static final int VIEW_AS_COMPACT = 2;
    SwipeMoveAnimator swipeMoveAnimator;
    private RecyclerView mRecyclerView;
    private TextView emptyText;
    private TextView groupTitle;
    private EnhancedSwipeRefreshLayout mPullToRefreshLayout;
    private SlidingTabLayout slidingTabLayout;
    private FloatingActionButton btnWireless;
    private Bundle mExtra = null;
    private int requestedColumnWidth;

    // Adapter
    private ExecutablesSourceBase adapterSource;
    private ExecuteAdapter adapter;

    // Groups
    private GroupPagerAdapter groupPagerAdapter;
    private UUID groupFilter = null;
    private int clickedPosition;
    private boolean disableClicks = false;
    /**
     * Called by the model data listener
     */
    private int lastResID = 0;

    public OutletsViewFragment() { }

    public static Bundle createBundleForView(int viewType) {
        Bundle bundle = new Bundle();
        bundle.putInt("viewtype", viewType);
        return bundle;
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
                    if (i < 1) i = 1;
                    getAdapter().setItemsInRow(i);
//                    SpannableGridLayoutManager spannableGridLayoutManager = new SpannableGridLayoutManager(getActivity());
//                    spannableGridLayoutManager.setNumColumns(i);
//                    spannableGridLayoutManager.setNumRows(1);
                    GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), i);
                    gridLayoutManager.setSpanSizeLookup(getAdapter().getSpanSizeLookup());
                    mRecyclerView.setHasFixedSize(false);
                    mRecyclerView.setLayoutManager(gridLayoutManager);
                    mRecyclerView.invalidate();

                }
            };

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (mRecyclerView != null) {
            mRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(mListViewNumColumnsChangeListener);
        }
    }

    @Override
    public void onRefresh() {
        this.refreshNow();
    }

    @Override
    public void onIconCacheCleared() {
        if (mRecyclerView != null)
            mRecyclerView.setAdapter(getAdapter());
    }

    public ExecuteAdapter getAdapter() {
        return adapter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        groupPagerAdapter = new GroupPagerAdapter();
        SharedPrefs.getInstance().registerHideNotReachable(this);
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        groupPagerAdapter.onDestroy();
    }

    @Override
    public void onPause() {
        ListenService.observersStartStopRefresh.unregister(this);
        ListenService.observersServiceModeChanged.unregister(this);
        super.onPause();
        if (adapterSource != null)
            adapterSource.onPause();
    }

    @Override
    public void onResume() {
        ListenService.observersStartStopRefresh.register(this);
        ListenService.observersServiceModeChanged.register(this);

        if (adapterSource != null)
            adapterSource.onResume();

        super.onResume();
    }

    public void changeArguments(Bundle extra) {
        if (mRecyclerView == null) {
            mExtra = extra;
            return;
        }

        UUID newGroupFilter;

        if (extra != null && extra.containsKey("filter"))
            newGroupFilter = UUID.fromString(extra.getString("filter"));
        else
            newGroupFilter = null;

        setCurrentGroupByIndex(AppData.getInstance().groupCollection.indexOf(newGroupFilter) + 1); // +1 because position is without "overview" entry.

        // Set view type
        int viewType;
        if (extra != null && extra.containsKey("viewtype")) {
            viewType = extra.getInt("viewtype");
        } else
            viewType = (SharedPrefs.getInstance().getOutletsViewType());
        setViewType(viewType);
    }

    private void setViewType(int viewType) {
        SharedPrefs.getInstance().setOutletsViewType(viewType);

        if (adapter == null)
            return;

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

        adapter.setEnableEditing(SharedPrefs.getInstance().isOutletEditingEnabled());
        mRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(mListViewNumColumnsChangeListener);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.outlets, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_help: {
                //noinspection ConstantConditions
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.menu_help)
                        .setMessage(R.string.help_overview_and_groups)
                        .setIcon(android.R.drawable.ic_menu_help).show();
                return true;
            }
        }
        return false;
    }

    public int getNextGroup(boolean before) {
        int pos = slidingTabLayout.getCurrentPosition();
        if (before) {
            if (pos == 0)
                return 0;
            else
                --pos;
        } else {
            if (pos == slidingTabLayout.getCount() - 1)
                return pos;
            else
                ++pos;
        }
        return pos;
    }

    private void onPlusClicked(View clickedView) {
        if (!AppData.getInstance().deviceCollection.hasDevices()) {
            MainActivity.getNavigationController().changeToDialog(getActivity(), DevicesAutomaticDialog.class.getName());
            return;
        }

        //noinspection ConstantConditions
        PopupMenu popup = new PopupMenu(getActivity(), clickedView);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.outlet_adds, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.menu_add_group:
                        GroupUtilities.createGroup(getActivity());
                        return true;
                    case R.id.menu_add_scene:
                        Intent it = new Intent(getActivity(), EditSceneActivity.class);
                        it.putExtra(EditSceneActivity.EDIT_SCENE_NOT_SHORTCUT, true);
                        startActivity(it);
                        return true;
                }
                return false;
            }
        });
        popup.show();
    }

    @Override
    public void onStart() {
        super.onStart();
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar_actionbar);
        if (toolbar != null) {
            Toolbar.LayoutParams l = new Toolbar.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

            toolbar.addView(slidingTabLayout, l);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar_actionbar);
        if (toolbar != null) {
            toolbar.removeView(slidingTabLayout);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_outlets, container, false);
        assert view != null;

        { // Add scene floating button
            final FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.btnAdd);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onPlusClicked(fab);
                }
            });
            if (!SharedPrefs.getInstance().isOutletEditingEnabled())
                fab.setVisibility(View.GONE);
            else {
                fab.setVisibility(View.INVISIBLE);
                AnimationController.animateBottomViewIn(fab);
            }
        }

        mRecyclerView = (RecyclerView) view.findViewById(android.R.id.list);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), this, null));
        LoadStoreIconData.iconCacheClearedObserver.register(this);

        final View main_layout = view.findViewById(R.id.layout);
        emptyText = (TextView) view.findViewById(R.id.empty_text);
        groupTitle = (TextView) view.findViewById(R.id.title_text);

        slidingTabLayout = new SlidingTabLayout(getActivity());
        slidingTabLayout.setCustomTabView(R.layout.tab_indicator, android.R.id.text1);
        //slidingTabLayout.setSelectedIndicatorColors(res.getColor(R.color.tab_selected_strip));
        slidingTabLayout.setDistributeEvenly(true);
        slidingTabLayout.setViewPagerAdapter(groupPagerAdapter);
        slidingTabLayout.setTabClickedListener(new SlidingTabLayout.onTabClickedListener() {
            @Override
            public void onTabClicked(int position) {
                setCurrentGroupByIndex(position);
            }
        });
        //slidingTabLayout.setId(View.generateViewId());

        { // Swiping
            final SwipeMoveAnimator.DismissCallbacks dismissCallbacks = new SwipeMoveAnimator.DismissCallbacks() {
                private Animation animation = null;

                @Override
                public void onSwipeComplete(boolean fromLeftToRight, boolean finished) {
                    mPullToRefreshLayout.setEnabled(true);
                    disableClicks = false;
                    if (finished) {
                        int position = getNextGroup(fromLeftToRight);
                        setCurrentGroupByIndex(position);
                        // Only hide the group title if there is content
                        if (adapter.getItemCount() > 0)
                            animation = AnimationController.animateViewInOutWithoutCheck(groupTitle, false, true);
                        checkEmpty();
                    } else {
                        slidingTabLayout.onPageResetScrolled();
                        animation = AnimationController.animateViewInOutWithoutCheck(groupTitle, false, true);
                    }
                }

                @Override
                public boolean onSwipeStarted(boolean fromLeftToRight) {
                    // Disable other touch events
                    mPullToRefreshLayout.setEnabled(false);
                    disableClicks = true;
                    int pos = slidingTabLayout.getCurrentPosition();

                    boolean allowSwipe = !(fromLeftToRight && pos == 0 || !fromLeftToRight && pos == slidingTabLayout.getCount() - 1);

                    if (animation != null)
                        animation.cancel();
                    animation = null;

                    if (allowSwipe) {
                        // group title position
                        groupTitle.setTranslationX(fromLeftToRight ? -groupTitle.getX() - groupTitle.getWidth() : main_layout.getWidth() - groupTitle.getX());
                        swipeMoveAnimator.setOffsetX(groupTitle.getTranslationX());
                        groupTitle.setVisibility(View.VISIBLE);
                        groupTitle.setText(groupPagerAdapter.getPageTitle(getNextGroup(fromLeftToRight)));
                    }

                    return allowSwipe;
                    //setEmptyText(R.string.empty_no_outlets);
                }

                @Override
                public void onSwipeProgress(boolean fromLeftToRight, float offset) {
                    if (slidingTabLayout != null)
                        slidingTabLayout.onPageScrolled(fromLeftToRight, offset);
                }
            };

            swipeMoveAnimator = new SwipeMoveAnimator(getActivity(), dismissCallbacks);
            swipeMoveAnimator.setSwipeView(groupTitle);
            mRecyclerView.setOnScrollListener(swipeMoveAnimator.makeScrollListener());
            mRecyclerView.setOnTouchListener(swipeMoveAnimator);
            //main_layout.setOnTouchListener(swipeMoveAnimator);

        }

        btnWireless = (FloatingActionButton) view.findViewById(R.id.btnWirelessSettings);
        if (!ListenService.isWirelessLanConnected(getActivity()))
            AnimationController.animateBottomViewIn(btnWireless);
        btnWireless.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                //intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        });

        ////////// Adapter and Adapter sources
        ExecutablesSourceChain executablesSourceChain = new ExecutablesSourceChain();

        adapterSource = new ExecutablesSourceDevicePorts(executablesSourceChain);
        adapterSource.setHideNotReachable(SharedPrefs.getInstance().isHideNotReachable());
        adapterSource.setAutomaticUpdate(true);

        ExecutablesSourceGroups groupSource = new ExecutablesSourceGroups(executablesSourceChain);
        groupSource.setHideNotReachable(SharedPrefs.getInstance().isHideNotReachable());
        groupSource.setAutomaticUpdate(true);

        ExecutablesSourceScenes sceneSource = new ExecutablesSourceScenes(executablesSourceChain);
        sceneSource.setAutomaticUpdate(true);

        adapter = new ExecuteAdapter(adapterSource, LoadStoreIconData.iconLoadingThread);
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                checkEmpty();
            }
        });
        if (groupFilter != null) {
            if (adapter.setGroupFilter(groupFilter))
                adapterSource.updateNow();
        }

        ///// For pull to refresh
        mPullToRefreshLayout = (EnhancedSwipeRefreshLayout) view.findViewById(R.id.list_layout);
        mPullToRefreshLayout.setOnRefreshListener(this);
        mPullToRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        ///// END: For pull to refresh

        mRecyclerView.setAdapter(adapter);
        checkEmpty();

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        changeArguments(mExtra);

        super.onViewCreated(view, savedInstanceState);
    }

    public void refreshNow() {
        ListenService service = ListenService.getService();
        if (service != null)
            service.findDevices(true, null);
        else {
            InAppNotifications.showException(getActivity(), null, "OutletsFragment refresh: Service is down.");
        }
    }

    public void checkEmpty() {
        if (!isAdded())
            return;

        if (adapter.getItemCount() == 0) {
            int resID;
            boolean hasDevices = AppData.getInstance().deviceCollection.hasDevices();
            if (!hasDevices) {
                resID = (R.string.empty_no_outlets_no_devices);
            } else if (groupFilter != null) {
                resID = (R.string.empty_group);
            } else {
                resID = (R.string.empty_no_outlets);
            }
            // Abort if res id is the same as before
            if (lastResID == resID)
                return;

            lastResID = resID;
            emptyText.setText(resID);
            AnimationController.animateViewInOut(emptyText, true, false);
        } else if (lastResID != 0) {
            lastResID = 0;
            AnimationController.animateViewInOut(emptyText, false, false);
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_removeGroup: {
                UUID uuid = adapter.getItem(clickedPosition).groupID();
                if (!AppData.getInstance().groupCollection.remove(uuid)) {
                    return true;
                }
                // change to overview if the removed group is the current group
                if (uuid.equals(groupFilter))
                    setCurrentGroupByIndex(0);
                return true;
            }
            case R.id.menu_renameGroup: {
                GroupUtilities.renameGroup(getActivity(), adapter.getItem(clickedPosition).groupID());
                return true;
            }
        }
        return false;
    }

    private void setCurrentGroupByIndex(int position) {
        slidingTabLayout.changeToPosition(position);

        groupFilter = position == 0 ? null : AppData.getInstance().groupCollection.get(position - 1).uuid;
        if (adapter != null) {
            if (adapter.setGroupFilter(groupFilter))
                adapterSource.updateNow();
        }
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
            InAppNotifications.updatePermanentNotification(getActivity(), new TextNotification("not_reachable",
                    getString(R.string.devices_not_reachable, devices), true));
        } else {
            InAppNotifications.closePermanentNotification(getActivity(), "not_reachable");
        }
    }

    @Override
    public boolean onItemClick(View view, int position, boolean isLongClick) {
        if (disableClicks)
            return false;

        ExecutableAdapterItem item = adapter.getItem(position);

        //////////////// GROUP ////////////////
        if (!(item.groupType() == ExecutableAdapterItem.groupTypeEnum.NOGROUP_TYPE)) {
            clickedPosition = position;
            //noinspection ConstantConditions
            PopupMenu popup = new PopupMenu(getActivity(), view);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.group, popup.getMenu());
            popup.setOnMenuItemClickListener(OutletsViewFragment.this);
            popup.show();
            return true;
        }

        //////////////// EDIT ITEM ////////////////
        if (view.getId() == R.id.icon_edit) {
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
                dialog.setDevicePort(devicePort, (ExecutableViewHolder) mRecyclerView.findViewHolderForPosition(position), adapter);
                MainActivity.getNavigationController().changeToDialog(getActivity(), dialog);
            } else if (executable instanceof Scene) {
                Intent it = new Intent(getActivity(), EditSceneActivity.class);
                it.putExtra(EditSceneActivity.EDIT_SCENE_NOT_SHORTCUT, true);
                it.putExtra(EditSceneActivity.LOAD_SCENE, (executable).toString());
                startActivity(it);
            }
            return true;
        }

        //////////////// ITEM CLICK (maybe restricted by name) ////////////////
        boolean restrict = SharedPrefs.getInstance().getSmallerClickExecuteArea();
        if (restrict && view.getId() != R.id.title)
            return true;

        // We animate the click event. This is done by calling animate on the
        // viewHolder of the current position.
        ((ExecutableViewHolder) mRecyclerView.findViewHolderForPosition(position)).animate();
        adapter.notifyItemChanged(position);

        AppData.getInstance().executeToggle(item.getExecutable(), null);
        return true;
    }

    @Override
    public void hideNotReachable(boolean hideNotReachable) {
        adapterSource.setHideNotReachable(hideNotReachable);
        adapterSource.updateNow();
    }

    @Override
    public void onServiceModeChanged(boolean isNetworkDown) {
        if (!ListenService.isWirelessLanConnected(getActivity()))
            AnimationController.animateBottomViewIn(btnWireless);
        else
            AnimationController.animateBottomViewOut(btnWireless);

        if (isNetworkDown)
            InAppNotifications.updatePermanentNotification(getActivity(), new TextNotification("energy", getString(R.string.device_energysave_mode), false));
        else
            InAppNotifications.closePermanentNotification(getActivity(), "energy");
    }

    @Override
    public void onRefreshStateChanged(boolean isRefreshing) {
        if (mPullToRefreshLayout != null)
            mPullToRefreshLayout.setRefreshing(isRefreshing);
    }


}
