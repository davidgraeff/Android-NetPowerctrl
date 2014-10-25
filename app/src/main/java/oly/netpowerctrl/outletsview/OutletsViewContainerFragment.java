package oly.netpowerctrl.outletsview;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;

import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.Executable;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.data.onDataQueryCompleted;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.devices.DevicePort;
import oly.netpowerctrl.devices.DevicesAutomaticFragment;
import oly.netpowerctrl.devices.UnconfiguredDeviceCollection;
import oly.netpowerctrl.executables.ExecutableAdapterItem;
import oly.netpowerctrl.executables.ExecutableViewHolder;
import oly.netpowerctrl.executables.ExecutablesSourceBase;
import oly.netpowerctrl.executables.ExecutablesSourceDevicePorts;
import oly.netpowerctrl.executables.ExecutablesSourceGroups;
import oly.netpowerctrl.executables.ExecutablesSourceScenes;
import oly.netpowerctrl.executables.ExecuteAdapter;
import oly.netpowerctrl.groups.GroupCollection;
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
import oly.netpowerctrl.utils.actionbar.GroupPagerAdapter;
import oly.netpowerctrl.utils.controls.FloatingActionButton;
import oly.netpowerctrl.utils.fragments.onFragmentChangeArguments;
import oly.netpowerctrl.utils.notifications.InAppNotifications;
import oly.netpowerctrl.utils.notifications.TextNotification;

/**
 * This fragment consists of some floating buttons (add + no_wireless) and a view pager. Within the
 * fragments in the view pager is a RecycleView and a placeholder text. The computation for
 */
