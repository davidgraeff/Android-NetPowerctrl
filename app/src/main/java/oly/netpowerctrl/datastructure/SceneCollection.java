package oly.netpowerctrl.datastructure;

import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * List of scenes
 */
public class SceneCollection {
    public List<Scene> scenes;

    public static SceneCollection fromScenes(List<Scene> scenes) {
        SceneCollection dc = new SceneCollection();
        dc.scenes = scenes;
        return dc;
    }

    public static SceneCollection fromJSON(JsonReader reader) throws IOException {
        SceneCollection dc = new SceneCollection();
        dc.scenes = new ArrayList<Scene>();

        reader.beginArray();
        while (reader.hasNext()) {
            dc.scenes.add(Scene.fromJSON(reader));
        }
        reader.endArray();
        return dc;
    }

    public void toJSON(JsonWriter writer) throws IOException {
        writer.beginArray();
        for (Scene di : scenes) {
            di.toJSON(writer);
        }
        writer.endArray();
    }
}
