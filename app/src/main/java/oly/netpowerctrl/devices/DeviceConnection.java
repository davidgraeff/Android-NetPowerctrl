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
    public String HostName;
    public String not_reachable_reason;
    public boolean updatedFlag;
    protected boolean mIsAssignedByDevice = false;
    // Cache
    private InetAddress[] cached_addresses;

    public DeviceConnection(Device device) {
        this.device = device;
    }

    public String getDestinationHost() {
        return HostName;
    }

    public Device getDevice() {
        return device;
    }

    public boolean isReachable() {
        return not_reachable_reason == null;
    }

    public String getNotReachableReason() {
        return not_reachable_reason;
    }

    public void setNotReachable(String not_reachable_reason) {
        this.not_reachable_reason = not_reachable_reason;
    }

    public void setReachable() {
        not_reachable_reason = null;
    }

    public String getString() {
        return getProtocol() + "/" + HostName + ":" + String.valueOf(getDestinationPort());
    }

    public boolean needResolveName() {
        return cached_addresses == null;
    }

    // This has to be executed in another thread not the gui thread!
    public InetAddress[] getHostnameIPs() {
        if (cached_addresses == null || cached_addresses.length == 0) {
            try {
                cached_addresses = InetAddress.getAllByName(HostName);
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

    public abstract boolean fromJSON(JsonReader reader) throws IOException, ClassNotFoundException;

    public abstract int getListenPort();

    public abstract int getDestinationPort();

    public abstract String getProtocol();

    public boolean needsUpdate(DeviceConnection otherConnection) {
        if (not_reachable_reason == null) {
            return otherConnection.not_reachable_reason != null;
        } else
            return not_reachable_reason.equals(otherConnection.not_reachable_reason);
    }
}
