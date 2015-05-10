package oly.netpowerctrl.scenes;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.executables.adapter.ExecutableAdapterItem;
import oly.netpowerctrl.scenes.adapter.SceneElementsAdapter;

/**
 * Created by david on 22.10.14.
 */
public class SceneFactory {
    /**
     * Create a list of scene items by all visible device ports.
     *
     * @return List of scene items.
     */
    public static List<SceneItem> sceneItemsFromList(SceneElementsAdapter adapter) {
        List<SceneItem> list_of_scene_items = new ArrayList<>();
        for (ExecutableAdapterItem info : adapter.mItems) {
            if (info.getExecutable() == null) // skip header items
                continue;
            list_of_scene_items.add(new SceneItem(info.getExecutableUid(), info.getCommandValue()));
        }
        return list_of_scene_items;
    }
}
