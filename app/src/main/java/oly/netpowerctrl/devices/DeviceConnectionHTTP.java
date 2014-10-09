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

    /**
     * Create a new http connection with hostname and port that does not use default ports.
     *
     * @param device   The device
     * @param hostName The hostname or IP
     * @param httpPort The http port
     */
    public DeviceConnectionHTTP(Device device, String hostName, int httpPort) {
        super(device);
        this.mHostName = hostName;
        this.PortHttp = httpPort;
        DefaultPorts = false;
    }

    public void toJSON(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("connection_type").value(ID);
        writer.name("DefaultPorts").value(DefaultPorts);
        writer.name("HttpPort").value(PortHttp);
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
            mIsAssignedByDevice = mHostName.startsWith("192.") || mHostName.startsWith("10.");
        }

        return members >= 4;
    }

    @Override
    public int getDestinationPort() {
        if (DefaultPorts)
            return 80;
        return PortHttp;
    }

    @Override
    public String getProtocol() {
        return ID;
    }

    @Override
    public boolean equals(DeviceConnection deviceConnection) {
        return this == deviceConnection ||
                deviceConnection instanceof DeviceConnectionHTTP &&
                        equalsByDestinationAddress(deviceConnection) &&
                        PortHttp == ((DeviceConnectionHTTP) deviceConnection).PortHttp;
    }

    @Override
    public boolean equalsByDestinationAddress(DeviceConnection otherConnection) {
        return cached_addresses == null ? mHostName.equals(otherConnection.mHostName) : hasAddress(otherConnection.getHostnameIPs());
    }
}
