package oly.netpowerctrl.ioconnection;

import android.support.annotation.NonNull;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;

import oly.netpowerctrl.credentials.Credentials;

/**
 * A device connection for udp
 */
public class IOConnectionIP extends IOConnection {
    public static final String PROTOCOL = "IP";
    public String physicalAddress = "";

    public IOConnectionIP(@NonNull Credentials credentials) {
        super(credentials);
    }

    public IOConnectionIP() {
        super(null);
    }

    @Override
    protected void write(JsonWriter writer) throws IOException {
        writer.name("physicalAddress").value(physicalAddress);
    }

    @Override
    protected void read(@NonNull JsonReader reader, String name) throws IOException {
        switch (name) {
            case "physicalAddress":
                physicalAddress = reader.nextString();
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
        return (PROTOCOL + physicalAddress + hostName).hashCode();
    }

    public void copyFrom(IOConnectionIP ioConnection) {
        receivedPackets = ioConnection.receivedPackets;
        credentials = ioConnection.credentials;
        deviceUID = ioConnection.deviceUID;
        hostName = ioConnection.hostName;
        physicalAddress = ioConnection.physicalAddress;
    }
}
