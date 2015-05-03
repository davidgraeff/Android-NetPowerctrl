package oly.netpowerctrl.groups;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import oly.netpowerctrl.utils.IOInterface;
import oly.netpowerctrl.utils.JSONHelper;

/**
 * Created by david on 31.08.14.
 */
public class Group implements IOInterface {
    public final long id = GroupCollection.nextStableID++;
    public String uid;
    public String name;
    public Bitmap bitmap = null;
    public int last_changeCode = 0;

    // Invalid Group
    @SuppressWarnings("unused")
    public Group() {
    }

    public Group(String uuid, String name) {
        this.uid = uuid;
        this.name = name;
    }

    public Group setUUID(String uuid) {
        this.uid = uuid;
        return this;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Group)
            return uid.equals(((Group) other).uid);
        else if (other instanceof String)
            return uid.equals(other);
        return false;
    }

    @Override
    public String getUid() {
        return uid;
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
                case "uid":
                    uid = reader.nextString();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();

        if (name == null || uid == null)
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

    @Override
    public boolean hasChanged() {
        return last_changeCode != name.hashCode();
    }

    @Override
    public void resetChanged() {
        last_changeCode = name.hashCode();
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
        writer.name("uid").value(uid);
        writer.name("name").value(name);
        writer.endObject();

        writer.close();
    }
}
