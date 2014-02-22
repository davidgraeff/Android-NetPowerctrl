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
            try {
                dc.devices.add(DeviceInfo.fromJSON(reader));
            } catch (ClassNotFoundException e) {
                // If we read a device description, where we do not support that device type,
                // we just ignore that device and go on. Nevertheless print a backtrace.
                e.printStackTrace();
            }
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
