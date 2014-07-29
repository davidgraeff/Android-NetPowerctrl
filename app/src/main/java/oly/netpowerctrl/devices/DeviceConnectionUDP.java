package oly.netpowerctrl.devices;

import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;

import oly.netpowerctrl.preferences.SharedPrefs;

/**
 * Created by david on 28.07.14.
 */
public class DeviceConnectionUDP implements DeviceConnection {
    public static final String ID = "UDP";
    // Device
    public final Device device;
    public String HostName;
    public boolean DefaultPorts = true;
    public int PortUDPSend = -1;
    public int PortUDPReceive = -1;
    public String not_reachable_reason;

    public DeviceConnectionUDP(Device device) {
        this.device = device;
    }

    public DeviceConnectionUDP(Device device, String hostName, int PortUDPReceive, int PortUDPSend) {
        this.device = device;
        this.HostName = hostName;
        this.PortUDPReceive = PortUDPReceive;
        this.PortUDPSend = PortUDPSend;

    }

    public void toJSON(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("connection_type").value(ID);
        writer.name("DefaultPorts").value(DefaultPorts);
        writer.name("PortUDPSend").value(PortUDPSend);
        writer.name("PortUDPReceive").value(PortUDPReceive);
        writer.name("HostName").value(HostName);
        writer.endObject();
    }

    @Override
    public boolean fromJSON(JsonReader reader) throws IOException, ClassNotFoundException {
        int members = 0;
//        reader.beginObject();
        // no beginObject! we are already inside a json object
        while (reader.hasNext()) {
            String name = reader.nextName();
            assert name != null;
            switch (name) {
                case "HostName":
                    HostName = reader.nextString();
                    ++members;
                    break;
                case "DefaultPorts":
                    DefaultPorts = reader.nextBoolean();
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

        return members >= 4;
    }

    @Override
    public int getListenPort() {
        if (DefaultPorts)
            return SharedPrefs.getDefaultReceivePort();
        return PortUDPReceive;
    }

    @Override
    public int getDestinationPort() {
        if (DefaultPorts)
            return SharedPrefs.getDefaultSendPort();
        return PortUDPSend;
    }

    @Override
    public String getDestinationHost() {
        return HostName;
    }

    @Override
    public String getProtocol() {
        return "UDP";
    }

    @Override
    public Device getDevice() {
        return device;
    }

    @Override
    public String getString() {
        return getProtocol() + "/" + HostName;
    }

    @Override
    public boolean isReachable() {
        return not_reachable_reason == null;
    }

    @Override
    public String getNotReachableReason() {
        return not_reachable_reason;
    }

    @Override
    public void setNotReachable(String not_reachable_reason) {
        this.not_reachable_reason = not_reachable_reason;
    }

    @Override
    public void setReachable() {
        not_reachable_reason = null;
    }
}
