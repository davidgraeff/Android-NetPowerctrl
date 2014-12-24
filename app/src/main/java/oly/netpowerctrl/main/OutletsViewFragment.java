package oly.netpowerctrl.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.widget.SwipeRefreshLayout;
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
import android.widget.PopupMenu;
import android.widget.TextView;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.IconCacheCleared;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.data.onDataQueryRefreshQuery;
import oly.netpowerctrl.device_base.executables.Executable;
import oly.netpowerctrl.executables.AdapterSource;
import oly.netpowerctrl.executables.AdapterSourceInputDevicePorts;
import oly.netpowerctrl.executables.AdapterSourceInputGroups;
import oly.netpowerctrl.executables.AdapterSourceInputScenes;
import oly.netpowerctrl.executables.ExecutableAdapterItem;
import oly.netpowerctrl.executables.ExecutableViewHolder;
import oly.netpowerctrl.executables.ExecuteAdapter;
import oly.netpowerctrl.groups.GroupAdapter;
import oly.netpowerctrl.groups.GroupUtilities;
import oly.netpowerctrl.pluginservice.PluginService;
import oly.netpowerctrl.pluginservice.onServiceReady;
import oly.netpowerctrl.ui.RecyclerItemClickListener;
import oly.netpowerctrl.ui.widgets.FloatingActionButton;
import oly.netpowerctrl.utils.AnimationController;
import oly.netpowerctrl.utils.DividerItemDecoration;
import oly.netpowerctrl.utils.fragments.onFragmentBackButton;

/**
 * This fragment consists of some floating buttons (add + no_wireless) and a view pager. Within the
 * fragments in the view pager is a RecycleView and a placeholder text. The computation for
 */
