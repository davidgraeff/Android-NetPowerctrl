package oly.netpowerctrl.scenes;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.utils.Icons;
import oly.netpowerctrl.utils.JSONHelper;
import oly.netpowerctrl.utils.SortCriteriaInterface;
import oly.netpowerctrl.utils.Sorting;

/**
 * List of scenes
 */
public class SceneCollection implements SortCriteriaInterface {
    public List<Scene> scenes;
    private final IScenesSave storage;
    private final ArrayList<IScenesUpdated> observers = new ArrayList<>();

    public SceneCollection(IScenesSave storage) {
        this.storage = storage;
        this.scenes = new ArrayList<>();
    }

    public static SceneCollection fromScenes(List<Scene> scenes, IScenesSave storage) {
        SceneCollection dc = new SceneCollection(storage);
        dc.scenes = scenes;
        if (dc.scenes == null)
            dc.scenes = new ArrayList<>();
        return dc;
    }

    public static SceneCollection fromJSON(JsonReader reader, IScenesSave storage) throws IOException {
        SceneCollection dc = new SceneCollection(storage);
        dc.scenes = new ArrayList<>();
        if (reader == null)
            return dc;

        reader.beginArray();
        while (reader.hasNext()) {
            dc.scenes.add(Scene.fromJSON(reader));
        }
        reader.endArray();
        return dc;
    }

    @SuppressWarnings("unused")
    public void registerObserver(IScenesUpdated o) {
        if (!observers.contains(o)) {
            observers.add(o);
        }
    }

    @SuppressWarnings("unused")
    public void unregisterObserver(IScenesUpdated o) {
        observers.remove(o);
    }

    @SuppressWarnings("unused")
    private void notifyObservers(boolean addedOrRemoved) {
        for (IScenesUpdated o : observers)
            o.scenesUpdated(addedOrRemoved);
    }

    public int length() {
        return scenes.size();
    }

    /**
     * Return the json representation of all groups
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

    void toJSON(JsonWriter writer) throws IOException {
        writer.beginArray();
        for (Scene di : scenes) {
            di.toJSON(writer);
        }
        writer.endArray();
    }

    public void setBitmap(Context context, Scene scene, Bitmap scene_icon) {
        if (scene == null)
            return;
        Icons.saveIcon(context, Icons.resizeBitmap(context, scene_icon, 128, 128), scene.uuid,
                Icons.IconType.SceneIcon, Icons.IconState.StateUnknown);
        notifyObservers(false);
    }


    public void setFavourite(Scene scene, boolean favourite) {
        if (scene == null)
            return;
        scene.favourite = favourite;
        notifyObservers(false);
    }

    public void setMasterScene(Scene scene, Scene.SceneItem item) {
        if (scene == null)
            return;
        scene.uuid_master = item.uuid;
        notifyObservers(false);
    }

    public void executeScene(int position) {
        NetpowerctrlApplication.getDataController().execute(getScene(position), null);
    }

    public void add(Scene data) {
        if (data == null)
            return;

        // scenes.indexOf(--data--)
        int position = -1;
        for (int i = 0; i < scenes.size(); ++i)
            if (scenes.get(i).equals(data)) {
                position = i;
                break;
            }

        // Replace existing item
        if (position != -1) {
            scenes.set(position, data);
        } else { // Add new item
            scenes.add(data);
        }

        save();
        notifyObservers(true);
    }

    public void removeScene(int position) {
        if (position < 0 || position > scenes.size()) return;
        scenes.remove(position);
        save();
        notifyObservers(true);
    }

    public void deleteAll() {
        scenes.clear();
        save();
        notifyObservers(true);
    }

    public void save() {
        if (storage != null)
            storage.scenesSave(this);
    }

    public boolean contains(Scene scene) {
        for (Scene s : scenes)
            if (s.equals(scene))
                return true;
        return false;
    }

    public Scene getScene(int position) {
        return scenes.get(position);
    }


    /**
     * Import String data (JSON) and either replace all existing data or merge it with the
     * existing data.
     *
     * @param tryToMerge If you merge the data instead of replacing the process is slower.
     */
    public boolean importData(boolean tryToMerge, String data) {
        SceneCollection dc;
        try {
            dc = SceneCollection.fromJSON(JSONHelper.getReader(data), null);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if (tryToMerge) {
            for (Scene di : dc.scenes)
                add(di);
        } else {
            scenes.clear();
            scenes = dc.scenes;

            notifyObservers(true);
            save();
        }
        return true;
    }

    @Override
    public String[] getContentList() {
        String[] l = new String[scenes.size()];
        for (int i = 0; i < scenes.size(); ++i) {
            l[i] = scenes.get(i).sceneName;
        }
        return l;
    }

    @Override
    public String[] getSortCriteria() {
        String[] s = new String[1];
        s[0] = "Alphabetisch";
        return s;
    }

    @Override
    public void applySortCriteria(final boolean[] criteria) {
        Sorting.qSort(scenes, 0, scenes.size() - 1, new Sorting.qSortComparable<Scene>() {
            @Override
            public boolean isGreater(Scene first, Scene second) {
                boolean isGreater = false;
                if (criteria[0] &&
                        first.sceneName.compareTo(second.sceneName) > 0) { // alphabetical
                    isGreater = true;
                }
                return isGreater;
            }
        });

        notifyObservers(false);
        save();
    }

    @Override
    public boolean allowCustomSort() {
        return true;
    }

    @Override
    public void setSortOrder(int[] sortOrder) {
        if (sortOrder.length != scenes.size()) {
            Log.e("scenes", "setSortOrder length wrong");
            return;
        }

        List<Scene> copy = scenes;
        scenes = new ArrayList<>();

        // Assign new positions
        for (int i = 0; i < copy.size(); ++i) {
            scenes.add(copy.get(sortOrder[i]));
        }

        notifyObservers(false);
        save();
    }

    public interface IScenesUpdated {
        void scenesUpdated(boolean addedOrRemoved);
    }

    public interface IScenesSave {
        void scenesSave(SceneCollection scenes);
    }
}
