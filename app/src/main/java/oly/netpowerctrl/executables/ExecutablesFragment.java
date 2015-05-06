package oly.netpowerctrl.executables;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Build;
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
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.data.graphic.IconCacheCleared;
import oly.netpowerctrl.data.graphic.LoadStoreIconData;
import oly.netpowerctrl.data.onServiceReady;
import oly.netpowerctrl.data.query.onDataQueryRefreshQuery;
import oly.netpowerctrl.devices.AutomaticSetup;
import oly.netpowerctrl.executables.adapter.AdapterSource;
import oly.netpowerctrl.executables.adapter.ExecutableAdapterItem;
import oly.netpowerctrl.executables.adapter.ExecutableViewHolder;
import oly.netpowerctrl.executables.adapter.ExecutablesEditableAdapter;
import oly.netpowerctrl.executables.adapter.FilterByHidden;
import oly.netpowerctrl.executables.adapter.FilterByReachable;
import oly.netpowerctrl.executables.adapter.FilterBySingleGroup;
import oly.netpowerctrl.executables.adapter.InputExecutables;
import oly.netpowerctrl.executables.adapter.InputGroupChanges;
import oly.netpowerctrl.groups.GroupListFragment;
import oly.netpowerctrl.groups.GroupUtilities;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.main.EditActivity;
import oly.netpowerctrl.network.Utils;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.ui.EmptyListener;
import oly.netpowerctrl.ui.FragmentUtils;
import oly.netpowerctrl.ui.ItemShadowDecoration;
import oly.netpowerctrl.ui.RecyclerItemClickListener;
import oly.netpowerctrl.ui.SimpleListDividerDecoration;
import oly.netpowerctrl.utils.AnimationController;

;

/**
 * This fragment consists of some floating buttons (add + no_wireless) and a view pager. Within the
 * fragments in the view pager is a RecycleView and a placeholder text. The computation for
 */
