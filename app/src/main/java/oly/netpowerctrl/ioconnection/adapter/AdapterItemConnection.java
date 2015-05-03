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

    /**
     * Create an entry for a device connection
     *
     * @param ioConnection The connection to get data from.
     */
    public AdapterItemConnection(IOConnection ioConnection, DataService service, IOConnectionAdapter adapter) {
        super(adapter);
        this.title = "";
        this.title += ioConnection.getProtocol() + "/" + ioConnection.getDestinationHost();
        if (ioConnection.getDestinationPort() != -1)
            this.title += ":" + String.valueOf(ioConnection.getDestinationPort());

        this.ioConnection = ioConnection;
        this.credentials = ioConnection.credentials;
        this.UID = ioConnection.getUid();

        this.subtitle = ioConnection.getStatusMessage();
        if (this.subtitle == null) {
            if (this.ioConnection.reachableState() == ReachabilityStates.MaybeReachable) {
                this.subtitle = App.getAppString(R.string.device_connection_notTested);
            } else
                this.subtitle = App.getAppString(R.string.device_reachable);
        }

        this.deviceUID = ioConnection.deviceUID;
        this.isConfigured = ioConnection.credentials.isConfigured();
        service.connections.registerObserver(this);
    }

    public ReachabilityStates reachableState() {
        return ioConnection.reachableState();
    }

    @Override
    public void destroy() {
        DataService.getService().connections.unregisterObserver(this);
    }

    @Override
    public boolean updated(@NonNull IOConnectionsCollection ioConnectionsCollection, @Nullable IOConnection ioConnection, @NonNull ObserverUpdateActions action) {
        if ((action == ObserverUpdateActions.UpdateAction || action == ObserverUpdateActions.UpdateReachableAction) && this.ioConnection.equals(ioConnection)) {
            this.ioConnection = ioConnection;
            last_known_position = adapter.notifyItemChanged(this, last_known_position);
        }

        return true;
    }
}
