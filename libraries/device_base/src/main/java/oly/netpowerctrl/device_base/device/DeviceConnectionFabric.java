package oly.netpowerctrl.device_base.device;

import android.util.JsonReader;
import android.util.Log;

import java.io.IOException;

/**
 * Created by david on 28.07.14.
 */
public class DeviceConnectionFabric {

    public static DeviceConnection fromJSON(JsonReader reader, Device device)
            throws IOException, ClassNotFoundException {
        reader.beginObject();
        if (!reader.hasNext()) {
            return null;
        }

        String name = reader.nextName();
        if (!name.equals("connection_type")) {
            Log.e("DeviceConnection", "Expected connection_type first! " + name);
            reader.endObject();
            return null;
        }

        name = reader.nextString();

        DeviceConnection deviceConnection;

        switch (name) {
            case DeviceConnectionUDP.ID:
                deviceConnection = new DeviceConnectionUDP(device);
                break;
            case DeviceConnectionHTTP.ID:
                deviceConnection = new DeviceConnectionHTTP(device);
                break;
            case DeviceConnectionAPI.ID:
                deviceConnection = new DeviceConnectionAPI(device);
                break;
            default:
                throw new ClassNotFoundException("Unexpected connection_type: " + name);
        }

        if (!deviceConnection.fromJSON(reader, true)) {
            return null;
        } else
            return deviceConnection;
    }
}
