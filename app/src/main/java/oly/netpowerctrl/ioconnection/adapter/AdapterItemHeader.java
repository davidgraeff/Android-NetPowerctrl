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

    public AdapterItemHeader(Credentials credentials, final DataService service, IOConnectionAdapter adapter) {
        super(adapter);
        if (!credentials.isConfigured())
            this.title = App.getAppString(R.string.device_new, credentials.getDeviceName());
        else
            this.title = credentials.getDeviceName();

        this.credentials = credentials;
        this.isConfigured = credentials.isConfigured();
        this.deviceUID = credentials.deviceUID;
        this.UID = credentials.getUid();

        /**
         * The following is done in a runnable, because AdapterItemConnection uses IOConnectionCollection.registerObserver and
         * the update method where that happens is called by the observer list. The result would be a concurrent access to the observer list.
         */
        App.getMainThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                service.credentials.registerObserver(AdapterItemHeader.this);
            }
        });

    }

    @Override
    public void destroy() {
        DataService.getService().credentials.unregisterObserver(this);
    }

    @Override
    public String getSubtitle() {
        return "";
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
