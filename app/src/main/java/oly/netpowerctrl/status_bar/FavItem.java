package oly.netpowerctrl.status_bar;

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
 * Created by david on 27.04.15.
 */
public class FavItem implements IOInterface {
    private String executable_uid;

    public FavItem() {
    }

    public FavItem(String executable_uid) {
        this.executable_uid = executable_uid;
    }

    @Override
    public String getUid() {
        return executable_uid;
    }

    @Override
    public void load(@NonNull InputStream input) throws IOException, ClassNotFoundException {
        JsonReader reader = (new JsonReader(new InputStreamReader(input)));
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            assert name != null;
            switch (name) {
                case "executable_uid":
                    executable_uid = reader.nextString();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
    }

    @Override
    public void save(@NonNull OutputStream output) throws IOException {
        toJSON(JSONHelper.createWriter(output));
    }

    @Override
    public boolean hasChanged() {
        return true;
    }

    @Override
    public void setHasChanged() {
    }

    @Override
    public void resetChanged() {

    }

    private void toJSON(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("executable_uid").value(executable_uid);
        writer.endObject();
        writer.close();
    }
}
