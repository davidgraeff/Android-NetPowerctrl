package oly.netpowerctrl.outletsview;

import android.app.Activity;
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
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
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
import oly.netpowerctrl.data.onDataQueryRefreshQuery;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.device_base.executables.Executable;
import oly.netpowerctrl.executables.ExecutableAdapterItem;
import oly.netpowerctrl.executables.ExecutableViewHolder;
import oly.netpowerctrl.executables.ExecutablesSourceBase;
import oly.netpowerctrl.executables.ExecutablesSourceChain;
import oly.netpowerctrl.executables.ExecutablesSourceDevicePorts;
import oly.netpowerctrl.executables.ExecutablesSourceGroups;
import oly.netpowerctrl.executables.ExecutablesSourceScenes;
import oly.netpowerctrl.executables.ExecuteAdapter;
import oly.netpowerctrl.groups.GroupUtilities;
import oly.netpowerctrl.main.IntroductionFragment;
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.network.onNotReachableUpdate;
import oly.netpowerctrl.pluginservice.PluginService;
import oly.netpowerctrl.pluginservice.onServiceModeChanged;
import oly.netpowerctrl.scenes.EditSceneActivity;
import oly.netpowerctrl.scenes.Scene;
import oly.netpowerctrl.scenes.SceneFactory;
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
        onDataQueryRefreshQuery, SharedPrefs.IHideNotReachable, onServiceModeChanged,
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
    private UUID clicked_group_uid;
    private boolean disableClicks = false;
    private int lastScrolledPosition = 0;
    /**
     * Called by the model data listener
     */
    private int lastResID = 0;

    public OutletsViewFragment() {
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (groupFilter != null)
            outState.putString("groupFilter", groupFilter.toString());
        if (mRecyclerView != null) {
            View view = mRecyclerView.findChildViewUnder(10, 10);
            if (view != null)
                outState.putInt("lastScrolledPosition", mRecyclerView.getChildPosition(view));
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            String groupFilterString = savedInstanceState.getString("groupFilter");
            if (groupFilterString != null)
                groupFilter = UUID.fromString(groupFilterString);
            lastScrolledPosition = savedInstanceState.getInt("lastScrolledPosition", 0);
        }
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
        AppData.observersStartStopRefresh.unregister(this);
        PluginService.observersServiceModeChanged.unregister(this);
        super.onPause();
        if (adapterSource != null)
            adapterSource.onPause();
    }

    private final ViewTreeObserver.OnGlobalLayoutListener mListViewNumColumnsChangeListener =
            new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    //noinspection deprecation
                    mRecyclerView.getViewTreeObserver().removeGlobalOnLayoutListener(mListViewNumColumnsChangeListener);

                    int i = mRecyclerView.getWidth() / requestedColumnWidth;
                    if (i < 1) i = 1;
                    adapter.setItemsInRow(i);
                    GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), i);
                    gridLayoutManager.setSpanSizeLookup(getAdapter().getSpanSizeLookup());
                    mRecyclerView.setHasFixedSize(false);
                    mRecyclerView.setLayoutManager(gridLayoutManager);
                    mRecyclerView.setAdapter(adapter);
                    if (lastScrolledPosition != 0)
                        mRecyclerView.scrollToPosition(lastScrolledPosition);
                }
            };

    @Override
    public void onResume() {
        AppData.observersStartStopRefresh.register(this);
        PluginService.observersServiceModeChanged.register(this);

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
                adapter.setLayoutRes(R.layout.grid_item_compact_executable);
                requestedColumnWidth = (int) getResources().getDimension(R.dimen.min_grid_item_width);
                break;
            case VIEW_AS_GRID:
                adapter.setLayoutRes(R.layout.grid_item_executable);
                requestedColumnWidth = (int) getResources().getDimension(R.dimen.min_grid_item_width);
                break;
            case VIEW_AS_LIST:
            default:
                adapter.setLayoutRes(R.layout.list_item_executable);
                requestedColumnWidth = (int) getResources().getDimension(R.dimen.min_list_item_width);
                break;
        }

        adapter.setEnableEditing(SharedPrefs.getInstance().isOutletEditingEnabled());
        mRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(mListViewNumColumnsChangeListener);
        mRecyclerView.requestLayout();
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

    /**
     * Handles clicks on the plus floating button
     *
     * @param clickedView The clicked view
     */
    private void onPlusClicked(View clickedView) {
        if (!AppData.getInstance().deviceCollection.hasDevices()) {
            MainActivity.getNavigationController().changeToFragment(IntroductionFragment.class.getName());
            return;
        }

        //noinspection ConstantConditions
        PopupMenu popup = new PopupMenu(getActivity(), clickedView);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.outlet_adds, popup.getMenu());
        popup.getMenu().findItem(R.id.menu_add_items_to_group).setVisible(groupFilter != null);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.menu_add_items_to_group:
                        //noinspection ConstantConditions
                        new AlertDialog.Builder(getActivity())
                                .setTitle(R.string.menu_help)
                                .setMessage(R.string.help_groups)
                                .setIcon(android.R.drawable.ic_menu_help).show();
                        return true;
                    case R.id.menu_add_group:
                        GroupUtilities.createGroup(getActivity(), new GroupUtilities.GroupCreatedCallback() {
                            @Override
                            public void onGroupCreated(int group_index) {
                                setCurrentGroupByIndex(group_index + 1);
                            }
                        });
                        return true;
                    case R.id.menu_add_scene:
                        Intent it = new Intent(getActivity(), EditSceneActivity.class);
                        it.putExtra(EditSceneActivity.EDIT_SCENE_NOT_SHORTCUT, true);
                        startActivityForResult(it, EditSceneActivity.REQUEST_CODE);
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
            fab.setHideOnLongClick(2000);
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

            @Override
            public void onTabLongClicked(View view, int position) {
                if (position == 0) return;
                showGroupPopupMenu(view, AppData.getInstance().groupCollection.get(position - 1).uuid);
            }
        });
        //slidingTabLayout.setId(View.generateViewId());

        { // Swiping
            final SwipeMoveAnimator.DismissCallbacks dismissCallbacks = new SwipeMoveAnimator.DismissCallbacks() {
                private ViewPropertyAnimator animation = null;
                private boolean allowSwipe;
                private int targetPosition;

                @Override
                public void onSwipeComplete(boolean fromLeftToRight, boolean finished) {
                    mPullToRefreshLayout.setEnabled(true);
                    disableClicks = false;
                    if (finished) {
                        setCurrentGroupByIndex(targetPosition);
                        // Only hide the group title if there is content
                        //checkEmpty();
                    } else {
                        slidingTabLayout.onPageResetScrolled();
                    }
                    if (adapter.getItemCount() > 0)
                        animation = AnimationController.animateViewInOutWithoutCheck(groupTitle, false, true, 1200);
                }

                @Override
                public boolean onSwipeStarted(boolean fromLeftToRight) {
                    // Disable other touch events
                    mPullToRefreshLayout.setEnabled(false);
                    disableClicks = true;
                    int pos = slidingTabLayout.getCurrentPosition();

                    allowSwipe = !(fromLeftToRight && pos <= 0 || !fromLeftToRight && pos >= AppData.getInstance().groupCollection.length());

                    if (animation != null)
                        animation.cancel();
                    animation = null;

                    if (allowSwipe) {
                        // group title position
                        groupTitle.setTranslationX(fromLeftToRight ? -groupTitle.getX() - groupTitle.getWidth() : main_layout.getWidth() - groupTitle.getX());
                        emptyText.setTranslationX(fromLeftToRight ? -emptyText.getX() - emptyText.getWidth() : main_layout.getWidth() - emptyText.getX());
                        swipeMoveAnimator.setOffsetX(groupTitle.getTranslationX());
                        groupTitle.setVisibility(View.VISIBLE);
                        targetPosition = getNextGroup(fromLeftToRight);
                        UUID next_groupFilter = targetPosition == 0 ? null : AppData.getInstance().groupCollection.get(targetPosition - 1).uuid;
                        checkEmpty(adapterSource.countIfGroup(next_groupFilter), next_groupFilter);
                        groupTitle.setText(groupPagerAdapter.getPageTitle(targetPosition));
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
            swipeMoveAnimator.setSwipeView(groupTitle, emptyText);
            mRecyclerView.setOnScrollListener(swipeMoveAnimator.makeScrollListener());
            mRecyclerView.setOnTouchListener(swipeMoveAnimator);

        }

        btnWireless = (FloatingActionButton) view.findViewById(R.id.btnWirelessSettings);
        if (!PluginService.isWirelessLanConnected(getActivity()))
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
        ExecutablesSourceGroups groupSource = new ExecutablesSourceGroups(executablesSourceChain);
        ExecutablesSourceScenes sceneSource = new ExecutablesSourceScenes(executablesSourceChain);

        adapter = new ExecuteAdapter(adapterSource, LoadStoreIconData.iconLoadingThread);
        adapter.setGroupFilter(groupFilter);

        groupSource.setTargetAdapter(adapter);
        sceneSource.setTargetAdapter(adapter);
        adapterSource.setHideNotReachable(SharedPrefs.getInstance().isHideNotReachable());
        adapterSource.setTargetAdapter(adapter);
        adapterSource.setAutomaticUpdate(true);

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                checkEmpty(adapter.getItemCount(), groupFilter);
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                checkEmpty(adapter.getItemCount(), groupFilter);
            }
        });

        adapterSource.updateNow();

        ///// For pull to refresh
        mPullToRefreshLayout = (EnhancedSwipeRefreshLayout) view.findViewById(R.id.list_layout);
        mPullToRefreshLayout.setOnRefreshListener(this);
        mPullToRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        ///// END: For pull to refresh

        mRecyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        changeArguments(mExtra);

        super.onViewCreated(view, savedInstanceState);
    }

    public void refreshNow() {
        AppData.getInstance().refreshDeviceData(false);
    }

    public void checkEmpty(int count, UUID groupFilter) {
        if (count == 0) {
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
            emptyText.setVisibility(View.VISIBLE);
        } else if (lastResID != 0) {
            lastResID = 0;
            emptyText.setVisibility(View.GONE);
        }
    }

    /**
     * Handles the group menu popup clicks (remove, rename)
     *
     * @param menuItem The clicked menu item
     * @return Return true if handled.
     */
    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        if (clicked_group_uid == null) return false;

        switch (menuItem.getItemId()) {
            case R.id.menu_removeGroup: {
                if (!AppData.getInstance().groupCollection.remove(clicked_group_uid)) {
                    return true;
                }
                // change to overview if the removed group is the current group
                if (clicked_group_uid.equals(groupFilter))
                    setCurrentGroupByIndex(0);
                return true;
            }
            case R.id.menu_renameGroup: {
                GroupUtilities.renameGroup(getActivity(), clicked_group_uid);
                return true;
            }
        }
        return false;
    }

    /**
     * Change to the given group by position. Position 0 is reserved for the overview
     * so add 1 to a group_index to get the position.
     *
     * @param position The group_index+1 or 0 for the overview.
     */
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
                devices += di.getDeviceName() + " ";
            }
            InAppNotifications.updatePermanentNotification(getActivity(), new TextNotification("not_reachable",
                    getString(R.string.devices_not_reachable, devices), true));
        } else {
            InAppNotifications.closePermanentNotification(getActivity(), "not_reachable");
        }
    }

    @Override
    public boolean onItemClick(View view, final int position, boolean isLongClick) {
        if (disableClicks)
            return false;

        ExecutableAdapterItem item = adapter.getItem(position);

        //////////////// GROUP ////////////////
        if (!(item.groupType() == ExecutableAdapterItem.groupTypeEnum.NOGROUP_TYPE)) {
            showGroupPopupMenu(view, item.groupID());
            return true;
        }

        //////////////// EDIT ITEM ////////////////
        if (view.getId() == R.id.icon_edit) {
            // Animate press
            Animation a = AnimationController.animatePress(view);

            final Executable executable = adapter.getItem(position).getExecutable();
            a.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (executable instanceof DevicePort) {
                        DevicePort devicePort = (DevicePort) executable;

                        OutletEditFragment dialog = (OutletEditFragment) Fragment.instantiate(getActivity(), OutletEditFragment.class.getName());
                        dialog.setDevicePort(devicePort, (ExecutableViewHolder) mRecyclerView.findViewHolderForPosition(position), adapter);
                        MainActivity.getNavigationController().changeToFragment(dialog, OutletEditFragment.class.getName());
                    } else if (executable instanceof Scene) {
                        Scene scene = ((Scene) executable);
                        if (!AppData.getInstance().sceneCollection.getItems().contains(scene)) {
                            throw new RuntimeException("Scene not in sceneCollection!");
                        }

                        Intent it = new Intent(getActivity(), EditSceneActivity.class);
                        it.putExtra(EditSceneActivity.EDIT_SCENE_NOT_SHORTCUT, true);
                        it.putExtra(EditSceneActivity.LOAD_SCENE, scene.toString());
                        startActivityForResult(it, EditSceneActivity.REQUEST_CODE);
                    }
                }
            });

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

    private void showGroupPopupMenu(View view, UUID group_uid) {
        this.clicked_group_uid = group_uid;
        //noinspection ConstantConditions
        PopupMenu popup = new PopupMenu(getActivity(), view);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.group, popup.getMenu());
        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

    @Override
    public void hideNotReachable(boolean hideNotReachable) {
        adapterSource.setHideNotReachable(hideNotReachable);
        adapterSource.updateNow();
    }

    @Override
    public void onServiceModeChanged(boolean isNetworkDown) {
        if (!PluginService.isWirelessLanConnected(getActivity()))
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Receive added/updated/deleted scene from scene activity
        if (requestCode == EditSceneActivity.REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            SceneFactory.createSceneFromActivityIntent(getActivity(), data);
        } else
            super.onActivityResult(requestCode, resultCode, data);
    }


}
