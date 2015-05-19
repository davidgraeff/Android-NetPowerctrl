package oly.netpowerctrl.ioconnection;

import android.support.annotation.NonNull;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;

import oly.netpowerctrl.devices.Credentials;

/**
 * A device connection for udp
 */
public class IOConnectionUDP extends IOConnection {
    public static final String PROTOCOL = "UDP";
    // Device
    public int PortUDPSend = -1;
    public int PortUDPReceive = -1;

    public IOConnectionUDP(@NonNull Credentials credentials) {
        super(credentials);
    }

    public IOConnectionUDP() {
        super(null);
    }

    @Override
    protected void write(JsonWriter writer) throws IOException {
        writer.name("PortUDPSend").value(PortUDPSend);
        writer.name("PortUDPReceive").value(PortUDPReceive);
    }

    @Override
    protected void read(@NonNull JsonReader reader, String name) throws IOException {
        switch (name) {
            case "PortUDPSend":
                PortUDPSend = reader.nextInt();
                break;
            case "PortUDPReceive":
                PortUDPReceive = reader.nextInt();
                break;
            default:
                reader.skipValue();
                break;
        }
    }

    public int getListenPort() {
        return PortUDPReceive;
    }

    public int getDestinationPort() {
        return PortUDPSend;
    }

    @Override
    public String getProtocol() {
        return PROTOCOL;
    }

    @Override
    public int computeHash() {
        StringBuilder builder = new StringBuilder();
        builder.append(PROTOCOL);
        builder.append(PortUDPReceive);
        builder.append(PortUDPSend);
        builder.append(hostName);
        return builder.toString().hashCode();
    }
}
