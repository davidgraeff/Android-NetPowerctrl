package oly.netpowerctrl.device_base.device;

import android.support.annotation.NonNull;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;

/**
 * A device connection for udp
 */
public class DeviceConnectionUDP extends DeviceConnection {
    public static final String ID = "UDP";
    // Device
    public int PortUDPSend = -1;
    public int PortUDPReceive = -1;

    public DeviceConnectionUDP(Device device) {
        super(device);
    }

    public DeviceConnectionUDP(Device device, String hostName, int PortUDPReceive, int PortUDPSend) {
        super(device);
        this.mHostName = hostName;
        this.PortUDPReceive = PortUDPReceive;
        this.PortUDPSend = PortUDPSend;
    }

    public void toJSON(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("connection_type").value(ID);
        writer.name("PortUDPSend").value(PortUDPSend);
        writer.name("PortUDPReceive").value(PortUDPReceive);
        writer.name("HostName").value(mHostName);
        writer.name("AllowHostnameUpdates").value(mIsAssignedByDevice);
        writer.endObject();
    }

    @Override
    public boolean fromJSON(@NonNull JsonReader reader, boolean beginObjectAlreadyCalled) throws IOException, ClassNotFoundException {
        if (!beginObjectAlreadyCalled)
            reader.beginObject();

        int members = 0;

        while (reader.hasNext()) {
            String name = reader.nextName();
            assert name != null;
            switch (name) {
                case "HostName":
                    mHostName = reader.nextString();
                    ++members;
                    break;
                case "AllowHostnameUpdates":
                    mIsAssignedByDevice = reader.nextBoolean();
                    ++members;
                    break;
                case "PortUDPSend":
                    PortUDPSend = reader.nextInt();
                    ++members;
                    break;
                case "PortUDPReceive":
                    PortUDPReceive = reader.nextInt();
                    ++members;
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }

        reader.endObject();

        //DEPRECATED
        if (members == 4) {
            mIsAssignedByDevice = mHostName.startsWith("192.") || mHostName.startsWith("10.");
        }

        return members >= 5;
    }

    public int getListenPort() {
        return PortUDPReceive;
    }

    @Override
    public int getDestinationPort() {
        return PortUDPSend;
    }

    @Override
    public String getProtocol() {
        return ID;
    }

    @Override
    public boolean equals(@NonNull DeviceConnection deviceConnection) {
        return this == deviceConnection ||
                deviceConnection instanceof DeviceConnectionUDP &&
                        equalsByDestinationAddress(deviceConnection, false) &&
                        PortUDPReceive == ((DeviceConnectionUDP) deviceConnection).PortUDPReceive &&
                        PortUDPSend == ((DeviceConnectionUDP) deviceConnection).PortUDPSend;
    }

    @Override
    public boolean equalsByDestinationAddress(DeviceConnection otherConnection, boolean lookupDNSName) {
        return cached_addresses == null ? mHostName.equals(otherConnection.mHostName) : hasAddress(otherConnection.getHostnameIPs(lookupDNSName), lookupDNSName);
    }
}
