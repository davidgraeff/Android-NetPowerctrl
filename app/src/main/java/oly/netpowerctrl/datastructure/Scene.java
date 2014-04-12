package oly.netpowerctrl.datastructure;

import android.graphics.Bitmap;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.utils.Icons;
import oly.netpowerctrl.utils.JSONHelper;

public class Scene {
    public static long nextStableID = 0;

    public String sceneName = "";
    public UUID uuid = UUID.randomUUID();
    public Bitmap bitmap = null;
    public long id = nextStableID++;
    boolean favourite;

    public Bitmap getBitmap() {
        if (bitmap == null) {
            bitmap = Icons.loadIcon(NetpowerctrlApplication.instance, uuid,
                    Icons.IconType.SceneIcon, Icons.IconState.StateUnknown, R.drawable.netpowerctrl);
        }
        return bitmap;
    }

    public boolean isFavourite() {
        return favourite;
    }

    public static class SceneItem {
        public UUID uuid = UUID.randomUUID();
        public int command;

        public SceneItem() {
        }

        public SceneItem(UUID uuid, int command) {
            this.uuid = uuid;
            this.command = command;
        }
    }

    public List<SceneItem> sceneItems = new ArrayList<SceneItem>();

    /**
     * Return null if no master is set, otherwise return the command
     *
     * @return
     */
    public Integer getMasterCommand() {
        if (uuid_master == null)
            return null;

        for (SceneItem item : sceneItems)
            if (item.uuid.equals(uuid_master))
                return item.command;
        return null;
    }

    UUID uuid_master = null;

    public Scene() {
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Scene)
            return uuid.equals(((Scene) other).uuid);
        return false;
    }

    @SuppressWarnings("unused")
    public boolean equals(Scene other) {
        return uuid.equals(other.uuid);
    }

    public void add(UUID action_uuid, int command) {
        sceneItems.add(new SceneItem(action_uuid, command));
    }

    public int length() {
        return sceneItems.size();
    }

    public int getDevices(TreeSet<DeviceInfo> devices) {
        int valid_commands = 0;
        for (SceneItem c : sceneItems) {
            DevicePort port = NetpowerctrlApplication.getDataController().findDevicePort(c.uuid);
            if (port != null) {
                devices.add(port.device);
                ++valid_commands;
            }
        }
        return valid_commands;
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

    private static void readSceneItem(JsonReader reader, Scene scene) throws IOException {
        reader.beginObject();
        SceneItem item = new SceneItem();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("name")) {
                item.command = reader.nextInt();
            } else if (name.equals("uuid")) {
                item.uuid = UUID.fromString(reader.nextString());
            } else
                reader.skipValue();
        }
        reader.endObject();
        scene.sceneItems.add(item);
    }

    public static Scene fromJSON(JsonReader reader) throws IOException {
        Scene scene = new Scene();

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("sceneName")) {
                scene.sceneName = reader.nextString();
            } else if (name.equals("uuid")) {
                scene.uuid = UUID.fromString(reader.nextString());
            } else if (name.equals("master")) {
                scene.uuid_master = UUID.fromString(reader.nextString());
            } else if (name.equals("favourite")) {
                scene.favourite = reader.nextBoolean();
            } else if (name.equals("groupItems")) {
                reader.beginArray();
                while (reader.hasNext()) {
                    readSceneItem(reader, scene);
                }
                reader.endArray();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return scene;
    }

    public void toJSON(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("sceneName").value(sceneName);
        writer.name("uuid").value(uuid.toString());
        if (getSceneItem(uuid_master) != null)
            writer.name("uuid_master").value(uuid_master.toString());
        writer.name("favourite").value(favourite);
        writer.name("groupItems").beginArray();
        for (SceneItem c : sceneItems) {
            writer.beginObject();
            writer.name("uuid").value(c.uuid.toString());
            writer.name("name").value(c.command);
            writer.endObject();
        }
        writer.endArray();
        writer.endObject();
    }

    private SceneItem getSceneItem(UUID uuid) {
        if (uuid == null)
            return null;

        for (SceneItem item : sceneItems)
            if (item.uuid.equals(uuid))
                return item;
        return null;
    }

    public static class PortAndCommand {
        public DevicePort port;
        public Integer command;
    }
}
