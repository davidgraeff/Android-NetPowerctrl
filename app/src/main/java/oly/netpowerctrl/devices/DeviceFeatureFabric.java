package oly.netpowerctrl.devices;

import android.util.JsonReader;
import android.util.Log;

import java.io.IOException;

/**
 * Created by david on 28.07.14.
 */
public class DeviceFeatureFabric {

    public static DeviceFeature fromJSON(JsonReader reader)
            throws IOException, ClassNotFoundException {
        reader.beginObject();
        if (!reader.hasNext()) {
            return null;
        }

        String name = reader.nextName();
        if (!name.equals("feature_id")) {
            Log.e("DeviceFeatureFabric", "Expected feature_id! " + name);
            reader.endObject();
            return null;
        }

        name = reader.nextString();

        DeviceFeature deviceFeature = null;

        switch (name) {
            case DeviceFeatureTemperature.ID: {
                deviceFeature = new DeviceFeatureTemperature();
                break;
            }
            default:
                throw new ClassNotFoundException("Unexpected feature: " + name);
        }

        if (!deviceFeature.fromJSON(reader)) {
            return null;
        } else
            return deviceFeature;
    }
}