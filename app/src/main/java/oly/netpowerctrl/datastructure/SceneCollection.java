package oly.netpowerctrl.datastructure;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.utils.Icons;

/**
 * List of scenes
 */
public class SceneCollection {
    public List<Scene> scenes;
    public IScenesSave storage;
    private ArrayList<IScenesUpdated> observers = new ArrayList<IScenesUpdated>();

    public SceneCollection(IScenesSave storage) {
        this.storage = storage;
        this.scenes = new ArrayList<Scene>();
    }

    public static SceneCollection fromScenes(List<Scene> scenes, IScenesSave storage) {
        SceneCollection dc = new SceneCollection(storage);
        dc.scenes = scenes;
        if (dc.scenes == null)
            dc.scenes = new ArrayList<Scene>();
        return dc;
    }

    public static SceneCollection fromJSON(JsonReader reader, IScenesSave storage) throws IOException {
        SceneCollection dc = new SceneCollection(storage);
        dc.scenes = new ArrayList<Scene>();

        reader.beginArray();
        while (reader.hasNext()) {
            dc.scenes.add(Scene.fromJSON(reader));
        }
        reader.endArray();
        return dc;
    }

    @SuppressWarnings("unused")
    public boolean registerObserver(IScenesUpdated o) {
        if (!observers.contains(o)) {
            observers.add(o);
            return true;
        }
        return false;
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

    public void toJSON(JsonWriter writer) throws IOException {
        writer.beginArray();
        for (Scene di : scenes) {
            di.toJSON(writer);
        }
        writer.endArray();
    }

    public void setBitmap(Context context, Scene scene, Bitmap scene_icon) {
        if (scene == null)
            return;
        Icons.saveIcon(context, scene.uuid, Icons.resizeBitmap(context, scene_icon, 128, 128),
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

    public void addScene(Scene data) {
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

        if (storage != null)
            storage.scenesSave(this);
        notifyObservers(true);
    }

    public void removeScene(int position) {
        if (position < 0 || position > scenes.size()) return;
        scenes.remove(position);
        storage.scenesSave(this);
        notifyObservers(true);
    }

    public void deleteAll() {
        scenes.clear();
        storage.scenesSave(this);
        notifyObservers(true);
    }

    public void reorderItems(int originalPosition, int newPosition, boolean saveReordering) {
        if (newPosition >= scenes.size()) {
            return;
        }
        Scene temp = scenes.get(originalPosition);
        scenes.remove(originalPosition);
        scenes.add(newPosition, temp);
        notifyObservers(true);
        if (saveReordering)
            storage.scenesSave(this);
    }

    public void saveScenes() {
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

    public interface IScenesUpdated {
        void scenesUpdated(boolean addedOrRemoved);
    }

    public interface IScenesSave {
        void scenesSave(SceneCollection scenes);
    }
}
