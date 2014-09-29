package oly.netpowerctrl.devices;

import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;

import oly.netpowerctrl.data.SharedPrefs;

/**
 * Created by david on 28.07.14.
 */
public class DeviceConnectionUDP extends DeviceConnection {
    public static final String ID = "UDP";
    // Device
    public boolean DefaultPorts = true;
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
        writer.name("DefaultPorts").value(DefaultPorts);
        writer.name("PortUDPSend").value(PortUDPSend);
        writer.name("PortUDPReceive").value(PortUDPReceive);
        writer.name("HostName").value(mHostName);
        writer.name("AllowHostnameUpdates").value(mIsAssignedByDevice);
        writer.endObject();
    }

    @Override
    public boolean fromJSON(JsonReader reader, boolean beginObjectAlreadyCalled) throws IOException, ClassNotFoundException {
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
                case "DefaultPorts":
                    DefaultPorts = reader.nextBoolean();
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
        if (DefaultPorts)
            return SharedPrefs.getInstance().getDefaultReceivePort();
        return PortUDPReceive;
    }

    @Override
    public int getDestinationPort() {
        if (DefaultPorts)
            return SharedPrefs.getInstance().getDefaultSendPort();
        return PortUDPSend;
    }

    @Override
    public String getProtocol() {
        return ID;
    }

    @Override
    public boolean equals(DeviceConnection deviceConnection) {
        return this == deviceConnection ||
                deviceConnection instanceof DeviceConnectionUDP &&
                        equalsByDestinationAddress(deviceConnection) &&
                        PortUDPReceive == ((DeviceConnectionUDP) deviceConnection).PortUDPReceive &&
                        PortUDPSend == ((DeviceConnectionUDP) deviceConnection).PortUDPSend;
    }

    @Override
    public boolean equalsByDestinationAddress(DeviceConnection otherConnection) {
        return cached_addresses == null ? mHostName.equals(otherConnection.mHostName) : hasAddress(otherConnection.getHostnameIPs());
    }
}
