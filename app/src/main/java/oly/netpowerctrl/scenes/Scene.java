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
import oly.netpowerctrl.data.Executable;
import oly.netpowerctrl.data.JSONHelper;
import oly.netpowerctrl.data.StorableInterface;
import oly.netpowerctrl.device_ports.DevicePort;
import oly.netpowerctrl.device_ports.ExecutableType;
import oly.netpowerctrl.devices.Device;

public class Scene implements StorableInterface, Executable {
    private static long nextStableID = 0;
    //public Bitmap bitmap = null;
    public final long id = nextStableID++;
    public final List<UUID> groups = new ArrayList<>();
    public String sceneName = "";
    public String uuid;
    public List<SceneItem> sceneItems = new ArrayList<>();
    boolean favourite;
    private String uuid_master = null;

    public Scene() {
        uuid = UUID.randomUUID().toString();
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
                    item.uuid = reader.nextString();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        scene.sceneItems.add(item);
    }

    /**
     * Return true if this scene is a favourite. If you want to set the favourite
     * flag use AppData.getInstance().sceneCollection.setFavaourite(scene, boolean);
     *
     * @return Return true if this scene is a favourite.
     */
    public boolean isFavourite() {
        return favourite;
    }

    public void setMaster(DevicePort master) {
        uuid_master = (master != null) ? master.getUid() : null;
    }

    public String getMasterUUid() {
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

    public void add(String action_uuid, int command) {
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
        writer.name("uuid").value(uuid);
        if (getSceneItem(uuid_master) != null)
            writer.name("uuid_master").value(uuid_master);
        writer.name("favourite").value(favourite);

        writer.name("groupItems").beginArray();
        for (SceneItem c : sceneItems) {
            writer.beginObject();
            writer.name("uuid").value(c.uuid);
            writer.name("name").value(c.command);
            writer.endObject();
        }
        writer.endArray();

        writer.name("groups").beginArray();
        for (UUID group_uuid : groups)
            writer.value(group_uuid.toString());
        writer.endArray();

        writer.endObject();

        writer.close();
    }

    private SceneItem getSceneItem(String uuid) {
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
                    scene.uuid = reader.nextString();
                    break;
                case "uuid_master":
                    scene.uuid_master = reader.nextString();
                    break;
                case "favourite":
                    scene.favourite = reader.nextBoolean();
                    break;
                case "groups":
                    scene.groups.clear();
                    reader.beginArray();
                    while (reader.hasNext()) {
                        scene.groups.add(UUID.fromString(reader.nextString()));
                    }
                    reader.endArray();
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

    @Override
    public List<UUID> getGroups() {
        return groups;
    }

    @Override
    public String getUid() {
        return uuid.toString();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public ExecutableType getType() {
        return ExecutableType.TypeScene;
    }

    @Override
    public String getTitle() {
        return sceneName;
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public boolean isReachable() {
        return true;
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
