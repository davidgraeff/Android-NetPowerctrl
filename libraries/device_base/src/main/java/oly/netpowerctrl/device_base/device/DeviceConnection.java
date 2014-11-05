package oly.netpowerctrl.device_base.device;

import android.support.annotation.NonNull;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import oly.netpowerctrl.device_base.data.JSONHelper;

/**
 * Created by david on 28.07.14.
 */
public abstract class DeviceConnection {
    public final Device device;
    public String mHostName;
    public String not_reachable_reason;
    protected boolean mIsAssignedByDevice = false;
    // Cache
    protected InetAddress[] cached_addresses;
    protected int receivedPackets = 0;

    public DeviceConnection(Device device) {
        this.device = device;
    }

    public void setReceiveAddress(@NonNull InetAddress inetAddress) {
        cached_addresses = new InetAddress[1];
        cached_addresses[0] = inetAddress;
    }

    public String getDestinationHost() {
        return mHostName;
    }

    public Device getDevice() {
        return device;
    }

    public void connectionUsed() {
        ++receivedPackets;
    }

    public boolean isReachable() {
        return receivedPackets > 0;
    }

    public String getNotReachableReason() {
        return not_reachable_reason;
    }

    void setStatusMessage(String not_reachable_reason, boolean clearReachability) {
        this.not_reachable_reason = not_reachable_reason;
        if (clearReachability)
            receivedPackets = 0;
    }

    public void clearStatusMessage() {
        not_reachable_reason = null;
    }

    public String getString() {
        return getProtocol() + "/" + mHostName + ":" + String.valueOf(getDestinationPort());
    }

    public boolean needResolveName() {
        return cached_addresses == null;
    }

    public void lookupIPs() throws UnknownHostException {
        receivedPackets = 0;
        // do the lookup (exception may occur)
        cached_addresses = InetAddress.getAllByName(mHostName);
    }

    // This has to be executed in another thread not the gui thread if lookupDNSName is set.
    public InetAddress[] getHostnameIPs(boolean lookupDNSName) {
        if (cached_addresses == null || cached_addresses.length == 0) {
            if (lookupDNSName)
                try {
                    cached_addresses = InetAddress.getAllByName(mHostName);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    cached_addresses = new InetAddress[0];
                }
            else
                cached_addresses = new InetAddress[0];
        }
        return cached_addresses;
    }

    // This has to be executed in another thread not the gui thread if lookupDNSName is set.
    public boolean hasAddress(InetAddress[] addresses, boolean lookupDNSName) {
        getHostnameIPs(lookupDNSName);
        if (cached_addresses != null) {
            for (InetAddress local_address : cached_addresses)
                for (InetAddress other_address : addresses)
                    if (local_address.equals(other_address))
                        return true;
        }
        return false;
    }

    public boolean isAssignedByDevice() {
        return mIsAssignedByDevice;
    }

    public void setIsAssignedByDevice(boolean mIsCustom) {
        this.mIsAssignedByDevice = mIsCustom;
    }

    public abstract void toJSON(JsonWriter writer) throws IOException;

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

    public abstract boolean fromJSON(@NonNull JsonReader reader, boolean beginObjectAlreadyCalled) throws IOException, ClassNotFoundException;

    public abstract int getDestinationPort();

    public abstract String getProtocol();

    public abstract boolean equals(@NonNull DeviceConnection deviceConnection);

    public abstract boolean equalsByDestinationAddress(DeviceConnection otherConnection, boolean lookupDNSName);
}
