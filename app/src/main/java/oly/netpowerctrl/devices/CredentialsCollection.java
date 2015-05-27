package oly.netpowerctrl.devices;

import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import oly.netpowerctrl.data.AbstractBasePlugin;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.data.storage_container.CollectionMapItems;
import oly.netpowerctrl.data.storage_container.CollectionOtherThreadPut;
import oly.netpowerctrl.data.storage_container.CollectionOtherThreadPutHandler;
import oly.netpowerctrl.ioconnection.DeviceIOConnections;
import oly.netpowerctrl.utils.ObserverUpdateActions;

/**
 * Contains DeviceInfos. Used for NFC and backup transfers
 */
public class CredentialsCollection extends CollectionMapItems<CredentialsCollection, Credentials> implements CollectionOtherThreadPut<Credentials> {
    public CollectionOtherThreadPutHandler updateDeviceHandler = new CollectionOtherThreadPutHandler<>(new WeakReference<CollectionOtherThreadPut<Credentials>>(this));

    //private static final String TAG = "CredentialsCollection";
    public CredentialsCollection(DataService dataService) {
        super(dataService, "credentials");
    }

    /**
     * Add a new device to this collection. If there is a device with the same unique id
     * it will be overwritten.
     *
     * @param credentials The new device to add (or replace an existing one).
     */
    public void put(Credentials credentials) {
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            updateDeviceHandler.obtainMessage(0, credentials).sendToTarget();
            return;
        }

        Credentials existed = items.get(credentials.getUid());
        if (existed != null) {
            boolean changed = credentials != existed;
            // If credentials object has changed, update credentials object in all connections
            if (changed) {
                DeviceIOConnections deviceIOConnections = dataService.connections.openDevice(credentials.deviceUID);
                if (deviceIOConnections != null)
                    deviceIOConnections.applyCredentials(credentials);
                dataService.executables.applyCredentials(credentials);
            }
            changed |= credentials.hasChanged();
            if (!changed) return;
            items.put(credentials.getUid(), credentials);
            if (credentials.isConfigured())
                storage.save(credentials);
            notifyObservers(credentials, ObserverUpdateActions.UpdateAction);
            return;
        }

        items.put(credentials.deviceUID, credentials);
        notifyObservers(credentials, ObserverUpdateActions.AddAction);
        if (credentials.isConfigured())
            storage.save(credentials);
    }

    public void remove(Credentials credentials) {
        storage.remove(credentials);
        items.remove(credentials.getUid());
        notifyObservers(credentials, ObserverUpdateActions.RemoveAction);
    }

    /**
     * Get credential for a given device unique id.
     * The complexity of this method is O(n), but for a very small number of devices (<3) this is
     * the best solution compared to hash or maps.
     *
     * @param uid The device unique id.
     * @return The credentials object.
     */
    @Nullable
    public Credentials findByUID(String uid) {
        return items.get(uid);
    }

    @NonNull
    public List<Credentials> findByPlugin(AbstractBasePlugin abstractBasePlugin) {
        List<Credentials> list = new ArrayList<>();
        for (Credentials di : items.values()) {
            // Mark all devices as changed: If network reduced mode ends all
            // devices propagate changes then.
            if (abstractBasePlugin.equals(di.getPlugin())) {
                list.add(di);
            }
        }
        return list;
    }

    public int countNotConfigured() {
        int i = 0;
        for (Credentials credentials : items.values())
            if (!credentials.isConfigured()) ++i;
        return i;
    }

    public int countConfigured() {
        int i = 0;
        for (Credentials credentials : items.values())
            if (credentials.isConfigured()) ++i;
        return i;
    }

    /**
     * Clear all credentials where the object is in the non-configured state.
     */
    public void clearNotConfigured() {
        for (Iterator<Credentials> iterator = items.values().iterator(); iterator.hasNext(); ) {
            Credentials credentials = iterator.next();
            if (!credentials.isConfigured()) {
                dataService.connections.remove(credentials);
                dataService.executables.remove(credentials);
                iterator.remove();
                notifyObservers(credentials, ObserverUpdateActions.RemoveAction);
            }
        }
    }
}
