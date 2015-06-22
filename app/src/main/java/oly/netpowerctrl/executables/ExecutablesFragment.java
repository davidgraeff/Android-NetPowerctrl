package oly.netpowerctrl.executables;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;

import oly.netpowerctrl.App;
import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AutomaticSetup;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.data.graphic.IconCacheCleared;
import oly.netpowerctrl.data.graphic.LoadStoreIconData;
import oly.netpowerctrl.data.onServiceReady;
import oly.netpowerctrl.data.query.onDataQueryRefreshQuery;
import oly.netpowerctrl.executables.adapter.AdapterSource;
import oly.netpowerctrl.executables.adapter.ExecutableAdapterItem;
import oly.netpowerctrl.executables.adapter.ExecutableViewHolder;
import oly.netpowerctrl.executables.adapter.ExecutablesAdapter;
import oly.netpowerctrl.executables.adapter.ExecutablesEditableAdapter;
import oly.netpowerctrl.executables.adapter.FilterByHidden;
import oly.netpowerctrl.executables.adapter.FilterByReachable;
import oly.netpowerctrl.executables.adapter.FilterBySingleGroup;
import oly.netpowerctrl.executables.adapter.InputConfiguredExecutables;
import oly.netpowerctrl.executables.adapter.InputGroupChanges;
import oly.netpowerctrl.groups.GroupListFragment;
import oly.netpowerctrl.groups.GroupUtilities;
import oly.netpowerctrl.main.EditActivity;
import oly.netpowerctrl.network.Utils;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.scenes.SceneHelp;
import oly.netpowerctrl.ui.EmptyListener;
import oly.netpowerctrl.ui.FragmentUtils;
import oly.netpowerctrl.ui.ItemShadowDecoration;
import oly.netpowerctrl.ui.MaterialCab;
import oly.netpowerctrl.ui.RecyclerItemClickListener;
import oly.netpowerctrl.ui.SimpleListDividerDecoration;
import oly.netpowerctrl.utils.AnimationController;

/**
 * This fragment consists of some floating buttons (add + no_wireless) and a view pager. Within the
 * fragments in the view pager is a RecycleView and a placeholder text. The computation for
 */
