package oly.netpowerctrl.data;

import android.support.annotation.NonNull;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import oly.netpowerctrl.device_base.data.JSONHelper;
import oly.netpowerctrl.device_base.data.StorableInterface;

/**
 * List of scenes
 */
public class FavCollection extends CollectionWithStorableItems<FavCollection, FavCollection.FavItem> {
    public int length() {
        return items.size();
    }

    /**
     * Updates the favourite flag of this executable. A favourite is shown in the android
     * system bar (if enabled) and can be executed from there directly.
     *
     * @param favourite The new favourite status.
     */
    public void setFavourite(String executable_uid, boolean favourite) {
        int i = 0;
        for (FavItem s : items) {
            if (s.executable_uid.equals(executable_uid)) {
                if (!favourite) {
                    notifyObservers(s, ObserverUpdateActions.RemoveAction, i);
                    remove(s);
                }
                return;
            }
            ++i;
        }

        if (favourite) {
            FavItem item = new FavItem();
            item.executable_uid = executable_uid;
            items.add(item);
            save(item);
            notifyObservers(item, ObserverUpdateActions.AddAction, items.size() - 1);
        }
    }

    /**
     * Return true if this scene is a favourite. If you want to set the favourite
     * flag use AppData.getInstance().sceneCollection.setFavaourite(scene, boolean);
     *
     * @return Return true if this scene is a favourite.
     */
    @SuppressWarnings("unused")
    public boolean isFavourite(String executable_uid) {
        for (FavItem s : items)
            if (s.executable_uid.equals(executable_uid))
                return true;
        return false;
    }

    @Override
    public String type() {
        return "favourites";
    }

    public static class FavItem implements StorableInterface {
        public String executable_uid;

        public FavItem() {
        }

        @Override
        public String getStorableName() {
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

        private void toJSON(JsonWriter writer) throws IOException {
            writer.beginObject();
            writer.name("executable_uid").value(executable_uid);
            writer.endObject();
            writer.close();
        }
    }
}
