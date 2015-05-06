package oly.netpowerctrl.ioconnection.adapter;

import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.data.onServiceReady;
import oly.netpowerctrl.devices.Credentials;
import oly.netpowerctrl.devices.CredentialsCollection;
import oly.netpowerctrl.ioconnection.DeviceIOConnections;
import oly.netpowerctrl.ioconnection.IOConnection;
import oly.netpowerctrl.ioconnection.IOConnectionsCollection;
import oly.netpowerctrl.network.ReachabilityStates;
import oly.netpowerctrl.ui.EmptyListener;
import oly.netpowerctrl.utils.ObserverUpdateActions;
import oly.netpowerctrl.utils.onCollectionUpdated;

/**
 * An adapter for showing all configured (and newly discovered) devices. Configured and new devices
 * are separated by headers.
 */
public class IOConnectionAdapter extends RecyclerView.Adapter<IOConnectionAdapter.ViewHolder> implements onServiceReady {

    private static final int DEVICE_HEADER = 0;
    private static final int DEVICE_CONNECTION = 1;
    private final boolean showNewDevices;
    private final boolean showConnections;
    CredentialsUpdater credentialsUpdater = new CredentialsUpdater();
    IOConnectionUpdater ioConnectionUpdater = new IOConnectionUpdater();
    private List<AdapterItem> mList = new ArrayList<>();
    private EmptyListener emptyListener = new EmptyListener() {
        public void onEmptyListener(boolean empty) {
        }
    };

    @SuppressWarnings("SameParameterValue")
    public IOConnectionAdapter(boolean showNewDevices, boolean showConnections) {
        this.showNewDevices = showNewDevices;
        this.showConnections = showConnections;
    }

    public void setEmptyListener(EmptyListener emptyListener) {
        this.emptyListener = emptyListener;
    }

    public void onResume() {
        DataService.observersServiceReady.register(this);
    }

    @Override
    public int getItemViewType(int position) {
        return mList.get(position) instanceof AdapterItemHeader ? DEVICE_HEADER : DEVICE_CONNECTION;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        int res;
        if (viewType == DEVICE_HEADER)
            res = R.layout.list_item_device;
        else
            res = R.layout.list_item_device_connection;
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(res, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        AdapterItem item = mList.get(position);
        TextView title = viewHolder.title;
        title.setText(item.title);

        viewHolder.updateData(item);

        if (item instanceof AdapterItemConnection) {
            ReachabilityStates state = ((AdapterItemConnection) item).reachableState();
            if (state == ReachabilityStates.Reachable) {
                title.setPaintFlags(title.getPaintFlags() & ~(Paint.STRIKE_THRU_TEXT_FLAG));
                viewHolder.image.setImageResource(android.R.drawable.presence_online);
            } else {
                if (state == ReachabilityStates.NotReachable)
                    viewHolder.image.setImageResource(android.R.drawable.presence_offline);
                else
                    viewHolder.image.setImageResource(android.R.drawable.presence_away);
                title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }
        }

        TextView subtitle = viewHolder.subtitle;
        subtitle.setText(item.getSubtitle());
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    private void fullUpdate(DataService dataService) {
        boolean empty = mList.isEmpty();
        mList.clear();

        List<Credentials> credentialsList = new ArrayList<>(dataService.credentials.getItems().values());

        for (Credentials credentials : credentialsList)
            addDeviceToList(credentials, mList.size(), dataService);

        // Initial data
        notifyDataSetChanged();

        if (empty != mList.isEmpty())
            emptyListener.onEmptyListener(mList.isEmpty());
    }

    /**
     * This is called by the {@link AdapterItem} if item data changed and the view has to be notified.
     *
     * @param adapterItem         The adapterItem.
     * @param last_known_position The last known position. Will speed up this method if the position is correct.
     */
    int notifyItemChanged(AdapterItem adapterItem, int last_known_position) {
        if (mList.get(last_known_position) == adapterItem) {
            notifyItemChanged(last_known_position);
            return last_known_position;
        }

        for (int i = mList.size() - 1; i >= 0; --i) {
            AdapterItem item = mList.get(i);
            if (item == adapterItem) {
                notifyItemChanged(i);
                return i;
            }
        }

        return 0;
    }

    private int addDeviceToList(Credentials credentials, int startIndex, DataService dataService) {
        mList.add(startIndex++, new AdapterItemHeader(credentials, dataService, IOConnectionAdapter.this));
        if (showConnections && credentials.isConfigured()) {
            DeviceIOConnections deviceIOConnections = dataService.connections.openDevice(credentials.deviceUID);
            if (deviceIOConnections != null) {
                for (Iterator<IOConnection> iterator = deviceIOConnections.iterator(); iterator.hasNext(); ) {
                    IOConnection ioConnection = iterator.next();
                    mList.add(startIndex++, new AdapterItemConnection(ioConnection, dataService, IOConnectionAdapter.this));

                }
                return 1 + deviceIOConnections.size(); // amount of connections plus the device itself
            }
        }

        return 1;
    }

    public AdapterItem getItem(int position) {
        return mList.get(position);
    }

    @Override
    public boolean onServiceReady(DataService service) {
        service.credentials.registerObserver(credentialsUpdater);
        service.connections.registerObserver(ioConnectionUpdater);

        fullUpdate(service);

        return false;
    }

    @Override
    public void onServiceFinished(DataService service) {
        service.credentials.unregisterObserver(credentialsUpdater);
        service.connections.unregisterObserver(ioConnectionUpdater);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView title;
        public TextView subtitle;
        public ImageView image;
        public AdapterItem credentials;

        public ViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.title);
            subtitle = (TextView) itemView.findViewById(R.id.subtitle);
            image = (ImageView) itemView.findViewById(R.id.device_connection_reachable);
        }

        public void updateData(AdapterItem credentials) {
            this.credentials = credentials;
        }
    }