public class ExecutablesFragment extends Fragment implements PopupMenu.OnMenuItemClickListener,
        onDataQueryRefreshQuery,
        SwipeRefreshLayout.OnRefreshListener, IconCacheCleared,
        RecyclerItemClickListener.OnItemClickListener, onServiceReady,
        SharedPreferences.OnSharedPreferenceChangeListener, EmptyListener, MaterialCab.Callback {

    // Column computations
    public static final int VIEW_AS_LIST = 0;
    public static final int VIEW_AS_GRID = 1;
    public static final int VIEW_AS_COMPACT = 2;
    // Groups
    FilterBySingleGroup filterBySingleGroup = new FilterBySingleGroup(null);
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mPullToRefreshLayout;
    private View btnWireless;
    // Adapter
    private AdapterSource adapterSource;
    private ExecutablesEditableAdapter adapter;
    private String clicked_group_uid;
    // UI
    private boolean editMode;
    private SimpleListDividerDecoration listDividerDecoration;
    private GridLayoutManager gridLayoutManager;
    private int requestedColumnWidth = 0;
    private View btnAdd;
    private View btnEdit;
    // Data
    private DataService dataService;
    private AutomaticSetup automaticSetup;
    private MaterialCab cab = null;

    public ExecutablesFragment() {
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
        setHasOptionsMenu(false);
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
        int columns;

        switch (viewType) {
            case VIEW_AS_COMPACT:
                requestedColumnWidth = (int) getResources().getDimension(R.dimen.min_grid_item_width);
                columns = Math.max(mRecyclerView.getWidth() / requestedColumnWidth, 1);
                gridLayoutManager.setSpanCount(columns);
                adapter.setLayoutRes(R.layout.grid_item_compact_executable);
                break;
            case VIEW_AS_GRID:
                requestedColumnWidth = (int) getResources().getDimension(R.dimen.min_grid_item_width);
                columns = Math.max(mRecyclerView.getWidth() / requestedColumnWidth, 1);
                gridLayoutManager.setSpanCount(columns);
                adapter.setLayoutRes(R.layout.grid_item_executable);
                break;
            case VIEW_AS_LIST:
            default:
                mRecyclerView.addItemDecoration(listDividerDecoration);
                requestedColumnWidth = (int) getResources().getDimension(R.dimen.min_list_item_width);
                columns = Math.max(mRecyclerView.getWidth() / requestedColumnWidth, 1);
                gridLayoutManager.setSpanCount(columns);
                adapter.setLayoutRes(R.layout.list_item_executable);
                break;
        }
    }

    @Override
    public void onServiceFinished(DataService service) {
        dataService = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_outlets, container, false);
        assert view != null;

        cab = new MaterialCab(getActivity(), R.id.cab_stub);
        cab.setMenu(R.menu.outlets_editmode);
        cab.setTitleRes(R.string.outlets_edit_mode);

        btnEdit = getActivity().findViewById(R.id.btnEdit);
        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setEditMode(!editMode);
            }
        });

        mRecyclerView = (RecyclerView) view.findViewById(android.R.id.list);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mRecyclerView.addItemDecoration(new ItemShadowDecoration((NinePatchDrawable) ContextCompat.getDrawable(getActivity(), R.drawable.material_shadow_z1)));
        }
        listDividerDecoration = new SimpleListDividerDecoration(ContextCompat.getDrawable(getActivity(), R.drawable.list_divider), true);
        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), this, null));
        gridLayoutManager = new GridLayoutManager(getActivity(), 1);
        mRecyclerView.setHasFixedSize(false);
        mRecyclerView.setLayoutManager(gridLayoutManager);

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
        adapterSource.addInput(new InputConfiguredExecutables(), new InputGroupChanges());

        adapter = new ExecutablesEditableAdapter(adapterSource, LoadStoreIconData.iconLoadingThread);
        adapter.setItemsInRow(new ExecutablesAdapter.ItemsInRow() {
            @Override
            public int getItemsInRow() {
                return gridLayoutManager.getSpanCount();
            }
        });
        gridLayoutManager.setSpanSizeLookup(adapter.getSpanSizeLookup());
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

        btnAdd = view.findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (SharedPrefs.getInstance().isFirstTimeSceneAdd()) {
                    SceneHelp.showHelp(getActivity(), R.string.scene_add, R.string.help_scene);
                } else {
                    Intent it = new Intent(getActivity(), EditActivity.class);
                    it.putExtra(EditActivity.CREATE_SCENE, true);
                    startActivityForResult(it, EditActivity.REQUEST_CODE);
                }
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                v.removeOnLayoutChangeListener(this);
                if (savedInstanceState != null) {
                    setEditMode(savedInstanceState.getBoolean("editMode"));
                } else
                    setEditMode(false);
                mRecyclerView.scrollToPosition(SharedPrefs.getInstance().getLastScrollIndex());
            }
        });

        mRecyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            int lastWidth = 0;

            @Override
            public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                if (requestedColumnWidth == 0 || mRecyclerView.getWidth() == lastWidth) return;
                lastWidth = mRecyclerView.getWidth();
                int columns = Math.max(lastWidth / requestedColumnWidth, 1);
                gridLayoutManager.setSpanCount(columns);
            }
        });

        LoadStoreIconData.iconCacheClearedObserver.register(this);
        DataService.observersStartStopRefresh.register(this);
        DataService.observersServiceReady.register(this); // Will call onServiceReady and setup current group
    }

    @Override
    public void onDestroyView() {
        View view = mRecyclerView.findChildViewUnder(10, 10);
        if (view != null)
            SharedPrefs.getInstance().setLastScrollIndex(mRecyclerView.getChildAdapterPosition(view));
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
        ((ExecutableViewHolder) mRecyclerView.findViewHolderForAdapterPosition(position)).animate();
        adapter.notifyItemChanged(position);

        item.getExecutable().execute(dataService, ExecutableAndCommand.TOGGLE, null);
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
                    ExecutableViewHolder executableViewHolder = (ExecutableViewHolder) mRecyclerView.findViewHolderForAdapterPosition(position);
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
        updateEditButtonVisibility();
        if (editMode) {
            AnimationController.animateBottomViewIn(btnAdd, btnAdd.getVisibility() == View.VISIBLE);
            cab.start(this);
        } else {
            AnimationController.animateBottomViewOut(btnAdd);
            if (cab.isActive()) cab.finish();
        }
        adapter.setEditMode(editMode);
    }

    private void updateEditButtonVisibility() {
        boolean visible = !adapterSource.isEmpty() && !editMode;
        AnimationController.animateViewInOutWithoutCheck(btnEdit, visible, true, 500);
    }

    @Override
    public void onStop() {
        AnimationController.animateViewInOutWithoutCheck(btnEdit, false, true, 500);
        super.onStop();
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
        if (view == null || dataService == null) return;

        view.findViewById(R.id.empty_no_outlets_no_devices).setVisibility(View.GONE);
        view.findViewById(R.id.empty_group).setVisibility(View.GONE);
        view.findViewById(R.id.empty_no_outlets).setVisibility(View.GONE);

        if (empty) {
            boolean hasDevices = dataService.credentials.countConfigured() > 0;
            if (!hasDevices) {
                view.findViewById(R.id.empty_no_outlets_no_devices).setVisibility(View.VISIBLE);
                automaticSetup.refresh();
            } else if (filterBySingleGroup.getFilterGroup() != null) {
                view.findViewById(R.id.empty_group).setVisibility(View.VISIBLE);
            } else {
                view.findViewById(R.id.empty_no_outlets).setVisibility(View.VISIBLE);
            }
        }

        updateEditButtonVisibility();
    }

    @Override
    public boolean onCabCreated(MaterialCab cab, Menu menu) {
        return true;
    }

    @Override
    public boolean onCabItemClicked(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_view_mode: {
                FragmentUtils.changeToDialog(getActivity(), ExecutablesViewModeDialog.class.getName());
                return true;
            }
            case R.id.menu_device_hide_items:
                FragmentUtils.changeToDialog(getActivity(), ExecutableHideShowDialog.class.getName());
                return true;
        }
        return false;
    }

    @Override
    public void onCabFinished(MaterialCab cab) {
        setEditMode(false);
    }
}
