package oly.netpowerctrl.ioconnection;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import oly.netpowerctrl.devices.Credentials;
import oly.netpowerctrl.network.ReachabilityStates;
import oly.netpowerctrl.utils.ObserverUpdateActions;

/**
 * Represents connections for one device
 */
public class DeviceIOConnections {
    final String deviceUID;
    private Map<String, IOConnection> connections = new TreeMap<>();
    private IOConnection cached_reachable = null;

    public DeviceIOConnections(String deviceUID) {
        this.deviceUID = deviceUID;
    }

    public ObserverUpdateActions putConnection(IOConnection ioConnection) {
        // We assume that the given connection is already added, no connectivity change happened and
        // no data change happened (result == null)
        ObserverUpdateActions result = null;

        // Try to find a connection with the same connection unique id
        IOConnection existing = connections.get(ioConnection.getUid());
        // Put the given connection into the connection list now (doesn't matter if we have found the object already)
        connections.put(ioConnection.getUid(), ioConnection);

        if (existing == null) {
            result = ObserverUpdateActions.AddAction;
        } else if (ioConnection.hasChanged()) {
            result = ObserverUpdateActions.UpdateAction;
        }

        return result;
    }

    public int size() {
        return connections.size();
    }

    public Iterator<IOConnection> iterator() {
        return connections.values().iterator();
    }

    /**
     * Determine set of hostnames and start a lookup
     *
     * @return Return true if lookup has been successful or if nothing to do.
     */
    public boolean lookupIP() {
        if (connections.isEmpty()) return true;
        IOConnection connection = connections.values().iterator().next();
        if (!connection.needResolveName()) return true;

        try {
            connection.lookupIPs();
            return true;
        } catch (UnknownHostException e) {
            setStatusMessage(e.getLocalizedMessage());
            setReachability(ReachabilityStates.NotReachable);
            return false;
        }
    }

    public void setStatusMessage(@NonNull String not_reachable_reason) {
        for (IOConnection ioConnection : connections.values()) {
            ioConnection.setStatusMessage(not_reachable_reason);
        }
    }

    public void setReachability(ReachabilityStates reachable) {
        for (IOConnection ioConnection : connections.values()) {
            ioConnection.setReachability(reachable);
        }
    }


    public IOConnection findByUID(String uid) {
        return connections.get(uid);
    }

    void compute_reachability() {
        if (cached_reachable != null && cached_reachable.reachableState() != ReachabilityStates.NotReachable)
            return;

        for (IOConnection existing : connections.values()) {
            if (existing.reachableState() != ReachabilityStates.NotReachable) {
                cached_reachable = existing;
                return;
            }
        }
    }

    @Nullable
    public IOConnection findReachable() {
        return cached_reachable;
    }

    public IOConnection findReachable(String protocol) {
        for (IOConnection existing : connections.values()) {
            if (existing.getProtocol().equals(protocol) && existing.reachableState() != ReachabilityStates.NotReachable) {
                return existing;
            }
        }
        return null;
    }

    public boolean remove(IOConnection ioConnection) {
        if (cached_reachable != null && cached_reachable.getUid().equals(ioConnection.getUid()))
            cached_reachable = null;

        for (Iterator<String> iterator = connections.keySet().iterator(); iterator.hasNext(); ) {
            String uid = iterator.next();
            if (ioConnection.getUid().equals(uid)) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    public void applyCredentials(Credentials credentials) {
        for (IOConnection existing : connections.values()) {
            existing.credentials = credentials;
        }
    }

//    public ReachabilityStates reachableState() {
//        ReachabilityStates r = ReachabilityStates.NotReachable;
//        return r;
//    }

}
