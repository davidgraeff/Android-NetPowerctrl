package oly.netpowerctrl.devices;

import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;

/**
 * Created by david on 28.07.14.
 */
public class DeviceConnectionAPI extends DeviceConnection {
    public static final String ID = "API";
    boolean isReachable = false;

    public DeviceConnectionAPI(Device device) {
        super(device);
        mIsAssignedByDevice = true;
    }

    public void toJSON(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("connection_type").value(ID);
        writer.name("HostName").value(mHostName);
        writer.name("isReachable").value(isReachable);
        writer.endObject();
    }

    @Override
    public boolean fromJSON(JsonReader reader, boolean beginObjectAlreadyCalled) throws IOException, ClassNotFoundException {
        if (!beginObjectAlreadyCalled)
            reader.beginObject();

        while (reader.hasNext()) {
            String name = reader.nextName();
            assert name != null;
            switch (name) {
                case "HostName":
                    mHostName = reader.nextString();
                    break;
                case "isReachable":
                    isReachable = reader.nextBoolean();
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
    public boolean equals(DeviceConnection deviceConnection) {
        return this == deviceConnection ||
                deviceConnection instanceof DeviceConnectionAPI &&
                        mHostName.equals(deviceConnection.mHostName);
    }

    @Override
    public boolean equalsByDestinationAddress(DeviceConnection otherConnection) {
        return mHostName.equals(otherConnection.mHostName);
    }
}
