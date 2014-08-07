package oly.netpowerctrl.devices;

import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;

/**
 * Created by david on 28.07.14.
 */
public class DeviceConnectionHTTP implements DeviceConnection {
    public static final String ID = "HTTP";
    // Device
    public final Device device;
    public boolean DefaultPorts = true;
    public int PortHttp = -1;
    public String HostName;
    public String not_reachable_reason;

    public DeviceConnectionHTTP(Device device) {
        this.device = device;
    }

    public DeviceConnectionHTTP(Device device, String hostName, int httpPort) {
        this.device = device;
        this.HostName = hostName;
        this.PortHttp = httpPort;
    }

    public void toJSON(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("connection_type").value(ID);
        writer.name("DefaultPorts").value(DefaultPorts);
        writer.name("HttpPort").value(PortHttp);
        writer.name("HostName").value(HostName);
        writer.endObject();
    }

    @Override
    public boolean fromJSON(JsonReader reader) throws IOException {
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
                case "HttpPort":
                    PortHttp = reader.nextInt();
                    ++members;
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }

        reader.endObject();

        return members >= 3;
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

    @Override
    public int getListenPort() {
        return PortHttp;
    }

    @Override
    public int getDestinationPort() {
        if (DefaultPorts)
            return 80;
        return PortHttp;
    }

    @Override
    public String getDestinationHost() {
        return HostName;
    }

    @Override
    public String getProtocol() {
        return "HTTP";
    }

    @Override
    public Device getDevice() {
        return device;
    }

    @Override
    public String getString() {
        return getProtocol() + "/" + HostName + ":" + String.valueOf(getDestinationPort());
    }
}
