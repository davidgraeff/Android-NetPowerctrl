package oly.netpowerctrl.scenes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.CollectionWithStorableItems;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.data.onDataLoaded;
import oly.netpowerctrl.data.onStorageUpdate;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.devices.DeviceCollection;

/**
 * List of scenes
 */
public class SceneCollection extends CollectionWithStorableItems<SceneCollection, Scene> {
    private onCollectionUpdated<DeviceCollection, Device> deviceObserver = new onCollectionUpdated<DeviceCollection, Device>() {
        @Override
        public boolean updated(DeviceCollection deviceCollection, Device device, ObserverUpdateActions action, int position) {
            device.lockDevicePorts();
            Iterator<DevicePort> iterator = device.getDevicePortIterator();
            while (iterator.hasNext()) {
                DevicePort devicePort = iterator.next();
                int index = -1;
                for (Scene scene : items) {
                    ++index;
                    if (scene.getMasterUUid() == null)
                        continue;
                    if (scene.getMasterUUid().equals(devicePort.getUid())) {
                        scene.setCurrentValue(devicePort.getCurrentValue());
                        scene.setMaximumValue(devicePort.getMaximumValue());
                        scene.setReachable(devicePort.isReachable());
                        notifyObservers(scene, ObserverUpdateActions.UpdateAction, index);
                    }
                }
            }
            device.releaseDevicePorts();
            return true;
        }
    };

    public SceneCollection() {
        AppData.observersOnDataLoaded.register(new onDataLoaded() {
            @Override
            public boolean onDataLoaded() {
                AppData.getInstance().deviceCollection.registerObserver(deviceObserver);
                return false;
            }
        });
    }

    @SuppressWarnings("unused")
    public static SceneCollection fromScenes(List<Scene> scenes, onStorageUpdate storage) {
        SceneCollection dc = new SceneCollection();
        dc.storage = storage;
        dc.items = scenes;
        if (dc.items == null)
            dc.items = new ArrayList<>();
        return dc;
    }

    public int length() {
        return items.size();
    }

    public void setFavourite(Scene scene, boolean favourite) {
        if (scene == null)
            return;
        scene.favourite = favourite;
        if (storage != null)
            storage.save(this, scene);
        notifyObservers(scene, ObserverUpdateActions.UpdateAction, items.indexOf(scene));
    }

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
    public void removeScene(Scene scene) {
        for (int i = 0; i < items.size(); ++i)
            if (items.get(i).equals(scene)) {
                items.remove(i);
                if (storage != null)
                    storage.remove(this, scene);
                notifyObservers(scene, ObserverUpdateActions.RemoveAction, i);
                break;
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

    public Scene get(String sceneId) {
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
