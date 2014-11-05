package oly.netpowerctrl.device_base.device;

import android.support.annotation.NonNull;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;

/**
 * This is used by extensions and their device connections.
 */
public class DeviceConnectionAPI extends DeviceConnection {
    public static final String ID = "API";
    private boolean includeUseCounter = false;

    public DeviceConnectionAPI(Device device) {
        super(device);
        mIsAssignedByDevice = true;
    }

    @SuppressWarnings("unused")
    public void setIncludeUseCounter(boolean includeUseCounter) {
        this.includeUseCounter = includeUseCounter;
    }

    public void toJSON(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("connection_type").value(ID);
        writer.name("HostName").value(mHostName);
        if (includeUseCounter) {
            writer.name("receivedPackets").value(receivedPackets);
            if (not_reachable_reason != null)
                writer.name("not_reachable_reason").value(not_reachable_reason);
        }
        writer.endObject();
    }

    @Override
    public boolean fromJSON(@NonNull JsonReader reader, boolean beginObjectAlreadyCalled) throws IOException, ClassNotFoundException {
        if (!beginObjectAlreadyCalled)
            reader.beginObject();

        while (reader.hasNext()) {
            String name = reader.nextName();
            assert name != null;
            switch (name) {
                case "not_reachable_reason":
                    not_reachable_reason = reader.nextString();
                    break;
                case "receivedPackets":
                    receivedPackets = reader.nextInt();
                    break;
                case "HostName":
                    mHostName = reader.nextString();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }

        reader.endObject();

        if (mHostName == null)
            mHostName = "";

        return mHostName.length() > 0;
    }

    @Override
    public boolean needResolveName() {
        return false;
    }

    @Override
    public int getDestinationPort() {
        return -1;
    }

    @Override
    public String getProtocol() {
        return ID;
    }

    @Override
    public boolean equals(@NonNull DeviceConnection deviceConnection) {
        return this == deviceConnection ||
                deviceConnection instanceof DeviceConnectionAPI &&
                        mHostName.equals(deviceConnection.mHostName);
    }

    @Override
    public boolean equalsByDestinationAddress(DeviceConnection otherConnection, boolean lookupDNSName) {
        return mHostName.equals(otherConnection.mHostName);
    }
}
