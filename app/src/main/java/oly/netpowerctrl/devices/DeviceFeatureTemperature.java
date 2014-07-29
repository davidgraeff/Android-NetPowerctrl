package oly.netpowerctrl.devices;

import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;

/**
 * Created by david on 28.07.14.
 */
public class DeviceFeatureTemperature implements DeviceFeature {
    public static final String ID = "Temperature";
    public String temp;

    public DeviceFeatureTemperature() {
    }

    public DeviceFeatureTemperature(String temp) {
        this.temp = temp;
    }

    @Override
    public String getString() {
        return temp;
    }

    public void toJSON(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("feature_id").value(ID);
        writer.name("temp").value(temp);
        writer.endObject();
    }

    @Override
    public boolean fromJSON(JsonReader reader) throws IOException {
        boolean ok = false;
        // no beginObject! we are already inside a json object
        while (reader.hasNext()) {
            String name = reader.nextName();
            assert name != null;

            switch (name) {
                case "temp":
                    temp = reader.nextString();
                    ok = true;
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }

        reader.endObject();
        return ok;
    }
}
