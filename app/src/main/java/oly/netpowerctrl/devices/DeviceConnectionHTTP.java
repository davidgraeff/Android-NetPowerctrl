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
    }

    public void toJSON(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("connection_type").value(ID);
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

        return members >= 3;
    }

    @Override
    public int getDestinationPort() {
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
                        equalsByDestinationAddress(deviceConnection, false) &&
                        PortHttp == ((DeviceConnectionHTTP) deviceConnection).PortHttp;
    }

    @Override
    public boolean equalsByDestinationAddress(DeviceConnection otherConnection, boolean lookupDNSName) {
        return cached_addresses == null ? mHostName.equals(otherConnection.mHostName) : hasAddress(otherConnection.getHostnameIPs(lookupDNSName), lookupDNSName);
    }
}
