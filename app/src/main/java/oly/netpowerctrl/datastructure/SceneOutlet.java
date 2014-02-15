package oly.netpowerctrl.datastructure;

import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;

import oly.netpowerctrl.application_state.NetpowerctrlApplication;

public class SceneOutlet {
    public static final int OFF = 0;
    public static final int ON = 1;
    public static final int TOGGLE = 2;
    // This field is not saved and is filled by fromOutletInfo for cache purposes only
    public String description = "";
    public OutletInfo outletinfo = null;

    public String device_mac = "";
    public int outletNumber;
    public int state = -1; //0:off;1:on;2:toggle
    public boolean enabled = false;

    @Override
    public boolean equals(Object o) {
        SceneOutlet other = (SceneOutlet) o;
        return other.device_mac.equals(device_mac) && other.outletNumber == outletNumber;
    }

    public static SceneOutlet fromOutletInfo(OutletInfo info, boolean enabled) {
        SceneOutlet c = new SceneOutlet();
        c.outletinfo = info;
        c.enabled = enabled;
        c.description = info.device.DeviceName + ": " + info.getDescription();
        c.device_mac = info.device.MacAddress;
        c.outletNumber = info.OutletNumber;
        c.state = info.State ? ON : OFF;
        return c;
    }

    public static SceneOutlet fromJSON(JsonReader reader) throws IOException {
        reader.beginObject();
        SceneOutlet oi = new SceneOutlet();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("outletNumber")) {
                oi.outletNumber = reader.nextInt();
            } else if (name.equals("device_mac")) {
                oi.device_mac = reader.nextString();
            } else if (name.equals("enabled")) {
                oi.enabled = reader.nextBoolean();
            } else if (name.equals("state")) {
                oi.state = reader.nextInt();
            } else {
                reader.skipValue();
            }
        }

        reader.endObject();
        oi.updateDeviceAndOutletLinks();

        return oi;
    }

    public void toJSON(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("outletNumber").value(outletNumber);
        writer.name("device_mac").value(device_mac);
        writer.name("enabled").value(enabled);
        writer.name("state").value(state);
        writer.endObject();
    }

    public boolean updateDeviceAndOutletLinks() {
        outletinfo = NetpowerctrlApplication.getDataController().findOutlet(device_mac, outletNumber);
        if (outletinfo != null) {
            description = outletinfo.device.DeviceName + ": " + outletinfo.getDescription();
            return true;
        }
        return false;
    }
}