    class CredentialsUpdater implements onCollectionUpdated<CredentialsCollection, Credentials> {
        @Override
        public boolean updated(@NonNull CredentialsCollection credentialsCollection, @Nullable Credentials credentials, @NonNull ObserverUpdateActions action) {
            if (credentials == null) return true;

            switch (action) {
                case AddAction:
                    if (!showNewDevices && !credentials.isConfigured()) break;
                    boolean empty = mList.isEmpty();
                    addDeviceToList(credentials, mList.size(), credentialsCollection.dataService);
                    if (empty) emptyListener.onEmptyListener(false);
                    break;
                case ClearAndNewAction:
                case RemoveAllAction:
                    fullUpdate(credentialsCollection.dataService);
                    break;
                case RemoveAction:
                    for (int i = mList.size() - 1; i >= 0; --i) {
                        AdapterItem item = mList.get(i);
                        if (item.matchesDeviceUID(credentials.deviceUID)) {
                            item.destroy();
                            mList.remove(i);
                            notifyItemRemoved(i);
                            if (mList.isEmpty()) emptyListener.onEmptyListener(true);
                        }
                    }
                    break;
            }
            return true;
        }
    }

    class IOConnectionUpdater implements onCollectionUpdated<IOConnectionsCollection, IOConnection> {
        @Override
        public boolean updated(@NonNull IOConnectionsCollection ioConnectionsCollection, @Nullable IOConnection ioConnection, @NonNull ObserverUpdateActions action) {
            if (ioConnection == null) return true;

            switch (action) {
                case AddAction:
                    for (int i = mList.size() - 1; i >= 0; --i) {
                        AdapterItem item = mList.get(i);
                        if (item.matchesDeviceUID(ioConnection.deviceUID)) {
                            mList.add(i + 1, new AdapterItemConnection(ioConnection, ioConnectionsCollection.dataService, IOConnectionAdapter.this));
                            notifyItemInserted(i + 1);
                            return true;
                        }
                    }
                    break;
                case RemoveAction:
                    for (int i = mList.size() - 1; i >= 0; --i) {
                        AdapterItem item = mList.get(i);
                        if (item.matchesUID(ioConnection.getUid())) {
                            item.destroy();
                            mList.remove(i);
                            notifyItemRemoved(i);
                            return true;
                        }
                    }
                    break;
            }
            return true;
        }
    }
}
