package oly.netpowerctrl.ioconnection.adapter;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.ioconnection.IOConnection;
import oly.netpowerctrl.ioconnection.IOConnectionsCollection;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.network.ReachabilityStates;
import oly.netpowerctrl.utils.ObserverUpdateActions;
import oly.netpowerctrl.utils.onCollectionUpdated;

/**
 * Created by david on 25.04.15.
 */
public class AdapterItemConnection extends AdapterItem implements onCollectionUpdated<IOConnectionsCollection, IOConnection> {
    public IOConnection ioConnection;
    public String subtitle;

    /**
     * Create an entry for a device connection
     *
     * @param ioConnection The connection to get data from.
     */
    public AdapterItemConnection(IOConnection ioConnection, final DataService service, IOConnectionAdapter adapter) {
        super(adapter);
        this.title = "";
        this.title += ioConnection.getProtocol() + "/" + ioConnection.getDestinationHost();
        if (ioConnection.getDestinationPort() != -1)
            this.title += ":" + String.valueOf(ioConnection.getDestinationPort());

        this.ioConnection = ioConnection;
        this.credentials = ioConnection.credentials;
        this.UID = ioConnection.getUid();

        updateSubtitle();

        this.deviceUID = ioConnection.deviceUID;
        this.isConfigured = ioConnection.credentials.isConfigured();

        /**
         * The following is done in a runnable, because AdapterItemConnection uses IOConnectionCollection.registerObserver and
         * the update method where that happens is called by the observer list. The result would be a concurrent access to the observer list.
         */
        App.getMainThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                service.connections.registerObserver(AdapterItemConnection.this);
            }
        });
    }

    public ReachabilityStates reachableState() {
        return ioConnection.reachableState();
    }

    @Override
    public void destroy() {
        App.getMainThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                DataService.getService().connections.unregisterObserver(AdapterItemConnection.this);
            }
        });
    }

    private void updateSubtitle() {
        subtitle = ioConnection.getStatusMessage();
        if (subtitle == null) {
            if (this.ioConnection.reachableState() == ReachabilityStates.MaybeReachable) {
                subtitle = App.getAppString(R.string.device_connection_notTested);
            } else
                subtitle = App.getAppString(R.string.device_reachable);
        }
    }

    @Override
    public String getSubtitle() {
        return subtitle;
    }

    @Override
    public boolean updated(@NonNull IOConnectionsCollection ioConnectionsCollection, @Nullable IOConnection ioConnection, @NonNull ObserverUpdateActions action) {
        if ((action == ObserverUpdateActions.UpdateAction || action == ObserverUpdateActions.UpdateReachableAction) && this.ioConnection.equals(ioConnection)) {
            this.ioConnection = ioConnection;
            updateSubtitle();
            last_known_position = adapter.notifyItemChanged(this, last_known_position);
        }

        return true;
    }
}
