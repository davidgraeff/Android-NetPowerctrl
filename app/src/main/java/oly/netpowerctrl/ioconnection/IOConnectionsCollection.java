package oly.netpowerctrl.ioconnection;

import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import oly.netpowerctrl.App;
import oly.netpowerctrl.data.AbstractBasePlugin;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.data.storage_container.CollectionManipulation;
import oly.netpowerctrl.data.storage_container.CollectionMapItems;
import oly.netpowerctrl.data.storage_container.CollectionObserver;
import oly.netpowerctrl.data.storage_container.CollectionOtherThreadPut;
import oly.netpowerctrl.data.storage_container.CollectionOtherThreadPutHandler;
import oly.netpowerctrl.data.storage_container.CollectionStorage;
import oly.netpowerctrl.devices.Credentials;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.network.ReachabilityStates;
import oly.netpowerctrl.utils.ObserverUpdateActions;

/**
 * Contains device connections. This collection class does not use the {@link CollectionMapItems} base class,
 * but implements everything on its own to support the hierarchical structure of DeviceIOConnections->IOConnections.
 */
public class IOConnectionsCollection extends CollectionObserver<IOConnectionsCollection, IOConnection>
        implements CollectionManipulation<IOConnection>, CollectionOtherThreadPut<IOConnection> {
    final public CollectionStorage<IOConnection> storage;
    final public DataService dataService;
    public CollectionOtherThreadPutHandler<IOConnection> updateDeviceHandler = new CollectionOtherThreadPutHandler<>(new WeakReference<CollectionOtherThreadPut<IOConnection>>(this));
    protected Map<String, DeviceIOConnections> items = new TreeMap<>();

    public IOConnectionsCollection(DataService dataService) {
        this.dataService = dataService;
        storage = new CollectionStorage<IOConnection>() {
            @Override
            public void clear() {
                items.clear();
            }

            @Override
            public void addWithoutSave(IOConnection item) throws ClassNotFoundException {
                DeviceIOConnections deviceIOConnections = items.get(item.deviceUID);
                if (deviceIOConnections == null) {
                    deviceIOConnections = new DeviceIOConnections(item.deviceUID);
                    items.put(item.deviceUID, deviceIOConnections);
                }
                deviceIOConnections.putConnection(item);
            }

            @Override
            public String type() {
                return "connections";
            }
        };
    }

    //private static final String TAG = "IOConnectionCollection";

    @Override
    public CollectionStorage<IOConnection> getStorage() {
        return storage;
    }

    private void put_test(IOConnection ioConnection) {
        if (ioConnection.getUid() == null || ioConnection.credentials == null)
            throw new RuntimeException();
    }

    /**
     * Call this to add or change an ioconnection and to notify observers about the change.
     * This may called from the gui thread and other threads.
     */
    public void put(IOConnection ioConnection) {
        put_test(ioConnection);

        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            updateDeviceHandler.obtainMessage(0, ioConnection).sendToTarget();
            return;
        }

        DeviceIOConnections deviceIOConnections = items.get(ioConnection.deviceUID);
        if (deviceIOConnections == null) {
            deviceIOConnections = new DeviceIOConnections(ioConnection.deviceUID);
            items.put(ioConnection.deviceUID, deviceIOConnections);
        }

        putInternal(deviceIOConnections, ioConnection);
    }

    private void putInternal(DeviceIOConnections deviceIOConnections, IOConnection ioConnection) {
        ObserverUpdateActions updateActions = null;

        if (ioConnection.isReachabilityChanged()) {
            // Store new reachability, reset isReachabilityChanged flag
            ioConnection.storeReachability();
            // First compute reachability while isReachabilityChanged is still set to true.
            boolean reachabilityChanged = deviceIOConnections.compute_reachability();
            // This is at least an UpdateReachableAction now
            updateActions = ObserverUpdateActions.UpdateReachableAction;
            // Make executables aware of the change
            if (reachabilityChanged)
                dataService.executables.notifyReachability(ioConnection.deviceUID, deviceIOConnections.reachableState());
        }

        if (ioConnection.hasChanged()) {
            ioConnection.resetChanged();

            if (ioConnection.credentials.isConfigured())
                storage.save(ioConnection);

            updateActions = deviceIOConnections.putConnection(ioConnection);
        }

        // This is for notifying the DeviceQuery. We always have to do that regardless of a
        // connectivity change of the connection.
        dataService.notifyOfUpdatedDevice(ioConnection);

        if (updateActions != null) {
            notifyObservers(ioConnection, updateActions);
        }
    }

    public void put(final DeviceIOConnections deviceIOConnections) {
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            App.getMainThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    put(deviceIOConnections);
                }
            });
            return;
        }

        for (Iterator<IOConnection> iterator = deviceIOConnections.iterator(); iterator.hasNext(); ) {
            IOConnection ioConnection = iterator.next();
            putInternal(deviceIOConnections, ioConnection);
        }
    }

    /**
     * Save all connections that belong to the same device ID as the given credential object.
     * This will not issue updated notifications even if the objects have changed since last save.
     */
    public void save(String deviceUID) {
        DeviceIOConnections deviceIOConnections = openDevice(deviceUID);
        if (deviceIOConnections == null) return;

        for (Iterator<IOConnection> iterator = deviceIOConnections.iterator(); iterator.hasNext(); ) {
            IOConnection ioConnection = iterator.next();
            if (ioConnection.deviceUID.equals(deviceIOConnections.deviceUID)) {
                storage.save(ioConnection);
            }
        }
    }

    public void remove(IOConnection ioConnection) {
        DeviceIOConnections deviceIOConnections = items.get(ioConnection.deviceUID);
        if (deviceIOConnections != null) {
            if (deviceIOConnections.remove(ioConnection)) {
                storage.remove(ioConnection);
                notifyObservers(ioConnection, ObserverUpdateActions.RemoveAction);
            }
        }
    }

    public void removeOrphaned() {
        for (Iterator<Map.Entry<String, DeviceIOConnections>> iterator = items.entrySet().iterator(); iterator.hasNext(); ) {
            DeviceIOConnections deviceIOConnections = iterator.next().getValue();
            for (Iterator<IOConnection> connectionIterator = deviceIOConnections.iterator(); connectionIterator.hasNext(); ) {
                IOConnection ioConnection = connectionIterator.next();
                if (ioConnection.getCredentials() != null) continue;
                storage.remove(ioConnection);
                connectionIterator.remove();
            }
            if (deviceIOConnections.size() == 0)
                iterator.remove();
        }
    }

    /**
     * Remove all IOConnections belonging to this deviceUID.
     */
    public void remove(Credentials credentials) {
        DeviceIOConnections deviceIOConnections = items.get(credentials.deviceUID);
        if (deviceIOConnections == null) return;

        for (Iterator<IOConnection> iterator = deviceIOConnections.iterator(); iterator.hasNext(); ) {
            IOConnection ioConnection = iterator.next();
            storage.remove(ioConnection);
            notifyObservers(ioConnection, ObserverUpdateActions.RemoveAction);
        }

        items.remove(credentials.deviceUID);
    }

    /**
     * Clwar all connections where the credentials object is in the non-configured state.
     * Connections are removed from disk, too if applicable.
     */
    public void clearNotConfigured() {
        for (DeviceIOConnections deviceIOConnections : items.values())
            for (Iterator<IOConnection> iterator = deviceIOConnections.iterator(); iterator.hasNext(); ) {
                IOConnection ioConnection = iterator.next();
                if (!ioConnection.credentials.isConfigured()) {
                    iterator.remove();
                    notifyObservers(ioConnection, ObserverUpdateActions.RemoveAction);
                }
            }
    }

    @Nullable
    public DeviceIOConnections openDevice(String deviceUID) {
        return items.get(deviceUID);
    }

    @NonNull
    public DeviceIOConnections openCreateDevice(String deviceUID) {
        DeviceIOConnections deviceIOConnections = items.get(deviceUID);
        if (deviceIOConnections == null) {
            deviceIOConnections = new DeviceIOConnections(deviceUID);
            items.put(deviceUID, deviceIOConnections);
        }
        return deviceIOConnections;
    }

    public int getRecentlyDetectedDevices(boolean configured, long lastSeenInMS) {
        Set<String> s = new TreeSet<>();
        for (DeviceIOConnections deviceIOConnections : items.values())
            for (Iterator<IOConnection> iterator = deviceIOConnections.iterator(); iterator.hasNext(); ) {
                IOConnection ioConnection = iterator.next();
                if (ioConnection.credentials.isConfigured() == configured) {
                    long t = System.currentTimeMillis() - ioConnection.getLastUsed();
                    if (t < lastSeenInMS)
                        s.add(ioConnection.deviceUID);
                }
            }
        return s.size();
    }

    public ReachabilityStates getReachableState(Executable executable) {
        DeviceIOConnections deviceIOConnections = openDevice(executable.deviceUID);
        if (deviceIOConnections == null) return ReachabilityStates.NotReachable;
        IOConnection ioConnection = deviceIOConnections.findReachable();
        if (ioConnection == null) return ReachabilityStates.NotReachable;
        return ioConnection.reachableState();
    }

    //! get a list of all send ports of all configured devices plus the default send port
    public Set<Integer> getAllUDPSendPorts(AbstractBasePlugin plugin) {
        HashSet<Integer> ports = new HashSet<>();

        for (DeviceIOConnections deviceIOConnections : items.values()) {
            for (Iterator<IOConnection> iterator = deviceIOConnections.iterator(); iterator.hasNext(); ) {
                IOConnection ci = iterator.next();
                if (ci.credentials.getPlugin() == plugin && ci instanceof IOConnectionUDP)
                    ports.add(ci.getDestinationPort());
            }
        }
        return ports;
    }

    //! get a list of all receive ports of all configured devices plus the default receive port
    public Set<Integer> getAllUDPReceivePorts(AbstractBasePlugin plugin) {
        Set<Integer> ports = new HashSet<>();

        for (DeviceIOConnections deviceIOConnections : items.values()) {
            for (Iterator<IOConnection> iterator = deviceIOConnections.iterator(); iterator.hasNext(); ) {
                IOConnection ci = iterator.next();
                if (ci.credentials.getPlugin() == plugin && ci instanceof IOConnectionUDP)
                    ports.add(((IOConnectionUDP) ci).getListenPort());
            }
        }
        return ports;
    }

    public void applyStateToAll(ReachabilityStates state) {
        for (DeviceIOConnections deviceIOConnections : items.values()) {
            for (Iterator<IOConnection> iterator = deviceIOConnections.iterator(); iterator.hasNext(); ) {
                IOConnection ioConnection = iterator.next();
                ioConnection.resetChanged();
                ioConnection.setReachability(state);
                putInternal(deviceIOConnections, ioConnection);
            }
        }
    }
}
