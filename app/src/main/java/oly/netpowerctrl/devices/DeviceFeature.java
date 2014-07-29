package oly.netpowerctrl.devices;

import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;

/**
 * Created by david on 28.07.14.
 */
public interface DeviceFeature {
    public String getString();

    public void toJSON(JsonWriter writer) throws IOException;

    boolean fromJSON(JsonReader reader) throws IOException;
}
