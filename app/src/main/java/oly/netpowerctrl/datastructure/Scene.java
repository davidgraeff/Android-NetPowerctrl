package oly.netpowerctrl.datastructure;

import android.content.Context;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeMap;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.main.NetpowerctrlApplication;
import oly.netpowerctrl.utils.JSONHelper;


public class Scene {
    public String sceneName = "";
    public String sceneDetails = "";
    public UUID uuid = UUID.randomUUID();
    public ArrayList<SceneOutlet> commands = new ArrayList<SceneOutlet>();

    public Scene() {
    }

    @Override
    public boolean equals(Object other) {
        return uuid.equals(((Scene) other).uuid);
    }

    @SuppressWarnings("unused")
    public boolean equals(Scene other) {
        return uuid.equals(other.uuid);
    }

    public String buildDetails() {
        int ons = 0;
        int offs = 0;
        int toggles = 0;
        for (SceneOutlet c : commands) {
            switch (c.state) {
                case 0:
                    ++offs;
                    break;
                case 1:
                    ++ons;
                    break;
                case 2:
                    ++toggles;
                    break;
            }
        }
        Context context = NetpowerctrlApplication.instance;
        return context.getResources().getString(R.string.off) + ": " + Integer.valueOf(offs).toString() + ", " +
                context.getResources().getString(R.string.on) + ": " + Integer.valueOf(ons).toString() + ", " +
                context.getResources().getString(R.string.toggle) + ": " + Integer.valueOf(toggles).toString();
    }

    public void add(SceneOutlet c) {
        commands.add(c);
    }

    public int length() {
        return commands.size();
    }

    public Collection<DeviceInfo> getDevices() {
        TreeMap<String, DeviceInfo> devices = new TreeMap<String, DeviceInfo>();
        for (SceneOutlet c : commands) {
            if (!devices.containsKey(c.device_mac)) {
                devices.put(c.device_mac, c.outletinfo.device);
            }
        }
        return devices.values();
    }

    /**
     * Return the json representation of this scene
     *
     * @return JSON String
     */
    @Override
    public String toString() {
        return toJSON();
    }

    /**
     * Return the json representation of this scene
     *
     * @return JSON String
     */
    public String toJSON() {
        try {
            JSONHelper h = new JSONHelper();
            toJSON(h.createWriter());
            return h.getString();
        } catch (IOException ignored) {
            return null;
        }
    }

    public static Scene fromJSON(JsonReader reader) throws IOException {
        Scene og = new Scene();

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("sceneName")) {
                og.sceneName = reader.nextString();
            } else if (name.equals("uuid")) {
                og.uuid = UUID.fromString(reader.nextString());
            } else if (name.equals("commands")) {
                reader.beginArray();
                while (reader.hasNext()) {
                    og.commands.add(SceneOutlet.fromJSON(reader));
                }
                reader.endArray();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();

        og.sceneDetails = og.buildDetails();
        return og;
    }

    public void toJSON(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("sceneName").value(sceneName);
        writer.name("uuid").value(uuid.toString());
        writer.name("commands").beginArray();
        for (SceneOutlet c : commands) {
            c.toJSON(writer);
        }
        writer.endArray();
        writer.endObject();
    }
}