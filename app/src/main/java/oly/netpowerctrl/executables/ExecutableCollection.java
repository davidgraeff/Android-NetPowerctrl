package oly.netpowerctrl.executables;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Looper;
import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import oly.netpowerctrl.credentials.Credentials;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.data.graphic.IconState;
import oly.netpowerctrl.data.graphic.LoadStoreIconData;
import oly.netpowerctrl.data.graphic.Utils;
import oly.netpowerctrl.data.storage_container.CollectionMapItems;
import oly.netpowerctrl.data.storage_container.CollectionOtherThreadPut;
import oly.netpowerctrl.data.storage_container.CollectionOtherThreadPutHandler;
import oly.netpowerctrl.network.ReachabilityStates;
import oly.netpowerctrl.utils.ObserverUpdateActions;

/**
 * Contains DeviceInfos. Used for NFC and backup transfers
 */
public class ExecutableCollection extends CollectionMapItems<ExecutableCollection, Executable> implements CollectionOtherThreadPut<Executable> {
    private static final String TAG = "ExecutableCollection";

    public CollectionOtherThreadPutHandler updateDeviceHandler = new CollectionOtherThreadPutHandler<>(new WeakReference<CollectionOtherThreadPut<Executable>>(this));

    public ExecutableCollection(DataService dataService) {
        super(dataService, "executables");
    }

    public void setExecutableBitmap(Context context, Executable executable, Bitmap bitmap, IconState state) {
        if (executable == null)
            return;

        LoadStoreIconData.saveIcon(context, Utils.resizeBitmap(context, bitmap, 128, 128), executable.getUid(), state);
        notifyObservers(executable, ObserverUpdateActions.UpdateAction);
    }

    @Nullable
    public Executable findByUID(String uid) {
        return items.get(uid);
    }

    private void put_test(Executable executable) {
        if (executable.getUid() == null || (executable.needCredentials() && executable.getCredentials() == null) || executable.title.isEmpty())
            throw new RuntimeException();
    }

    /**
     * Add a new device to this collection. If there is a device with the same unique id
     * it will be overwritten.
     *
     * @param executable The new device to add (or replace an existing one).
     */
    public void put(Executable executable) {
        put_test(executable);

        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            updateDeviceHandler.obtainMessage(0, executable).sendToTarget();
            return;
        }

        Executable existed = items.get(executable.getUid());
        if (existed != null) {
            if (executable == existed && !executable.hasChanged() && !executable.executionInProgress())
                return;

            executable.setExecutionInProgress(false);

            items.put(executable.getUid(), executable);
            if (!executable.needCredentials() || executable.getCredentials().isConfigured())
                storage.save(executable);
            notifyObservers(executable, ObserverUpdateActions.UpdateAction);
            return;
        } else
            executable.setExecutionInProgress(false);

        if (executable.isSaveable()) {
            items.put(executable.getUid(), executable);
            notifyObservers(executable, ObserverUpdateActions.AddAction);
            if (!executable.needCredentials() || executable.getCredentials().isConfigured())
                storage.save(executable);
        }
    }

    public void remove(Executable executable) {
        items.remove(executable.getUid());
        dataService.favourites.setFavourite(executable.getUid(), false);
        storage.remove(executable);
        notifyObservers(executable, ObserverUpdateActions.RemoveAction);
        executable.destroy(dataService);
    }

    /**
     * Remove all executables that belong to the given credentials.
     * This will also remove all favourites
     *
     * @param credentials The credentials
     */
    public void remove(Credentials credentials) {
        for (Iterator<Executable> iterator = items.values().iterator(); iterator.hasNext(); ) {
            Executable executable = iterator.next();
            if (executable.needCredentials() && executable.deviceUID.equals(credentials.deviceUID)) {
                iterator.remove();
                storage.remove(executable);
                dataService.favourites.setFavourite(executable.getUid(), false);
                notifyObservers(executable, ObserverUpdateActions.RemoveAction);
                executable.destroy(dataService);
            }
        }
    }

    public void removeOrphaned() {
        for (Iterator<Executable> iterator = items.values().iterator(); iterator.hasNext(); ) {
            Executable executable = iterator.next();
            if (executable.needCredentials() && executable.getCredentials() == null) {
                iterator.remove();
                dataService.favourites.setFavourite(executable.getUid(), false);
            }
        }
    }

    /**
     * This is called if IOConnections reachability and therefore device reachability changes. Because
     * Executables contain a member function for reachability we need to notify about reachability changes.
     *
     * @param deviceUID The device unique id
     * @param save
     */
    public void notifyReachability(String deviceUID, ReachabilityStates r, boolean save) {
        // Filter scenes
        for (Executable executable : items.values()) {
            if (!executable.needCredentials() || !executable.deviceUID.equals(deviceUID)) continue;
            executable.updateCachedReachability(r);
            if (executable.hasReachabilityChanged()) {
                executable.resetReachabilityHasChangedFlag();
                notifyObservers(executable, ObserverUpdateActions.UpdateReachableAction);
            }
            if (save)
                storage.save(executable);
        }
    }

    public void notifyReachability(Executable executable) {
        notifyObservers(executable, ObserverUpdateActions.UpdateReachableAction);
    }

    public void applyCredentials(Credentials credentials) {
        for (Executable executable : items.values()) {
            if (executable.needCredentials() && executable.deviceUID.equals(credentials.getUid())) {
                executable.setCredentials(credentials);
                executable.updateCachedReachability(dataService.connections.getReachableState(executable));
            }
        }
    }

    public List<Executable> filterExecutables(PredicateExecutable p) {
        List<Executable> list = new ArrayList<>();
        for (Executable executable : items.values()) {
            if (p.accept(executable)) list.add(executable);
        }

        return list;
    }

    public List<Executable> filterExecutables(Credentials credentials) {
        List<Executable> list = new ArrayList<>();
        for (Executable executable : items.values()) {
            if (executable.needCredentials() && executable.deviceUID.equals(credentials.deviceUID))
                list.add(executable);
        }

        return list;
    }

    public interface PredicateExecutable {
        boolean accept(Executable e);
    }
}
