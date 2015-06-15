package oly.netpowerctrl.ioconnection.adapter;

import android.support.annotation.NonNull;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;

import oly.netpowerctrl.credentials.Credentials;
import oly.netpowerctrl.ioconnection.IOConnection;

/**
 * A device connection for udp
 */
public class IOConnectionIP extends IOConnection {
    public static final String PROTOCOL = "IP";
    public String additional = "";

    public IOConnectionIP(@NonNull Credentials credentials) {
        super(credentials);
    }

    public IOConnectionIP() {
        super(null);
    }

    @Override
    protected void write(JsonWriter writer) throws IOException {
        writer.name("additional").value(additional);
    }

    @Override
    protected void read(@NonNull JsonReader reader, String name) throws IOException {
        switch (name) {
            case "additional":
                additional = reader.nextString();
                break;
            default:
                reader.skipValue();
                break;
        }
    }

    @Override
    public String getProtocol() {
        return PROTOCOL;
    }

    @Override
    public int computeHash() {
        return (PROTOCOL + additional + hostName).hashCode();
    }

    public void copyFrom(IOConnectionIP ioConnection) {
        receivedPackets = ioConnection.receivedPackets;
        credentials = ioConnection.credentials;
        deviceUID = ioConnection.deviceUID;
        hostName = ioConnection.hostName;
        additional = ioConnection.additional;
    }
}
