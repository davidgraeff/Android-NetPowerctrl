package oly.netpowerctrl.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import oly.netpowerctrl.executables.ExecutablesEditableAdapter;
import oly.netpowerctrl.executables.FilterByReachable;
import oly.netpowerctrl.executables.FilterBySingleGroup;
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
public class OutletsFragment extends Fragment implements PopupMenu.OnMenuItemClickListener,
        onFragmentBackButton,
        onDataQueryRefreshQuery,
        SwipeRefreshLayout.OnRefreshListener, IconCacheCleared,
        RecyclerItemClickListener.OnItemClickListener, onServiceReady,
        SharedPreferences.OnSharedPreferenceChangeListener {

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
    private int requestedColumnWidth;
    private FloatingActionButton btnAdd;
    // Adapter
    private AdapterSource adapterSource;
    private ExecutablesEditableAdapter adapter;
    // Groups
    private GroupAdapter groupAdapter = new GroupAdapter();
    private FilterBySingleGroup filterBySingleGroup = new FilterBySingleGroup(null);
    private UUID clicked_group_uid;
    /**
     * Called by the model data listener
     */
    private int lastResID = 0;
    private boolean editMode;
    private AppData appData;

    public OutletsFragment() {
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

    public ExecutablesEditableAdapter getAdapter() {
        return adapter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.getDefaultSharedPreferences(App.instance).registerOnSharedPreferenceChangeListener(this);
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(App.instance).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onServiceReady(PluginService service) {
        appData = service.getAppData();
        setGroup(SharedPrefs.getInstance().getLastGroupUid(), false);
        applyViewType();
        return false;
    }

    private void applyViewType() {
        // Set view type
        int viewType = SharedPrefs.getInstance().getOutletsViewType();

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
    }

    @Override
    public void onServiceFinished(PluginService service) {
        appData = null;
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
            case R.id.menu_edit_mode: {
                setEditMode(!editMode);
                return true;
            }
            case R.id.menu_view_mode: {
                MainActivity.getNavigationController().changeToDialog(getActivity(), OutletsViewModeDialog.class.getName());
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
        popup.getMenu().findItem(R.id.menu_add_items_to_group).setVisible(filterBySingleGroup.getFilterGroup() != null);
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
                            public void onGroupCreated(int group_index, UUID group_uid) {
                                setGroup(group_uid, true);
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
    public void onDetach() {
        menu.setMode(SlidingMenu.LEFT);
        menu.setSecondaryMenu(null);
        super.onDetach();
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
        menu = (SlidingMenu) getActivity().findViewById(R.id.slidingmenulayout);
        menu.setMode(SlidingMenu.LEFT_RIGHT);
        menu.setSecondaryMenu(R.layout.group_list);
        menu.setSecondaryShadowDrawable(R.drawable.shadowright);
        menu.setMenu(menu.getMenu());

        {
            RecyclerView group_list = (RecyclerView) getActivity().findViewById(R.id.group_list);
            group_list.setItemAnimator(new DefaultItemAnimator());
            group_list.setLayoutManager(new LinearLayoutManager(getActivity()));
            group_list.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), new RecyclerItemClickListener.OnItemClickListener() {
                @Override
                public boolean onItemClick(View view, int position, boolean isLongClick) {
                    if (isLongClick && position > 0) {
                        showGroupPopupMenu(view, appData.groupCollection.get(position - 1).uuid);
                        return true;
                    }
                    setGroup(position > 0 ? appData.groupCollection.get(position - 1).uuid : null, true);
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
        adapterSource.addFilter(new FilterByReachable(SharedPrefs.getInstance().isHideNotReachable()));
        adapterSource.addFilter(filterBySingleGroup);
        adapterSource.addInput(new AdapterSourceInputDevicePorts(),
                new AdapterSourceInputGroups(), new AdapterSourceInputScenes());

        adapter = new ExecutablesEditableAdapter(adapterSource, LoadStoreIconData.iconLoadingThread);

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                checkEmpty();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                checkEmpty();
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
            setEditMode(savedInstanceState.getBoolean("editMode"));
        } else
            setEditMode(false);

        mRecyclerView.scrollToPosition(SharedPrefs.getInstance().getLastScrollIndex());

        LoadStoreIconData.iconCacheClearedObserver.register(this);
        AppData.observersStartStopRefresh.register(this);
        PluginService.observersServiceReady.register(this); // Will call onServiceReady and setup current group
    }

    @Override
    public void onDestroyView() {
        View view = mRecyclerView.findChildViewUnder(10, 10);
        if (view != null)
            SharedPrefs.getInstance().setLastScrollIndex(mRecyclerView.getChildPosition(view));
        PluginService.observersServiceReady.unregister(this);
        LoadStoreIconData.iconCacheClearedObserver.unregister(this);
        AppData.observersStartStopRefresh.unregister(this);
        super.onDestroyView();
    }

    public void refreshNow() {
        appData.refreshDeviceData(PluginService.getService(), false);
    }

    public void checkEmpty() {
        if (adapter.getItemCount() == 0) {
            int resID;
            boolean hasDevices = appData != null && appData.deviceCollection.hasDevices();
            if (!hasDevices) {
                resID = (R.string.empty_no_outlets_no_devices);
            } else if (filterBySingleGroup.getFilterGroup() != null) {
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
                if (clicked_group_uid.equals(filterBySingleGroup.getFilterGroup())) {
                    setGroup(null, true);
                }
                return true;
            }
            case R.id.menu_renameGroup: {
                GroupUtilities.renameGroup(getActivity(), appData.groupCollection, clicked_group_uid);
                return true;
            }
        }
        return false;
    }

    private void setGroup(UUID group_uid, boolean save) {
        filterBySingleGroup.setFilterGroup(group_uid);
        if (save) SharedPrefs.getInstance().setLastGroupUid(group_uid);
        groupAdapter.setSelectedItem(appData.groupCollection.indexOf(group_uid) + 1);
    }

    @Override
    public boolean onItemClick(View view, final int position, boolean isLongClick) {
//        boolean disableClicks = false;
//        if (disableClicks)
//            return false;

        ExecutableAdapterItem item = adapterSource.getItem(position);

        //////////////// GROUP ////////////////
        if (!(item.groupType() == ExecutableAdapterItem.groupTypeEnum.NOGROUP_TYPE)) {
            showGroupPopupMenu(view, item.groupID());
            return true;
        }

        //////////////// EDIT ITEM ////////////////
        if (editMode) {
            Executable executable = adapterSource.getItem(position).getExecutable();
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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        switch (s) {
            case SharedPrefs.PREF_OutletsViewType: {
                applyViewType();
                break;
            }
        }
    }


}
