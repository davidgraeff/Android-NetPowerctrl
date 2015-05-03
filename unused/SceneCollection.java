package oly.netpowerctrl.scenes;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

;
import oly.netpowerctrl.data.CollectionWithStorableItems;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.data.onStorageUpdate;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.executables.ExecutableCollection;

/**
 * List of scenes
 */
public class SceneCollection extends CollectionWithStorableItems<SceneCollection, Scene> {

    public onCollectionUpdated<ExecutableCollection, Executable> deviceObserver = new onCollectionUpdated<ExecutableCollection, Executable>() {
        @Override
        public boolean updated(@NonNull ExecutableCollection c, @Nullable Executable executable, @NonNull ObserverUpdateActions action, int position) {
            if (executable == null || (action != ObserverUpdateActions.UpdateAction && action != ObserverUpdateActions.ConnectionUpdateAction))
                return true;

            for (int index = 0; index < items.size(); ++index) {
                Scene scene = items.get(index);
                if (scene.isMasterSlave() && scene.getMasterExecutableUid().equals(executable.getUid())) {
                    if (updateSceneValue(scene, executable))
                        notifyObservers(scene, ObserverUpdateActions.UpdateAction, index);
                }
            }
            return true;
        }
    };

    public SceneCollection( PluginService pluginService) {
        super(PluginService);
        PluginService.executableCollection.registerObserver(deviceObserver);
    }

    @SuppressWarnings("unused")
    public static SceneCollection fromScenes( PluginService pluginService, List<Scene> scenes, onStorageUpdate storage) {
        SceneCollection dc = new SceneCollection(PluginService);
        dc.storage = storage;
        dc.items = scenes;
        if (dc.items == null)
            dc.items = new ArrayList<>();
        return dc;
    }

    private boolean updateSceneValue(Scene scene, Executable Executable) {
        if (scene.getMasterExecutableUid() == null)
            return false;

        boolean changed = false;
        if (scene.getCurrentValue() != Executable.getCurrentValue()) {
            scene.current_value = Executable.getCurrentValue();
            changed = true;
        }
        if (scene.getMaximumValue() != Executable.getMaximumValue()) {
            scene.max_value = Executable.getMaximumValue();
            changed = true;
        }
        if (scene.reachableState() != Executable.reachableState()) {
            scene.setReachable(Executable.reachableState());
            changed = true;
        }

        return changed;
    }

    public int length() {
        return items.size();
    }

    /**
     * Add/replace scene with same id
     *
     * @param scene           The new or updated scene object. If it is an updated scene object it has not be
     *                        the same object as before, the unique id just have to be equal.
     * @param notifyObservers Set to true to notify observers.
     * @return Return the new position of the added/updated scene.
     */
    public int add(Scene scene, boolean notifyObservers) {
        if (scene == null)
            return -1;

        // scenes.indexOf(--data--)
        int position = -1;
        for (int i = 0; i < items.size(); ++i)
            if (items.get(i).equals(scene)) {
                position = i;
                break;
            }

        // Get status of master
        Executable Executable = PluginService.findDevicePort(scene.getMasterExecutableUid());
        if (Executable != null)
            updateSceneValue(scene, Executable);

        // Replace existing item
        if (position != -1) {
            items.set(position, scene);
            if (notifyObservers)
                notifyObservers(scene, ObserverUpdateActions.UpdateAction, position);
        } else { // Add new item
            items.add(scene);
            if (notifyObservers)
                notifyObservers(scene, ObserverUpdateActions.AddAction, items.size() - 1);
        }

        save(scene);

        return position;
    }

    @SuppressWarnings("unused")
    public void removeScene(int position) {
        if (position < 0 || position > items.size()) return;
        Scene scene = items.get(position);
        items.remove(position);
        if (storage != null)
            storage.remove(this, scene);
        notifyObservers(scene, ObserverUpdateActions.RemoveAction, position);
    }

    @SuppressWarnings("unused")
    public void removeScene(String scene_uid) {
        for (int i = 0; i < items.size(); ++i) {
            Scene scene = items.get(i);
            if (scene.getUid().equals(scene_uid)) {
                items.remove(i);
                if (storage != null)
                    storage.remove(this, scene);
                notifyObservers(scene, ObserverUpdateActions.RemoveAction, i);
                break;
            }
        }
    }

    @SuppressWarnings("unused")
    public void removeAll() {
        int all = items.size();
        items.clear();
        if (storage != null)
            storage.clear(this);
        notifyObservers(null, ObserverUpdateActions.RemoveAllAction, all - 1);
    }

    public int indexOf(Scene scene) {
        for (int i = 0; i < items.size(); ++i)
            if (items.get(i).equals(scene))
                return i;
        return -1;
    }

    @Override
    public String type() {
        return "scenes";
    }

    @Nullable
    public Scene findByUID(String uid) {
        for (Scene s : items)
            if (s.getUid().equals(uid))
                return s;
        return null;
    }

    public void changed(int position) {
        if (position < 0 || position > items.size()) return;
        notifyObservers(items.get(position), ObserverUpdateActions.ClearAndNewAction, position);
    }
}
