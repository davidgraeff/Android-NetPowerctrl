package oly.netpowerctrl.ioconnection;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

import oly.netpowerctrl.credentials.Credentials;
import oly.netpowerctrl.network.ReachabilityStates;
import oly.netpowerctrl.utils.IOInterface;
import oly.netpowerctrl.utils.JSONHelper;

/**
 * Created by david on 28.07.14.
 */
public abstract class IOConnection implements IOInterface {
    public Credentials credentials;
    public String deviceUID;
    public String connectionUID; // e.g. mac+protocol+port. for self configured connections use UUID.random()
    public String hostName;
    protected String statusMessage;
    // Cache
    protected InetAddress[] cached_addresses;
    protected int receivedPackets = -1;
    // Cache: Hash value for all member variables (except "Cache"). Recomputed after save to disk.
    private int lastChangedCode = 0;
    // Cache: Last reachable state. Updated at call of collection->put(this_connection)
    private int lastReachableStateCode = 0;
    private long lastUsed = 0;

    public IOConnection(@Nullable Credentials credentials) {
        this.credentials = credentials;
        if (this.credentials != null)
            this.deviceUID = credentials.deviceUID;
    }

//    public boolean equalsByDestinationAddress(IOConnection otherConnection, boolean lookupDNSName) {
//        return cached_addresses == null ? hostName.equals(otherConnection.hostName) : hasAddress(otherConnection.getHostnameIPs(lookupDNSName), lookupDNSName);
//    }

    public void setReceiveAddress(@NonNull InetAddress inetAddress) {
        cached_addresses = new InetAddress[1];
        cached_addresses[0] = inetAddress;
    }

    public String getDestinationHost() {
        return hostName;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public void incReceivedPackets() {
        if (receivedPackets == -1)
            receivedPackets = 1;
        else
            ++receivedPackets;
        lastUsed = System.currentTimeMillis();
    }

    public ReachabilityStates reachableState() {
        if (receivedPackets == 0)
            return ReachabilityStates.MaybeReachable;
        else if (receivedPackets > 0)
            return ReachabilityStates.Reachable;
        else
            return ReachabilityStates.NotReachable;
    }

    public boolean isReachabilityChanged() {
        return lastReachableStateCode != reachableState().ordinal() + (statusMessage == null ? 0 : statusMessage.hashCode());
    }

    public void storeReachability() {
        lastReachableStateCode = reachableState().ordinal() + (statusMessage == null ? 0 : statusMessage.hashCode());
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(@Nullable String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public void setReachability(ReachabilityStates reachability) {
        switch (reachability) {
            case MaybeReachable:
                receivedPackets = 0;
                break;
            case NotReachable:
                receivedPackets = -1;
                break;
            case Reachable:
                receivedPackets = 1;
                break;
        }
    }

    public boolean needResolveName() {
        return cached_addresses == null;
    }

    /**
     * Use this connection immediately after this method. This connection is marked not reachable
     * but the state is not propagated to the connection manager!
     * Use this method not in the main/gui thread!
     *
     * @throws UnknownHostException
     */
    public void lookupIPs() throws UnknownHostException {
        receivedPackets = -1;
        // do the lookup (exception may occur)
        cached_addresses = InetAddress.getAllByName(hostName);
    }

    // This has to be executed in another thread not the gui thread if lookupDNSName is set.
//    public InetAddress[] getHostnameIPs(boolean lookupDNSName) {
//        if (cached_addresses == null || cached_addresses.length == 0) {
//            if (lookupDNSName)
//                try {
//                    cached_addresses = InetAddress.getAllByName(hostName);
//                } catch (UnknownHostException e) {
//                    e.printStackTrace();
//                    cached_addresses = new InetAddress[0];
//                }
//            else
//                cached_addresses = new InetAddress[0];
//        }
//        return cached_addresses;
//    }

    // This has to be executed in another thread not the gui thread if lookupDNSName is set.
//    public boolean hasAddress(InetAddress[] addresses, boolean lookupDNSName) {
//        getHostnameIPs(lookupDNSName);
//        if (cached_addresses != null) {
//            for (InetAddress local_address : cached_addresses)
//                for (InetAddress other_address : addresses)
//                    if (local_address.equals(other_address))
//                        return true;
//        }
//        return false;
//    }

    @Override
    public String getUid() {
        return connectionUID;
    }

    protected abstract void write(JsonWriter writer) throws IOException;

    private void toJSON(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("connection_type").value(getProtocol());
        writer.name("HostName").value(hostName);
        writer.name("CUID").value(connectionUID);
        writer.name("DUID").value(deviceUID);
        write(writer);
        writer.endObject();
        writer.close();
    }

    /**
     * Return the json representation of this connection
     *
     * @return JSON String
     */
    @Override
    public String toString() {
        try {
            JSONHelper h = new JSONHelper();
            toJSON(h.createWriter());
            return h.getString();
        } catch (IOException ignored) {
            return null;
        }
    }

    protected void read(@NonNull JsonReader reader, String name) throws IOException {
        reader.skipValue();
    }

    void load(@NonNull JsonReader reader, boolean noBeginObject) throws IOException, ClassNotFoundException {
        if (!noBeginObject) reader.beginObject();

        while (reader.hasNext()) {
            String name = reader.nextName();
            assert name != null;
            switch (name) {
                case "HostName":
                    hostName = reader.nextString();
                    break;
                case "CUID":
                    connectionUID = reader.nextString();
                    break;
                case "DUID":
                    deviceUID = reader.nextString();
                    break;
                default:
                    read(reader, name);
                    break;
            }
        }

        reader.endObject();
    }

    @Override
    public void load(@NonNull InputStream input) throws IOException, ClassNotFoundException {
        load(new JsonReader(new InputStreamReader(input)), false);
    }

    @Override
    public void save(@NonNull OutputStream output) throws IOException {
        toJSON(JSONHelper.createWriter(output));
    }

    protected abstract int computeHash();

    @Override
    public boolean hasChanged() {
        return lastChangedCode != computeHash();
    }

    @Override
    public void setHasChanged() {
        lastChangedCode = 0;
    }

    @Override
    public void resetChanged() {
        lastChangedCode = computeHash();
        lastUsed = System.currentTimeMillis();
    }

    public abstract String getProtocol();

    public final boolean equals(IOConnection otherConnection) {
        return connectionUID.equals(otherConnection.getUid());
    }

    public long getLastUsed() {
        return lastUsed;
    }
}
