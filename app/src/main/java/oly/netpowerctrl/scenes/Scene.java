package oly.netpowerctrl.scenes;

import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.JSONHelper;
import oly.netpowerctrl.data.Storable;
import oly.netpowerctrl.device_ports.DevicePort;
import oly.netpowerctrl.devices.Device;

public class Scene implements Storable {
    private static long nextStableID = 0;
    //public Bitmap bitmap = null;
    public final long id = nextStableID++;
    public String sceneName = "";
    public UUID uuid = UUID.randomUUID();
    public List<SceneItem> sceneItems = new ArrayList<>();
    boolean favourite;
    private UUID uuid_master = null;

    public Scene() {
    }

    private static void readSceneItem(JsonReader reader, Scene scene) throws IOException {
        reader.beginObject();
        SceneItem item = new SceneItem();
        while (reader.hasNext()) {
            String name = reader.nextName();
            assert name != null;
            switch (name) {
                case "name":
                    item.command = reader.nextInt();
                    break;
                case "uuid":
                    item.uuid = UUID.fromString(reader.nextString());
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        scene.sceneItems.add(item);
    }

    public boolean isFavourite() {
        return favourite;
    }

    public void setMaster(DevicePort master) {
        uuid_master = (master != null) ? master.uuid : null;
    }

    public UUID getMasterUUid() {
        return uuid_master;
    }

    /**
     * Return INVALID if no master is set, otherwise return
     * the final command. Final means: If the actual scene command
     * of the master is toggle, we first have to figure out the state
     * after toggling is applied.
     *
     * @return
     */
    public int getMasterCommand() {
        if (uuid_master == null)
            return DevicePort.INVALID;

        for (SceneItem item : sceneItems)
            if (item.uuid.equals(uuid_master)) {
                // If the command is not toggle, we return it now. It can be applied to slaves
                // directly.
                if (item.command != DevicePort.TOGGLE)
                    return item.command;
                // If the command is toggle, we have to find out the final command.
                DevicePort port = AppData.getInstance().findDevicePort(item.uuid);
                if (port == null)
                    return DevicePort.INVALID;

                return port.getCurrentValueToggled();
            }
        return DevicePort.INVALID;
    }

    public boolean isMasterSlave() {
        return (uuid_master != null);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Scene && uuid.equals(((Scene) other).uuid);
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

    public int getDevices(TreeSet<Device> devices) {
        int valid_commands = 0;
        for (SceneItem c : sceneItems) {
            DevicePort port = AppData.getInstance().findDevicePort(c.uuid);
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
        try {
            JSONHelper h = new JSONHelper();
            toJSON(h.createWriter());
            return h.getString();
        } catch (IOException ignored) {
            return null;
        }
    }

    private void toJSON(JsonWriter writer) throws IOException {
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

        writer.close();
    }

    private SceneItem getSceneItem(UUID uuid) {
        if (uuid == null)
            return null;

        for (SceneItem item : sceneItems)
            if (item.uuid.equals(uuid))
                return item;
        return null;
    }

    @Override
    public StorableDataType getDataType() {
        return StorableDataType.JSON;
    }

    @Override
    public String getStorableName() {
        return uuid.toString();
    }

    @Override
    public void load(JsonReader reader) throws IOException, ClassNotFoundException {
        Scene scene = this;

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            assert name != null;
            switch (name) {
                case "sceneName":
                    scene.sceneName = reader.nextString();
                    break;
                case "uuid":
                    scene.uuid = UUID.fromString(reader.nextString());
                    break;
                case "uuid_master":
                    scene.uuid_master = UUID.fromString(reader.nextString());
                    break;
                case "favourite":
                    scene.favourite = reader.nextBoolean();
                    break;
                case "groupItems":
                    reader.beginArray();
                    while (reader.hasNext()) {
                        readSceneItem(reader, scene);
                    }
                    reader.endArray();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
    }

    @Override
    public void load(InputStream input) throws IOException, ClassNotFoundException {
        load(new JsonReader(new InputStreamReader(input)));
    }

    @Override
    public void save(OutputStream output) throws IOException {
        toJSON(JSONHelper.createWriter(output));
    }

    public static class PortAndCommand {
        public final DevicePort port;
        public final Integer command;

        public PortAndCommand(DevicePort port, Integer command) {
            this.port = port;
            this.command = command;
        }
    }
}
