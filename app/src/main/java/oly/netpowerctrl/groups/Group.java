package oly.netpowerctrl.groups;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.UUID;

import oly.netpowerctrl.device_base.data.JSONHelper;
import oly.netpowerctrl.device_base.data.StorableInterface;

/**
 * Created by david on 31.08.14.
 */
public class Group implements StorableInterface {
    public final long id = GroupCollection.nextStableID++;
    public UUID uuid;
    public String name;
    public Bitmap bitmap = null;

    // Invalid Group
    @SuppressWarnings("unused")
    public Group() {
    }

    public Group(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public Group setUUID(UUID uuid) {
        this.uuid = uuid;
        return this;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Group)
            return uuid.equals(((Group) other).uuid);
        else if (other instanceof UUID)
            return uuid.equals(other);
        return false;
    }

    @Override
    public String getStorableName() {
        return uuid.toString();
    }

    public void load(@NonNull JsonReader reader) throws IOException, ClassNotFoundException {
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            assert name != null;
            switch (name) {
                case "name":
                    this.name = reader.nextString();
                    break;
                case "uuid":
                    uuid = UUID.fromString(reader.nextString());
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();

        if (name == null || uuid == null)
            throw new ClassNotFoundException();
    }

    @Override
    public void load(@NonNull InputStream input) throws IOException, ClassNotFoundException {
        load(new JsonReader(new InputStreamReader(input)));
    }

    @Override
    public void save(@NonNull OutputStream output) throws IOException {
        toJSON(JSONHelper.createWriter(output));
    }


    /**
     * Return the json representation of this group item
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
        writer.name("uuid").value(uuid.toString());
        writer.name("name").value(name);
        writer.endObject();

        writer.close();
    }
}
