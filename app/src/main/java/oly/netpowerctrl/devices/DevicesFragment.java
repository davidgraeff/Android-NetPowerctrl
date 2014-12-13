package oly.netpowerctrl.devices;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupMenu;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.onDataQueryRefreshQuery;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.pluginservice.PluginInterface;
import oly.netpowerctrl.pluginservice.PluginRemote;
import oly.netpowerctrl.pluginservice.PluginService;
import oly.netpowerctrl.preferences.PreferencesFragment;
import oly.netpowerctrl.ui.RecyclerItemClickListener;
import oly.netpowerctrl.ui.notifications.InAppNotifications;
import oly.netpowerctrl.ui.widgets.FloatingActionButton;
import oly.netpowerctrl.utils.AnimationController;
import oly.netpowerctrl.utils.DividerItemDecoration;

/**
 */
public class DevicesFragment extends Fragment
        implements PopupMenu.OnMenuItemClickListener,
        SwipeRefreshLayout.OnRefreshListener, onDataQueryRefreshQuery {
    private DevicesAdapter adapter;
    private SwipeRefreshLayout mPullToRefreshLayout;
    private RecyclerView mRecyclerView;
    private WeakReference<Device> deviceWeakReference = null;

    public DevicesFragment() {
    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.devices, menu);
        //noinspection ConstantConditions
        menu.findItem(R.id.menu_delete_all).setVisible(AppData.getInstance().deviceCollection.hasDevices());
    }

    @Override
    public void onPause() {
        AppData.observersStartStopRefresh.unregister(this);
        super.onPause();
        if (adapter != null)
            adapter.onPause();
    }

    @Override
    public void onResume() {
        AppData.observersStartStopRefresh.register(this);
        super.onResume();
        if (adapter != null)
            adapter.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_help: {
                //noinspection ConstantConditions
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.menu_help)
                        .setMessage(R.string.help_devices)
                        .setIcon(android.R.drawable.ic_menu_help).show();
                return true;
            }

            case R.id.menu_delete_all: {
                //noinspection ConstantConditions
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.delete_all_devices)
                        .setMessage(R.string.confirmation_delete_all_devices)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Delete all scenes
                                AppData.getInstance().deviceCollection.removeAll();
                                onRefresh();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();

                return true;
            }
        }
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new DevicesAdapter(true, true);
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                checkChanged();
            }

            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                checkChanged();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                checkChanged();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                checkChanged();
            }
        });
        setHasOptionsMenu(true);
    }

    private void checkChanged() {
        View emptyView = getView();
        if (emptyView == null)
            return;
        emptyView = emptyView.findViewById(android.R.id.empty);
        if (emptyView == null)
            return;

        if (adapter.getItemCount() == 0) {
            emptyView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.GONE);
        }
    }

    private void assignAdapter() {
        mRecyclerView.setAdapter(adapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_devices, container, false);
        assert view != null;
        mRecyclerView = (RecyclerView) view.findViewById(android.R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST) {
            @Override
            public boolean dividerForPosition(int position) {
                return position < adapter.getItemCount() - 1 && adapter.getItem(position + 1).isDeviceHeader;
            }
        });

        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public boolean onItemClick(View view, int position, boolean isLongClick) {
                final DevicesAdapter.DeviceAdapterItem item = adapter.getItem(position);
                if (!item.enabled)
                    return true;

                Device device = item.isConfigured ?
                        AppData.getInstance().findDevice(item.getUid()) :
                        AppData.getInstance().findDeviceUnconfigured(item.getUid());

                if (device == null)
                    return true;

                if (!item.isDeviceHeader) {
                    final PluginInterface pluginInterface = (PluginInterface) device.getPluginInterface();
                    if (pluginInterface != null) {
                        item.tested = false;
                        item.reachable = false;
                        item.subtitle = getString(R.string.device_connection_testing);
                        adapter.notifyItemChanged(position);
                        item.enabled = false;
                        device.setChangesFlag(Device.CHANGE_DEVICE_REACHABILITY);
                        final Device finalDevice = device;
                        App.getMainThreadHandler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                item.enabled = true;
                                pluginInterface.requestData(finalDevice, item.connectionID);
                            }
                        }, 1000);
                    }
                    return true;
                }

                if (device.isConfigured()) {
                    deviceWeakReference = new WeakReference<>(device);
                    @SuppressWarnings("ConstantConditions")
                    PopupMenu popup = new PopupMenu(getActivity(), view);
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.configured_device_item, popup.getMenu());
                    Menu menu = popup.getMenu();
                    menu.findItem(R.id.menu_device_configuration_page).setVisible(device.getPluginInterface() != null);
                    menu.findItem(R.id.menu_device_configure_network).setVisible(!(device.getPluginInterface() instanceof PluginRemote));
                    popup.setOnMenuItemClickListener(DevicesFragment.this);
                    popup.show();
                } else {
                    // Set default values for anel devices
                    show_configure_device_dialog(device);
                }
                return true;
            }
        }, null));
        assignAdapter();
        Button btn = (Button) view.findViewById(R.id.btnChangeToPreferences);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.getNavigationController().changeToFragment(PreferencesFragment.class.getName());
            }
        });
        mPullToRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.ptr_layout);
        mPullToRefreshLayout.setOnRefreshListener(this);
        mPullToRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.btnAdd);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                show_configure_device_dialog(null);
            }
        });
        fab.setVisibility(View.INVISIBLE);
        AnimationController.animateBottomViewIn(fab);

        return view;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        final Device current_device = deviceWeakReference.get();

        switch (menuItem.getItemId()) {
            case R.id.menu_device_configure_network: {
                show_configure_device_dialog(current_device);
                return true;
            }

            case R.id.menu_device_createGroup: {
                //noinspection ConstantConditions
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.device_createGroup)
                        .setMessage(R.string.confirmation_device_createGroup)
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                assert current_device != null;
                                current_device.lockDevicePorts();
                                Iterator<DevicePort> it = current_device.getDevicePortIterator();
                                UUID uuidOfDevice = UUID.nameUUIDFromBytes(current_device.getUniqueDeviceID().getBytes());
                                while (it.hasNext()) {
                                    it.next().addToGroup(uuidOfDevice);
                                }
                                current_device.releaseDevicePorts();
                                AppData.getInstance().deviceCollection.save(current_device);
                                AppData.getInstance().groupCollection.edit(uuidOfDevice, current_device.getDeviceName());
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();
                return true;
            }

            case R.id.menu_device_delete: {
                //noinspection ConstantConditions
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.delete_device)
                        .setMessage(R.string.confirmation_delete_device)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                AppData.getInstance().deviceCollection.remove(current_device);
                                AppData.getInstance().refreshDeviceData(true);
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();
                return true;
            }

            case R.id.menu_device_configuration_page:
                PluginInterface pluginInterface = (PluginInterface) current_device.getPluginInterface();
                if (pluginInterface != null)
                    pluginInterface.openConfigurationPage(current_device, getActivity());
                return true;
            case R.id.menu_device_share:
                DeviceShareDialog.show(getActivity(), current_device);
                return true;
            case R.id.menu_device_hide_items:
                DeviceItemsDialog.show(getActivity(), current_device);
                return true;
            default:
                return false;
        }
    }

    private void show_configure_device_dialog(Device device) {
        if (device == null) {
            // new device
            int selected = 0;
            //String[] plugins = ListenService.getService().pluginIDs();

            DevicesWizardNewDialog newFragment = (DevicesWizardNewDialog) Fragment.instantiate(getActivity(), DevicesWizardNewDialog.class.getName());
            //newFragment.setPlugin(ListenService.getService().getPluginByID(plugins[selected]));
            newFragment.setPlugin(PluginService.getService().getPlugin(selected));
            MainActivity.getNavigationController().changeToDialog(getActivity(), newFragment);
        } else {
            PluginInterface pluginInterface = (PluginInterface) device.getPluginInterface();
            if (pluginInterface == null) {
                InAppNotifications.showException(getActivity(), null, "show_configure_device_dialog: Plugin not known!");
                return;
            }
            pluginInterface.showConfigureDeviceScreen(device);
        }
    }

    @Override
    public void onRefreshStateChanged(boolean isRefreshing) {
        mPullToRefreshLayout.setRefreshing(isRefreshing);
    }

    @Override
    public void onRefresh() {
        PluginService.getService().showNotificationForNextRefresh(true);
        AppData.getInstance().refreshDeviceData(true);
    }
}