public class ExecutablesFragment extends Fragment implements PopupMenu.OnMenuItemClickListener,
        onDataQueryRefreshQuery,
        SwipeRefreshLayout.OnRefreshListener, IconCacheCleared,
        RecyclerItemClickListener.OnItemClickListener, onServiceReady,
        SharedPreferences.OnSharedPreferenceChangeListener, EmptyListener {

    // Column computations
    public static final int VIEW_AS_LIST = 0;
    public static final int VIEW_AS_GRID = 1;
    public static final int VIEW_AS_COMPACT = 2;
    // Groups
    FilterBySingleGroup filterBySingleGroup = new FilterBySingleGroup(null);
    private RecyclerView mRecyclerView;
    private View edit_hint_big;
    private SwipeRefreshLayout mPullToRefreshLayout;
    private View btnWireless;
    private int requestedColumnWidth;
    // Adapter
    private AdapterSource adapterSource;
    private ExecutablesEditableAdapter adapter;
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
    private String clicked_group_uid;
    // UI
    private boolean editMode;
    private SimpleListDividerDecoration listDividerDecoration;
    private View btnAdd;
    // Data
    private DataService dataService;
    private AutomaticSetup automaticSetup;

    public ExecutablesFragment() {
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
        dataService.refreshExistingDevices();
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
        automaticSetup.finish();
        PreferenceManager.getDefaultSharedPreferences(App.instance).unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * Called as soon as the plugin service is ready. We will store a reference to DataService here, setup the current
     * visible group and apply the right view type.
     */
    @Override
    public boolean onServiceReady(DataService service) {
        dataService = service;
        setGroup(SharedPrefs.getInstance().getLastGroupUid(), false);
        applyViewType();
        onEmptyListener(adapterSource.mItems.isEmpty());
        getActivity().invalidateOptionsMenu();
        return false;
    }

    private void applyViewType() {
        // Set view type
        int viewType = SharedPrefs.getInstance().getOutletsViewType();

        mRecyclerView.removeItemDecoration(listDividerDecoration);

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
                mRecyclerView.addItemDecoration(listDividerDecoration);
                adapter.setLayoutRes(R.layout.list_item_executable);
                requestedColumnWidth = (int) getResources().getDimension(R.dimen.min_list_item_width);
                break;
        }

        mRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(mListViewNumColumnsChangeListener);
        mRecyclerView.requestLayout();
    }

    @Override
    public void onServiceFinished(DataService service) {
        dataService = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.outlets, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean hasDevices = false;
        if (dataService != null) {
            hasDevices = dataService.credentials.countConfigured() > 0;
        }
        menu.findItem(R.id.menu_edit_mode).setVisible(hasDevices);
        menu.findItem(R.id.menu_view_mode).setVisible(hasDevices);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_edit_mode: {
                setEditMode(!editMode);
                return true;
            }
            case R.id.menu_view_mode: {
                FragmentUtils.changeToDialog(getActivity(), ExecutablesViewModeDialog.class.getName());
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mRecyclerView.addItemDecoration(new ItemShadowDecoration((NinePatchDrawable) getResources().getDrawable(R.drawable.material_shadow_z1)));
        }
        listDividerDecoration = new SimpleListDividerDecoration(getResources().getDrawable(R.drawable.list_divider), true);
        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), this, null));

        automaticSetup = new AutomaticSetup(
                (Button) view.findViewById(R.id.automatic_setup_start),
                (TextView) view.findViewById(R.id.automatic_status),
                view.findViewById(R.id.empty_no_outlets_no_devices_text));

        btnWireless = view.findViewById(R.id.btnWirelessSettings);
        if (!Utils.isWirelessLanConnected(getActivity()))
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
        adapterSource.addFilter(new FilterByHidden(true));
        adapterSource.addFilter(filterBySingleGroup);
        adapterSource.addInput(new InputExecutables(), new InputGroupChanges());

        adapter = new ExecutablesEditableAdapter(adapterSource, LoadStoreIconData.iconLoadingThread);

        adapter.getSource().setEmptyListener(this);

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

        btnAdd = view.findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent it = new Intent(getActivity(), EditActivity.class);
                it.putExtra(EditActivity.CREATE_SCENE, true);
                startActivityForResult(it, EditActivity.REQUEST_CODE);
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
        DataService.observersStartStopRefresh.register(this);
        DataService.observersServiceReady.register(this); // Will call onServiceReady and setup current group
    }

    @Override
    public void onDestroyView() {
        View view = mRecyclerView.findChildViewUnder(10, 10);
        if (view != null)
            SharedPrefs.getInstance().setLastScrollIndex(mRecyclerView.getChildPosition(view));
        DataService.observersServiceReady.unregister(this);
        LoadStoreIconData.iconCacheClearedObserver.unregister(this);
        DataService.observersStartStopRefresh.unregister(this);
        super.onDestroyView();
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
                if (!dataService.groups.remove(clicked_group_uid)) {
                    return true;
                }
                // change to overview if the removed group is the current group
                if (clicked_group_uid.equals(filterBySingleGroup.getFilterGroup())) {
                    setGroup(null, true);
                }
                return true;
            }
            case R.id.menu_renameGroup: {
                GroupUtilities.renameGroup(getActivity(), dataService.groups, clicked_group_uid);
                return true;
            }
        }
        return false;
    }

    public void setGroup(String groupUid, boolean save) {
        filterBySingleGroup.setFilterGroup(groupUid);
        if (save) SharedPrefs.getInstance().setLastGroupUID(groupUid);
        GroupListFragment groupListFragment = (GroupListFragment) getFragmentManager().findFragmentByTag("group");
        if (groupListFragment != null)
            groupListFragment.getAdapter().setSelectedItem(groupUid);
    }

    @Override
    public boolean onItemClick(View view, final int position, boolean isLongClick) {
        ExecutableAdapterItem item = adapterSource.getItem(position);

        //////////////// GROUP ////////////////
        if (!(item.groupType() == ExecutableAdapterItem.groupTypeEnum.NOGROUP_TYPE)) {
            showGroupPopupMenu(view, item.groupUID());
            return true;
        }

        //////////////// EDIT ITEM ////////////////
        if (editMode || isLongClick) {
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

        item.getExecutable().execute(dataService, null);
        return true;
    }

    public void showGroupPopupMenu(View view, String groupUID) {
        this.clicked_group_uid = groupUID;
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

        if (!Utils.isWirelessLanConnected(getActivity()))
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
                adapter.notifyDataSetChanged();
                if (position == -1) {
                    adapter.notifyDataSetChanged();
                } else {
                    ExecutableViewHolder executableViewHolder = (ExecutableViewHolder) mRecyclerView.findViewHolderForPosition(position);
                    if (executableViewHolder != null) executableViewHolder.reload();
                    adapter.notifyItemChanged(position);
                }
            }
        } else
            super.onActivityResult(requestCode, resultCode, data);
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
        if (editMode) {
            AnimationController.animateViewInOut(edit_hint_big, true, true);
            AnimationController.animateBottomViewIn(btnAdd, btnAdd.getVisibility() == View.VISIBLE);
        } else {
            AnimationController.animateViewInOut(edit_hint_big, false, true);
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

    @Override
    public void onEmptyListener(boolean empty) {
        View view = getView();
        if (view == null) return;

        view.findViewById(R.id.empty_no_outlets_no_devices).setVisibility(View.GONE);
        view.findViewById(R.id.empty_group).setVisibility(View.GONE);
        view.findViewById(R.id.empty_no_outlets).setVisibility(View.GONE);

        if (empty) {
            boolean hasDevices = dataService != null && dataService.credentials.countConfigured() > 0;
            if (!hasDevices) {
                view.findViewById(R.id.empty_no_outlets_no_devices).setVisibility(View.VISIBLE);
            } else if (filterBySingleGroup.getFilterGroup() != null) {
                view.findViewById(R.id.empty_group).setVisibility(View.VISIBLE);
            } else {
                view.findViewById(R.id.empty_no_outlets).setVisibility(View.VISIBLE);
            }
        }
    }

    private enum ViewContentState {NotEmpty, NothingConfigured, ConfiguredButNothingToShow, EmptyGroup}


}
