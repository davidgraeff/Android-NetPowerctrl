package oly.netpowerctrl.ioconnection.adapter;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.devices.Credentials;
import oly.netpowerctrl.devices.CredentialsCollection;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.utils.ObserverUpdateActions;
import oly.netpowerctrl.utils.onCollectionUpdated;

/**
 * Created by david on 25.04.15.
 */
public class AdapterItemHeader extends AdapterItem implements onCollectionUpdated<CredentialsCollection, Credentials> {

    public AdapterItemHeader(Credentials credentials, DataService service, IOConnectionAdapter adapter) {
        super(adapter);
        if (!credentials.isConfigured())
            this.title = App.getAppString(R.string.device_new, credentials.getDeviceName());
        else
            this.title = credentials.getDeviceName();

        this.subtitle = "";
        this.credentials = credentials;
        this.isConfigured = credentials.isConfigured();
        this.deviceUID = credentials.deviceUID;
        this.UID = credentials.getUid();

        service.credentials.registerObserver(this);

    }

    @Override
    public void destroy() {
        DataService.getService().credentials.unregisterObserver(this);
    }

    @Override
    public boolean updated(@NonNull CredentialsCollection credentialsCollection, @Nullable Credentials credentials, @NonNull ObserverUpdateActions action) {
        if ((action == ObserverUpdateActions.UpdateAction || action == ObserverUpdateActions.UpdateReachableAction) && this.credentials.equals(credentials)) {
            this.credentials = credentials;
            last_known_position = adapter.notifyItemChanged(this, last_known_position);
        }

        return true;
    }
}