public class OutletsViewContainerFragment extends Fragment implements PopupMenu.OnMenuItemClickListener,
        onNotReachableUpdate, onFragmentChangeArguments,
        onServiceRefreshQuery, SharedPrefs.IHideNotReachable, onServiceModeChanged,
        ExecutablesSourceDevicePorts.onChange, onDataQueryCompleted, onCollectionUpdated<UnconfiguredDeviceCollection, Device> {

    public static final int VIEW_AS_LIST = 0;
    public static final int VIEW_AS_GRID = 1;
    public static final int VIEW_AS_COMPACT = 2;
    ExecutablesSourceBase adapterSource;
    // Column computations
    int requestedColumnWidth;
    int spaceBottomForFloatingButtons;
    // Groups and outlets
    private GroupPagerAdapter groupPagerAdapter;
    private ExecuteAdapter adapter;
    private UUID groupFilter = null;
    private int clickedPosition;
    public RecyclerItemClickListener onItemTouchListener = new RecyclerItemClickListener(getActivity(), new RecyclerItemClickListener.OnItemClickListener() {
        @Override
        public boolean onItemClick(View view, int position) {
            ExecutableAdapterItem item = adapter.getItem(position);
            if (!(item.groupType() == ExecutableAdapterItem.groupTypeEnum.NOGROUP_TYPE)) {
                clickedPosition = position;
                //noinspection ConstantConditions
                PopupMenu popup = new PopupMenu(getActivity(), view);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.group, popup.getMenu());
                popup.setOnMenuItemClickListener(OutletsViewContainerFragment.this);
                popup.show();
                return true;
            }

            if (view.getId() == R.id.icon_edit) {
                onEditViewClicked(onItemTouchListener.getRecyclerView(), view, position);
                return true;
            }

            boolean restrict = SharedPrefs.getInstance().getSmallerClickExecuteArea();
            if (restrict && view.getId() != R.id.title)
                return true;

            // We animate the click event. This is done by calling animate on the
            // viewHolder of the current position.
            ((ExecutableViewHolder) onItemTouchListener.getRecyclerView().findViewHolderForPosition(position)).animate();
            adapter.notifyItemChanged(position);

            AppData.getInstance().executeToggle(item.getExecutable(), null);
            return true;
        }
    }, null);
    private FloatingActionButton btnWireless;
    private ViewPager pager;
    private int emptyInit = 0;
    private checkEmptyState lastState = checkEmptyState.UNKNOWN;

    public OutletsViewContainerFragment() {
    }

    public static Bundle createBundleForView(int viewType) {
        Bundle bundle = new Bundle();
        bundle.putInt("viewtype", viewType);
        return bundle;
    }

    public ExecuteAdapter getAdapter() {
        return adapter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        groupPagerAdapter = new GroupPagerAdapter(getFragmentManager(), this);
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
        if (mExtra != null && mExtra.containsKey("viewtype") && pager != null) {
            setViewType(mExtra.getInt("viewtype"));
            OutletsViewFragment fragment = (OutletsViewFragment) groupPagerAdapter.instantiateItem(pager, pager.getCurrentItem());
            if (fragment != null)
                fragment.viewTypeChanged();
            return;
        }

        UUID newGroupFilter;

        if (mExtra != null && mExtra.containsKey("filter"))
            newGroupFilter = UUID.fromString(mExtra.getString("filter"));
        else
            newGroupFilter = null;

        int pos = AppData.getInstance().groupCollection.indexOf(newGroupFilter) + 1;
        pager.setCurrentItem(pos);

        checkEmptyChanged(checkEmptyAction.ADDREMOVE);
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
                refreshNow();
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

        { // Add scene floating button
            final GroupCollection groupCollection = AppData.getInstance().groupCollection;
            final FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.btnAdd);
            fab.setVisibility(View.VISIBLE);
            fab.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (groupCollection.size() == 0)
                        return false;

                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1);
                    arrayAdapter.addAll(groupCollection.getGroupsArray());

                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.jump_to_group)
                            .setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int position) {
                                    pager.setCurrentItem(position + 1);
                                }
                            })
                            .show();
                    return true;
                }
            });
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!AppData.getInstance().deviceCollection.hasDevices()) {
                        MainActivity.getNavigationController().changeToDialog(getActivity(), DevicesAutomaticFragment.class.getName());
                        return;
                    }

                    //noinspection ConstantConditions
                    PopupMenu popup = new PopupMenu(getActivity(), fab);
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.outlet_adds, popup.getMenu());
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            switch (menuItem.getItemId()) {
                                case R.id.menu_add_group:
                                    GroupUtilities.createGroupForDevicePort(getActivity(), null);
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
            });
            AnimationController.animateFloatingButton(fab);
            spaceBottomForFloatingButtons = fab.getHeight();
        }

        pager = (ViewPager) view.findViewById(R.id.pager);
        pager.setAdapter(groupPagerAdapter);
        pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {

            }

            @Override
            public void onPageSelected(int position) {
                groupFilter = position == 0 ? null : AppData.getInstance().groupCollection.get(position - 1).uuid;
                if (adapter != null) {
                    if (adapter.setGroupFilter(groupFilter))
                        adapterSource.updateNow();
                }

                OutletsViewFragment fragment = (OutletsViewFragment) groupPagerAdapter.instantiateItem(pager, position);
                checkEmpty(fragment);
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

        btnWireless = (FloatingActionButton) view.findViewById(R.id.btnWirelessSettings);
        if (!ListenService.isWirelessLanConnected(getActivity()))
            AnimationController.animateFloatingButton(btnWireless);
        btnWireless.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                //intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        });

        // Create adapter first
        adapterSource = new ExecutablesSourceDevicePorts();
        adapterSource.setHideNotReachable(SharedPrefs.getInstance().isHideNotReachable());
        adapterSource.setAutomaticUpdate(true);
        adapterSource.setOnChangeListener(this);

        ExecutablesSourceGroups groupSource = new ExecutablesSourceGroups();
        groupSource.setHideNotReachable(SharedPrefs.getInstance().isHideNotReachable());
        groupSource.setAutomaticUpdate(true);

        adapterSource.addChainItem(groupSource);

        ExecutablesSourceScenes sceneSource = new ExecutablesSourceScenes();
        sceneSource.setAutomaticUpdate(true);
        sceneSource.setOnChangeListener(this);

        groupSource.addChainItem(sceneSource);

        adapter = new ExecuteAdapter(adapterSource, LoadStoreIconData.iconLoadingThread);
        if (groupFilter != null) {
            if (adapter.setGroupFilter(groupFilter))
                adapterSource.updateNow();
        }

        checkEmptyChanged(checkEmptyAction.INIT_AFTER_VIEW);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        lastState = checkEmptyState.UNKNOWN;

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
        if (newState == checkEmptyAction.INIT_AFTER_VIEW)
            emptyInit = 1;

        if (emptyInit == 0)
            return;

        OutletsViewFragment fragment = (OutletsViewFragment) groupPagerAdapter.instantiateItem(pager, pager.getCurrentItem());

        switch (newState) {
            case ADDREMOVE:
                if (emptyInit == 2 && lastState == checkEmptyState.EMPTY)
                    checkEmpty(fragment);
                break;
            case FIRSTDATA:
                emptyInit = 2;
                checkEmpty(fragment);
                checkService();
                break;
            case NEWDEVICE:
                if (lastState == checkEmptyState.EMPTY)
                    checkEmpty(fragment);
                break;
            case SERVICE:
                if (emptyInit == 2) {
                    checkEmpty(fragment);
                    checkService();
                }
                break;
        }
    }

    public void refreshNow() {
        ListenService service = ListenService.getService();
        if (service != null)
            service.findDevices(true, null);
        else {
            InAppNotifications.showException(getActivity(), null, "OutletsFragment refresh: Service is down.");
        }
    }

    /**
     * Do not call this directly! Use checkEmptyChanged instead.
     */
    public void checkEmpty(OutletsViewFragment fragment) {
        lastState = checkEmptyState.UNKNOWN;

        if (!isAdded() || fragment == null)
            return;

        if (adapter.getItemCount() == 0) {
            lastState = checkEmptyState.EMPTY;

            boolean hasDevices = AppData.getInstance().deviceCollection.hasDevices();
            if (!hasDevices) {
                fragment.setEmptyText(R.string.empty_no_outlets_no_devices);
            } else if (groupFilter != null) {
                fragment.setEmptyText(R.string.empty_group);
            } else {
                fragment.setEmptyText(R.string.empty_no_outlets);
            }
        } else {
            lastState = checkEmptyState.FILLED;
            fragment.setEmptyText(0);
        }
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
        switch (menuItem.getItemId()) {
            case R.id.menu_removeGroup: {
                UUID uuid = adapter.getItem(clickedPosition).groupID();
                if (!AppData.getInstance().groupCollection.remove(uuid)) {
                    return true;
                }
                // change to overview if the removed group is the current group
                if (uuid.equals(groupFilter))
                    pager.setCurrentItem(0);
                return true;
            }
            case R.id.menu_renameGroup: {
                GroupUtilities.renameGroup(getActivity(), adapter.getItem(clickedPosition).groupID());
                return true;
            }
        }
        return false;
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
            InAppNotifications.addPermanentNotification(getActivity(), new TextNotification("not_reachable",
                    getString(R.string.devices_not_reachable, devices), true));
        } else {
            InAppNotifications.removePermanentNotification(getActivity(), "not_reachable");
        }
    }

    public void onEditViewClicked(RecyclerView recyclerView, View view, int position) {
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
            dialog.setDevicePort(devicePort, (ExecutableViewHolder) recyclerView.findViewHolderForPosition(position), adapter);
            MainActivity.getNavigationController().changeToDialog(getActivity(), dialog);
        } else if (executable instanceof Scene) {
            Intent it = new Intent(getActivity(), EditSceneActivity.class);
            it.putExtra(EditSceneActivity.EDIT_SCENE_NOT_SHORTCUT, true);
            it.putExtra(EditSceneActivity.LOAD_SCENE, ((Scene) executable).toString());
            startActivity(it);
        }
    }

    @Override
    public void hideNotReachable(boolean hideNotReachable) {
        adapterSource.setHideNotReachable(hideNotReachable);
        adapterSource.updateNow();
    }

    @Override
    public void onServiceModeChanged(boolean isNetworkDown) {
        if (!ListenService.isWirelessLanConnected(getActivity()))
            AnimationController.animateFloatingButton(btnWireless);
        else
            AnimationController.animateFloatingButtonOut(btnWireless);

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
    public void onRefreshStateChanged(boolean isRefreshing) {
        OutletsViewFragment view = (OutletsViewFragment) groupPagerAdapter.instantiateItem(pager, pager.getCurrentItem());
        if (view != null)
            view.onRefreshStateChanged(isRefreshing);
    }

    private enum checkEmptyState {UNKNOWN, EMPTY, FILLED}

    private enum checkEmptyAction {SERVICE, FIRSTDATA, NEWDEVICE, INIT_AFTER_VIEW, ADDREMOVE}


}
