package oly.netpowerctrl.ioconnection;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import android.widget.PopupMenu;

import com.rey.material.app.Dialog;
import com.rey.material.app.SimpleDialog;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.App;
import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AbstractBasePlugin;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.data.query.onDataQueryRefreshQuery;
import oly.netpowerctrl.devices.Credentials;
import oly.netpowerctrl.devices.CredentialsDialog;
import oly.netpowerctrl.groups.GroupUtilities;
import oly.netpowerctrl.ioconnection.adapter.AdapterItem;
import oly.netpowerctrl.ioconnection.adapter.AdapterItemConnection;
import oly.netpowerctrl.ioconnection.adapter.AdapterItemHeader;
import oly.netpowerctrl.ioconnection.adapter.IOConnectionAdapter;
import oly.netpowerctrl.network.ReachabilityStates;
import oly.netpowerctrl.ui.EmptyListener;
import oly.netpowerctrl.ui.FragmentUtils;
import oly.netpowerctrl.ui.LineDividerDecoration;
import oly.netpowerctrl.ui.RecyclerItemClickListener;
import oly.netpowerctrl.ui.ThemeHelper;

;

/**
 * List of all Credentials and IOConnections.
 */
public class IOConnectionsFragment extends Fragment
        implements onDataQueryRefreshQuery, RecyclerItemClickListener.OnItemClickListener, EmptyListener {
    private IOConnectionAdapter adapter;
    private SwipeRefreshLayout mPullToRefreshLayout;
    private RecyclerView mRecyclerView;
    private View empty;
    private Handler testConnectionHandler = new TestConnectionHandler();

    public IOConnectionsFragment() {
    }

    @Override
    public void onPause() {
        DataService.observersStartStopRefresh.unregister(this);
        super.onPause();
    }

    @Override
    public void onResume() {
        DataService.observersStartStopRefresh.register(this);
        super.onResume();
        adapter.onResume();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new IOConnectionAdapter(true, true);
        adapter.setEmptyListener(this);
    }

    private void assignAdapter() {
        mRecyclerView.setAdapter(adapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_devices, container, false);
        assert view != null;

        empty = view.findViewById(R.id.empty);

        mRecyclerView = (RecyclerView) view.findViewById(android.R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addItemDecoration(new LineDividerDecoration(getActivity(), LineDividerDecoration.VERTICAL_LIST) {
            @Override
            public boolean dividerForPosition(int position) {
                return position < adapter.getItemCount() - 1 && adapter.getItem(position + 1) instanceof AdapterItemHeader;
            }
        });

        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), this, null));

        assignAdapter();

        mPullToRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.ptr_layout);
        mPullToRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                DataService service = DataService.getService();
                service.showNotificationForNextRefresh(true);
                service.detectDevices();
            }
        });
        mPullToRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        View fab = view.findViewById(R.id.btnAdd);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickNotConfiguredCredentials(null);
            }
        });

        return view;
    }

    @Override
    public boolean onItemClick(View view, int position, boolean isLongClick) {
        final AdapterItem item = adapter.getItem(position);
        if (!item.enabled)
            return true;

        if (item instanceof AdapterItemConnection) {
            PopupMenu popup = new PopupMenu(getActivity(), view);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.connection_item, popup.getMenu());
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    return clickConnection(menuItem.getItemId(), (AdapterItemConnection) item);
                }
            });
            popup.show();
            return true;
        }

        if (item.credentials.isConfigured()) {
            final WeakReference<Credentials> deviceWeakReference = new WeakReference<>(item.credentials);

            PopupMenu popup = new PopupMenu(getActivity(), view);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.configured_device_item, popup.getMenu());
            Menu menu = popup.getMenu();
            menu.findItem(R.id.menu_device_connections_add).setVisible(item.credentials.getPlugin().isNewIOConnectionAllowed(item.credentials));
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    return clickConfiguredCredentials(menuItem.getItemId(), deviceWeakReference.get());
                }
            });
            popup.show();
        } else {
            clickNotConfiguredCredentials(item.credentials);
        }
        return true;
    }

    private boolean clickConnection(int click_id, AdapterItemConnection item) {
        DataService dataService = DataService.getService();
        switch (click_id) {
            case R.id.menu_connection_edit: {
                if (item.ioConnection instanceof IOConnectionHTTP) {
                    IOConnectionHttpDialog.show(getActivity(), (IOConnectionHTTP) item.ioConnection);
                }
                return true;
            }
            case R.id.menu_connection_test: {
                item.enabled = false;
                item.ioConnection.setReachability(ReachabilityStates.MaybeReachable);
                item.ioConnection.setStatusMessage(getString(R.string.device_connection_testing));
                dataService.connections.put(item.ioConnection);
                testConnectionHandler.sendMessageDelayed(testConnectionHandler.obtainMessage(TestConnectionHandler.MSG_TEST_CONNECTION, item), 500);
                testConnectionHandler.sendMessageDelayed(testConnectionHandler.obtainMessage(TestConnectionHandler.MSG_TEST_ABORT, item), 1500);
                return true;
            }
            case R.id.menu_connection_delete: {
                dataService.connections.remove(item.ioConnection);
                return true;
            }
            default:
                return false;
        }
    }

    private boolean clickConfiguredCredentials(int click_id, @NonNull final Credentials credentials) {
        switch (click_id) {
            case R.id.menu_device_connections_add: {
                credentials.getPlugin().addNewIOConnection(credentials, getActivity());
                return true;
            }

            case R.id.menu_device_edit_credentials: {
                clickNotConfiguredCredentials(credentials);
                return true;
            }

            case R.id.menu_device_createGroup: {
                GroupUtilities.askForGroup(getActivity(), credentials);
                return true;
            }

            case R.id.menu_device_delete: {
                final SimpleDialog.Builder builder = new SimpleDialog.Builder(ThemeHelper.getDialogRes(getActivity()));
                builder.title(getString(R.string.delete_device));
                builder.message(getString(R.string.confirmation_delete_device));
                builder.negativeAction(getString(android.R.string.no)).positiveAction(getString(android.R.string.yes));
                final Dialog dialog = builder.build(getActivity());
                dialog.positiveActionClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                        DataService dataService = DataService.getService();
                        dataService.remove(credentials);
                    }
                });
                dialog.negativeActionClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
                dialog.show();
                return true;
            }

            case R.id.menu_device_external_configuration_page:
                AbstractBasePlugin abstractBasePlugin = credentials.getPlugin();
                if (abstractBasePlugin != null)
                    abstractBasePlugin.openConfigurationPage(credentials);
                return true;
            default:
                return false;
        }
    }

    private void clickNotConfiguredCredentials(@Nullable Credentials credentials) {
        if (credentials == null) {
            final List<String> pluginIDs = new ArrayList<>();
            final String[] plugins = DataService.getService().pluginNames(pluginIDs, new DataService.FilterPlugin() {
                @Override
                public boolean accept(AbstractBasePlugin plugin) {
                    return plugin.supportProperty(AbstractBasePlugin.Properties.ManuallyAddDevice);
                }
            });
            final SimpleDialog.Builder builder = new SimpleDialog.Builder(ThemeHelper.getDialogRes(getActivity()));
            builder.title(getString(R.string.select_plugin)).positiveAction(getString(android.R.string.ok))
                    .negativeAction(getString(android.R.string.cancel));
            builder.items(plugins, 0);
            final Dialog dialog = builder.build(getActivity());
            dialog.positiveActionClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                    int i = builder.getSelectedIndex();
                    CredentialsDialog newFragment = (CredentialsDialog) Fragment.instantiate(getActivity(), CredentialsDialog.class.getName());
                    newFragment.setCredentials(DataService.getService().getPlugin(pluginIDs.get(i)), null);
                    FragmentUtils.changeToDialog(getActivity(), newFragment);
                }
            });
            dialog.negativeActionClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                }
            });
            dialog.show();
        } else {
            CredentialsDialog newFragment = (CredentialsDialog) Fragment.instantiate(getActivity(), CredentialsDialog.class.getName());
            newFragment.setCredentials(credentials.getPlugin(), credentials);
            FragmentUtils.changeToDialog(getActivity(), newFragment);
        }
    }

    @Override
    public void onRefreshStateChanged(boolean isRefreshing) {
        mPullToRefreshLayout.setRefreshing(isRefreshing);
    }

    @Override
    public void onEmptyListener(boolean empty) {
        if (this.empty == null) return;
        this.empty.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private static class TestConnectionHandler extends Handler {
        private static final int MSG_TEST_CONNECTION = 0;
        private static final int MSG_TEST_ABORT = 1;

        @Override
        public void handleMessage(Message msg) {
            AdapterItemConnection item = (AdapterItemConnection) msg.obj;
            switch (msg.what) {
                case MSG_TEST_CONNECTION: {
                    item.enabled = true;
                    final AbstractBasePlugin abstractBasePlugin = item.credentials.getPlugin();
                    if (abstractBasePlugin != null) {
                        abstractBasePlugin.requestData(item.ioConnection);
                    }
                    break;
                }
                case MSG_TEST_ABORT: {
                    if (item.ioConnection.reachableState() == ReachabilityStates.MaybeReachable) {
                        item.ioConnection.setReachability(ReachabilityStates.NotReachable);
                        item.ioConnection.setStatusMessage(App.getAppString(R.string.device_timeout));
                        item.credentials.getPlugin().getDataService().connections.put(item.ioConnection);
                    }
                    break;
                }
            }
        }
    }
}