public class OutletsViewFragment extends Fragment implements PopupMenu.OnMenuItemClickListener,
        onFragmentBackButton,
        onDataQueryRefreshQuery,
        SwipeRefreshLayout.OnRefreshListener, IconCacheCleared, RecyclerItemClickListener.OnItemClickListener, onServiceReady {

    // Column computations
    public static final int VIEW_AS_LIST = 0;
    public static final int VIEW_AS_GRID = 1;
    public static final int VIEW_AS_COMPACT = 2;
    SlidingMenu menu;
    private RecyclerView mRecyclerView;
    private TextView emptyText;
    private View edit_hint_big;
    private SwipeRefreshLayout mPullToRefreshLayout;
    private FloatingActionButton btnWireless;
    private Bundle mExtra = null;
    private int requestedColumnWidth;
    private FloatingActionButton btnAdd;
    // Adapter
    private AdapterSource adapterSource;
    private ExecuteAdapter adapter;
    // Groups
    private GroupAdapter groupAdapter = new GroupAdapter();
    private UUID groupFilter = null;
    private UUID clicked_group_uid;
    private int lastScrolledPosition = 0;
    /**
     * Called by the model data listener
     */
    private int lastResID = 0;
    private boolean editMode;
    private AppData appData;

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
        setHasOptionsMenu(true);
    }

    @Override
    public boolean onServiceReady(PluginService service) {
        appData = service.getAppData();

        UUID newGroupFilter;

        if (mExtra != null && mExtra.containsKey("filter"))
            newGroupFilter = UUID.fromString(mExtra.getString("filter"));
        else
            newGroupFilter = null;

        setCurrentGroupByIndex(appData.groupCollection.indexOf(newGroupFilter) + 1); // +1 because position is without "overview" entry.

        // Set view type
        int viewType;
        if (mExtra != null && mExtra.containsKey("viewtype")) {
            viewType = mExtra.getInt("viewtype");
        } else
            viewType = (SharedPrefs.getInstance().getOutletsViewType());

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

        mRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(mListViewNumColumnsChangeListener);
        mRecyclerView.requestLayout();
        mExtra = null;

        return false;
    }

    @Override
    public void onServiceFinished(PluginService service) {
        appData = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.outlets, menu);
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
                }
            };

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
            case R.id.menu_edit_mode: {
                setEditMode(!editMode);
                return true;
            }
        }
        return false;
    }

    /**
     * Handles clicks on the plus floating button
     *
     * @param clickedView The clicked view
     */
    private void onPlusClicked(View clickedView) {
        if (!appData.deviceCollection.hasDevices()) {
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
                        GroupUtilities.createGroup(getActivity(), appData.groupCollection, new GroupUtilities.GroupCreatedCallback() {
                            @Override
                            public void onGroupCreated(int group_index) {
                                setCurrentGroupByIndex(group_index + 1);
                            }
                        });
                        return true;
                    case R.id.menu_add_scene:
                        Intent it = new Intent(getActivity(), EditActivity.class);
                        it.putExtra(EditActivity.CREATE_SCENE, true);
                        startActivityForResult(it, EditActivity.REQUEST_CODE);
                        return true;
                }
                return false;
            }
        });
        popup.show();
    }

    @Override
    public boolean onBackButton() {
        if (menu.isMenuShowing()) {
            menu.showContent();
            return true;
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
        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), this, null));

        emptyText = (TextView) view.findViewById(R.id.empty_text);

        // configure the SlidingMenu
        menu = (SlidingMenu) view.findViewById(R.id.slidingmenulayout);
        menu.setBehindCanvasTransformer(new SlidingMenu.CanvasTransformer() {
            @Override
            public void transformCanvas(Canvas canvas, float percentOpen) {
                canvas.scale(percentOpen, 1, 0, 0);
            }
        });
        {
            RecyclerView group_list = (RecyclerView) view.findViewById(R.id.group_list);
            group_list.setItemAnimator(new DefaultItemAnimator());
            group_list.setLayoutManager(new LinearLayoutManager(getActivity()));
            group_list.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), new RecyclerItemClickListener.OnItemClickListener() {
                @Override
                public boolean onItemClick(View view, int position, boolean isLongClick) {
                    if (isLongClick && position > 0) {
                        showGroupPopupMenu(view, appData.groupCollection.get(position - 1).uuid);
                        return true;
                    }
                    setCurrentGroupByIndex(position);
                    return true;
                }
            }, null));
            group_list.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST) {
                @Override
                public boolean dividerForPosition(int position) {
                    return true;
                }
            });
            group_list.setAdapter(groupAdapter);
            //menu.setMenu(group_list);
        }

        btnWireless = (FloatingActionButton) view.findViewById(R.id.btnWirelessSettings);
        if (!PluginService.isWirelessLanConnected(getActivity()))
            AnimationController.animateBottomViewIn(btnWireless, false);
        btnWireless.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                //intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        });

        ////////// Adapter and Adapter sources
        adapterSource = new AdapterSource(AdapterSource.AutoStartEnum.AutoStartAfterFirstQuery);
        adapterSource.setHideNotReachable(SharedPrefs.getInstance().isHideNotReachable());
        adapterSource.add(new AdapterSourceInputDevicePorts(),
                new AdapterSourceInputGroups(), new AdapterSourceInputScenes());

        adapter = new ExecuteAdapter(adapterSource, LoadStoreIconData.iconLoadingThread);
        adapter.setGroupFilter(groupFilter);

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

        ///// For pull to refresh
        mPullToRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.ptr_layout);
        mPullToRefreshLayout.setOnRefreshListener(this);
        mPullToRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        ///// END: For pull to refresh

        mRecyclerView.setAdapter(adapter);

        // Edit Mode
        edit_hint_big = view.findViewById(R.id.edit_hint_big);
        edit_hint_big.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                setEditMode(false);
            }
        });
        btnAdd = (FloatingActionButton) view.findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPlusClicked(btnAdd);
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            String groupFilterString = savedInstanceState.getString("groupFilter");
            if (groupFilterString != null)
                groupFilter = UUID.fromString(groupFilterString);
            lastScrolledPosition = savedInstanceState.getInt("lastScrolledPosition", 0);
            mRecyclerView.scrollToPosition(lastScrolledPosition);
            setEditMode(savedInstanceState.getBoolean("editMode"));
        } else
            setEditMode(false);

        LoadStoreIconData.iconCacheClearedObserver.register(this);
        AppData.observersStartStopRefresh.register(this);
        PluginService.observersServiceReady.register(this); // Will call onServiceReady and setup current group
    }

    @Override
    public void onDestroyView() {
        PluginService.observersServiceReady.unregister(this);
        LoadStoreIconData.iconCacheClearedObserver.unregister(this);
        AppData.observersStartStopRefresh.unregister(this);
        super.onDestroyView();
    }

    public void refreshNow() {
        appData.refreshDeviceData(PluginService.getService(), false);
    }

    public void checkEmpty(int count, UUID groupFilter) {
        if (count == 0) {
            int resID;
            boolean hasDevices = appData != null && appData.deviceCollection.hasDevices();
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
                if (!appData.groupCollection.remove(clicked_group_uid)) {
                    return true;
                }
                // change to overview if the removed group is the current group
                if (clicked_group_uid.equals(groupFilter))
                    setCurrentGroupByIndex(0);
                return true;
            }
            case R.id.menu_renameGroup: {
                GroupUtilities.renameGroup(getActivity(), appData.groupCollection, clicked_group_uid);
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
        //TODO update group list active item

        groupFilter = position == 0 ? null : appData.groupCollection.get(position - 1).uuid;
        if (adapter != null) {
            if (adapter.setGroupFilter(groupFilter) || groupFilter == null)
                adapterSource.updateNow();
        }
    }

    @Override
    public boolean onItemClick(View view, final int position, boolean isLongClick) {
//        boolean disableClicks = false;
//        if (disableClicks)
//            return false;

        ExecutableAdapterItem item = adapter.getItem(position);

        //////////////// GROUP ////////////////
        if (!(item.groupType() == ExecutableAdapterItem.groupTypeEnum.NOGROUP_TYPE)) {
            showGroupPopupMenu(view, item.groupID());
            return true;
        }

        //////////////// EDIT ITEM ////////////////
        if (editMode) {
            Executable executable = adapter.getItem(position).getExecutable();
            Intent it = new Intent(getActivity(), EditActivity.class);
            it.putExtra(EditActivity.LOAD_UUID, executable.getUid());
            it.putExtra(EditActivity.LOAD_ADAPTER_POSITION, position);
            startActivityForResult(it, EditActivity.REQUEST_CODE);

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

        appData.executeToggle(item.getExecutable(), null);
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
    public void onRefreshStateChanged(boolean isRefreshing) {
        if (mPullToRefreshLayout != null)
            mPullToRefreshLayout.setRefreshing(isRefreshing);

        if (!PluginService.isWirelessLanConnected(getActivity()))
            AnimationController.animateBottomViewIn(btnWireless, false);
        else
            AnimationController.animateBottomViewOut(btnWireless);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Receive added/updated/deleted scene from scene activity
        if (requestCode == EditActivity.REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                int position = data.getIntExtra(EditActivity.LOAD_ADAPTER_POSITION, -1);
                if (position == -1) {
                    adapter.notifyDataSetChanged();
                    return;
                }
                //ExecutableViewHolder executableViewHolder = (ExecutableViewHolder) mRecyclerView.findViewHolderForPosition(position);
                //if (executableViewHolder != null) executableViewHolder.reload();
                //updatePositionAfterResume = position; adapter.notifyItemChanged(updatePositionAfterResume);
            }
        } else
            super.onActivityResult(requestCode, resultCode, data);
    }

    private void setEditMode(boolean editMode) {
        this.editMode = editMode;
        if (editMode) {
            AnimationController.animateBottomViewIn(edit_hint_big, edit_hint_big.getVisibility() == View.VISIBLE);
            AnimationController.animateBottomViewIn(btnAdd, false);
        } else {
            AnimationController.animateBottomViewOut(edit_hint_big);
            AnimationController.animateBottomViewOut(btnAdd);
        }
        adapter.setEditMode(editMode);
    }


}
