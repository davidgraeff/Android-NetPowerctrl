package oly.netpowerctrl.devices;

import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

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
    private int receivedPackets = 0;

    public DeviceConnection(Device device) {
        this.device = device;
    }

    public void setReceiveAddress(InetAddress inetAddress) {
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

    public void setNotReachable(String not_reachable_reason) {
        this.not_reachable_reason = not_reachable_reason;
        if (not_reachable_reason != null)
            receivedPackets = 0;
    }

    public void setReachable() {
        not_reachable_reason = null;
    }

    public String getString() {
        return getProtocol() + "/" + mHostName + ":" + String.valueOf(getDestinationPort());
    }

    public boolean needResolveName() {
        return cached_addresses == null;
    }

    public void lookupIPs() throws UnknownHostException {
        cached_addresses = InetAddress.getAllByName(mHostName);
        receivedPackets = 0;
    }

    // This has to be executed in another thread not the gui thread!
    public InetAddress[] getHostnameIPs() {
        if (cached_addresses == null || cached_addresses.length == 0) {
            try {
                cached_addresses = InetAddress.getAllByName(mHostName);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                cached_addresses = new InetAddress[0];
            }
        }
        return cached_addresses;
    }

    public boolean hasAddress(InetAddress[] addresses) {
        getHostnameIPs();
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

    public abstract boolean fromJSON(JsonReader reader, boolean beginObjectAlreadyCalled) throws IOException, ClassNotFoundException;

    public abstract int getDestinationPort();

    public abstract String getProtocol();

    public abstract boolean equals(DeviceConnection deviceConnection);

    public abstract boolean equalsByDestinationAddress(DeviceConnection otherConnection);
}
