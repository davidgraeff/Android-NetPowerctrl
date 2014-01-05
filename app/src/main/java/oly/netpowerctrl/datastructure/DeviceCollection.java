package oly.netpowerctrl.datastructure;

import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains DeviceInfos. Used for NFC and backup transfers
 */
public class DeviceCollection {
    public List<DeviceInfo> devices;

    public static DeviceCollection fromDevices(List<DeviceInfo> devices) {
        DeviceCollection dc = new DeviceCollection();
        dc.devices = devices;
        return dc;
    }

    public static DeviceCollection fromJSON(JsonReader reader) throws IOException, IllegalStateException {
        DeviceCollection dc = new DeviceCollection();
        dc.devices = new ArrayList<DeviceInfo>();


        reader.beginArray();
        while (reader.hasNext()) {
            dc.devices.add(DeviceInfo.fromJSON(reader));
        }
        reader.endArray();
        return dc;
    }

    public void toJSON(JsonWriter writer) throws IOException {
        writer.beginArray();
        for (DeviceInfo di : devices) {
            di.toJSON(writer);
        }
        writer.endArray();
    }
}
