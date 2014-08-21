package oly.netpowerctrl.devices;

import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;

/**
 * Created by david on 28.07.14.
 */
public class DeviceConnectionHTTP extends DeviceConnection {
    public static final String ID = "HTTP";
    // Device
    public boolean DefaultPorts = true;
    public int PortHttp = -1;

    public DeviceConnectionHTTP(Device device) {
        super(device);
    }

    public DeviceConnectionHTTP(Device device, String hostName, int httpPort) {
        super(device);
        this.HostName = hostName;
        this.PortHttp = httpPort;
    }

    public void toJSON(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("connection_type").value(ID);
        writer.name("DefaultPorts").value(DefaultPorts);
        writer.name("HttpPort").value(PortHttp);
        writer.name("HostName").value(HostName);
        writer.name("AllowHostnameUpdates").value(mIsCustom);
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
                case "AllowHostnameUpdates":
                    mIsCustom = reader.nextBoolean();
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

        if (members == 3) {
            mIsCustom = HostName.startsWith("192.") || HostName.startsWith("10.");
        }

        return members >= 4;
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
    public String getProtocol() {
        return "HTTP";
    }
}
