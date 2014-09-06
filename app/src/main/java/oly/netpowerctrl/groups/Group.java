package oly.netpowerctrl.groups;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.JSONHelper;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.data.StorableInterface;

/**
 * Created by david on 31.08.14.
 */
public class Group implements StorableInterface {
    public final long id = GroupCollection.nextStableID++;
    public UUID uuid;
    public String name;
    public Bitmap bitmap = null;

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

    public Bitmap getBitmap(Context context) {
        if (bitmap == null) {
            bitmap = LoadStoreIconData.loadIcon(context, uuid,
                    LoadStoreIconData.IconType.SceneIcon, LoadStoreIconData.IconState.StateUnknown, R.drawable.netpowerctrl);
        }
        return bitmap;
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
    public void load(InputStream input) throws IOException, ClassNotFoundException {
        load(new JsonReader(new InputStreamReader(input)));
    }

    @Override
    public void save(OutputStream output) throws IOException {
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
