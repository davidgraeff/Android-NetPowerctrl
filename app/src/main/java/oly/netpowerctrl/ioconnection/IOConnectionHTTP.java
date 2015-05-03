package oly.netpowerctrl.ioconnection;

import android.support.annotation.NonNull;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;

import oly.netpowerctrl.devices.Credentials;

/**
 * A device connection for http
 */
public class IOConnectionHTTP extends IOConnection {
    public static final String PROTOCOL = "HTTP";
    public int PortHttp = -1;

    public IOConnectionHTTP(@NonNull Credentials credentials) {
        super(credentials);
    }

    public IOConnectionHTTP() {
        super(null);
    }


    @Override
    protected void write(JsonWriter writer) throws IOException {
        writer.name("HttpPort").value(PortHttp);
    }

    @Override
    protected void read(@NonNull JsonReader reader, String name) throws IOException {
        switch (name) {
            case "HttpPort":
                PortHttp = reader.nextInt();
                break;
            default:
                reader.skipValue();
                break;
        }
    }

    @Override
    public int getDestinationPort() {
        return PortHttp;
    }

    @Override
    public String getProtocol() {
        return PROTOCOL;
    }

    @Override
    public int computeHash() {
        StringBuilder builder = new StringBuilder();
        builder.append(PROTOCOL);
        builder.append(PortHttp);
        builder.append(hostName);
        return builder.toString().hashCode();
    }

    public void copyFrom(IOConnectionHTTP ioConnection) {
        receivedPackets = ioConnection.receivedPackets;
        credentials = ioConnection.credentials;
        deviceUID = ioConnection.deviceUID;
        hostName = ioConnection.hostName;
        PortHttp = ioConnection.PortHttp;
    }
}
