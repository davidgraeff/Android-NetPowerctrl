package oly.netpowerctrl.scenes;

import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.CollectionWithStorableItems;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.data.onStorageUpdate;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.devices.DeviceCollection;

/**
 * List of scenes
 */
public class SceneCollection extends CollectionWithStorableItems<SceneCollection, Scene> {

    public onCollectionUpdated<DeviceCollection, Device> deviceObserver = new onCollectionUpdated<DeviceCollection, Device>() {
        @Override
        public boolean updated(DeviceCollection deviceCollection, Device device, ObserverUpdateActions action, int position) {
            device.lockDevicePorts();
            Iterator<DevicePort> iterator = device.getDevicePortIterator();
            while (iterator.hasNext()) {
                DevicePort devicePort = iterator.next();
                for (int index = 0; index < items.size(); ++index) {
                    Scene scene = items.get(index);
                    if (scene.isMasterSlave() && scene.getMasterUUid().equals(devicePort.getUid())) {
                        if (updateSceneValue(scene, devicePort))
                            notifyObservers(scene, ObserverUpdateActions.UpdateAction, index);
                    }
                }
            }
            device.releaseDevicePorts();
            return true;
        }
    };

    @SuppressWarnings("unused")
    public static SceneCollection fromScenes(List<Scene> scenes, onStorageUpdate storage) {
        SceneCollection dc = new SceneCollection();
        dc.storage = storage;
        dc.items = scenes;
        if (dc.items == null)
            dc.items = new ArrayList<>();
        return dc;
    }

    private boolean updateSceneValue(Scene scene, DevicePort devicePort) {
        if (scene.getMasterUUid() == null)
            return false;

        boolean changed = false;
        if (scene.getCurrentValue() != devicePort.getCurrentValue()) {
            scene.setCurrentValue(devicePort.getCurrentValue());
            changed = true;
        }
        if (scene.getMaximumValue() != devicePort.getMaximumValue()) {
            scene.setMaximumValue(devicePort.getMaximumValue());
            changed = true;
        }
        if (scene.isReachable() != devicePort.isReachable()) {
            scene.setReachable(devicePort.isReachable());
            changed = true;
        }

        return changed;
    }

    public int length() {
        return items.size();
    }

    public void setFavourite(Scene scene, boolean favourite) {
        int index = indexOf(scene);
        if (scene == null || index == -1)
            return;
        scene.favourite = favourite;
        if (storage != null)
            storage.save(this, scene);
        notifyObservers(scene, ObserverUpdateActions.UpdateAction, index);
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
        DevicePort devicePort = AppData.getInstance().findDevicePort(scene.getMasterUUid());
        if (devicePort != null)
            updateSceneValue(scene, devicePort);

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

        if (storage != null)
            storage.save(this, scene);

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
    public Scene findScene(String sceneId) {
        for (Scene s : items)
            if (s.uuid.equals(sceneId))
                return s;
        return null;
    }

    public void changed(int position) {
        if (position < 0 || position > items.size()) return;
        notifyObservers(items.get(position), ObserverUpdateActions.ClearAndNewAction, position);
    }
}
