package oly.netpowerctrl.scenes;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.device_base.data.JSONHelper;
import oly.netpowerctrl.device_base.data.StorableInterface;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.device_base.executables.Executable;
import oly.netpowerctrl.device_base.executables.ExecutableType;

public class Scene implements StorableInterface, Executable {
    private static long nextStableID = 0;
    //public Bitmap bitmap = null;
    public final long id = nextStableID++;
    public final List<UUID> groups = new ArrayList<>();
    public String sceneName = "";
    public String uuid;
    public List<SceneItem> sceneItems = new ArrayList<>();
    private String uuid_master = null;
    private int currentValue = 0;
    private int maximumValue = 0;

    private boolean reachable = false;

    /**
     * Create an invalid scene. Do not use that constructor, it is for instantiating per reflection only!
     */
    public Scene() {
    }

    public static Scene createNewSzene() {
        Scene scene = new Scene();
        scene.uuid = UUID.randomUUID().toString();
        return scene;
    }

    public static Scene loadFromJson(JsonReader reader) throws IOException, ClassNotFoundException {
        Scene scene = new Scene();
        scene.load(reader);
        return scene;
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
        if (item.uuid != null)
            scene.sceneItems.add(item);
    }

    public void setMaster(DevicePort master) {
        uuid_master = (master != null) ? master.getUid() : null;
    }

    public String getMasterUUid() {
        return uuid_master;
    }

    public SceneItem getMasterSceneItem() {
        if (uuid_master == null)
            return null;

        for (SceneItem item : sceneItems)
            if (item.uuid.equals(uuid_master)) {
                return item;
            }
        return null;
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
    public String getStorableName() {
        return uuid;
    }

    public void load(@NonNull JsonReader reader) throws IOException {
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
    public void load(@NonNull InputStream input) throws IOException, ClassNotFoundException {
        load(new JsonReader(new InputStreamReader(input)));
    }

    @Override
    public void save(@NonNull OutputStream output) throws IOException {
        toJSON(JSONHelper.createWriter(output));
    }

    @Override
    public List<UUID> getGroups() {
        return groups;
    }

    @Override
    public String getUid() {
        return uuid;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public ExecutableType getType() {
        return isMasterSlave() ? ExecutableType.TypeToggle : ExecutableType.TypeStateless;
    }

    @Override
    public String getTitle() {
        return sceneName;
    }

    @Override
    public String getDescription(Context context) {
        return isMasterSlave() ? context.getString(R.string.master_slave) : context.getString(R.string.scene);
    }

    @Override
    public boolean isReachable() {
        return !isMasterSlave() || reachable;
    }

    public void setReachable(boolean reachable) {
        this.reachable = reachable;
    }

    public int getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(int currentValue) {
        this.currentValue = currentValue;
    }

    @Override
    public int getMaximumValue() {
        return maximumValue;
    }

    public void setMaximumValue(int maximumValue) {
        this.maximumValue = maximumValue;
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
