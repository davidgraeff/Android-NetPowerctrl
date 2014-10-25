package oly.netpowerctrl.scenes;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.CollectionWithStorableItems;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.data.onDataLoaded;
import oly.netpowerctrl.data.onStorageUpdate;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.devices.DeviceCollection;
import oly.netpowerctrl.devices.DevicePort;
import oly.netpowerctrl.utils.SortCriteriaInterface;
import oly.netpowerctrl.utils.Sorting;

/**
 * List of scenes
 */
public class SceneCollection extends CollectionWithStorableItems<SceneCollection, Scene> implements SortCriteriaInterface {
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

    public void setBitmap(Context context, Scene scene, Bitmap scene_icon) {
        if (scene == null)
            return;
        LoadStoreIconData.saveIcon(context, LoadStoreIconData.resizeBitmap(context, scene_icon, 128, 128), scene.uuid,
                LoadStoreIconData.IconType.SceneIcon, LoadStoreIconData.IconState.StateUnknown);
        notifyObservers(scene, ObserverUpdateActions.UpdateAction, items.indexOf(scene));
    }


    public void setFavourite(Scene scene, boolean favourite) {
        if (scene == null)
            return;
        scene.favourite = favourite;
        if (storage != null)
            storage.save(this, scene);
        notifyObservers(scene, ObserverUpdateActions.UpdateAction, items.indexOf(scene));
    }

    public void executeScene(int position) {
        AppData.getInstance().execute(get(position), null);
    }

    public void add(Scene scene) {
        if (scene == null)
            return;

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
            notifyObservers(scene, ObserverUpdateActions.UpdateAction, position);
        } else { // Add new item
            items.add(scene);
            notifyObservers(scene, ObserverUpdateActions.AddAction, items.size() - 1);
        }

        if (storage != null)
            storage.save(this, scene);
    }

    public void removeScene(int position) {
        if (position < 0 || position > items.size()) return;
        Scene scene = items.get(position);
        items.remove(position);
        if (storage != null)
            storage.remove(this, scene);
        notifyObservers(scene, ObserverUpdateActions.RemoveAction, position);
    }

    public void removeAll() {
        int all = items.size();
        items.clear();
        if (storage != null)
            storage.clear(this);
        notifyObservers(null, ObserverUpdateActions.RemoveAllAction, all - 1);
    }


    public boolean contains(Scene scene) {
        for (Scene s : items)
            if (s.equals(scene))
                return true;
        return false;
    }

    @Override
    public String[] getContentList(int startPosition) {
        String[] l = new String[items.size()];
        for (int i = 0; i < items.size(); ++i) {
            l[i] = items.get(i).sceneName;
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
        Sorting.qSort(items, 0, items.size() - 1, new Sorting.qSortComparable<Scene>() {
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

        saveAll();
        notifyObservers(null, ObserverUpdateActions.ClearAndNewAction, -1);
    }

    @Override
    public boolean allowCustomSort() {
        return true;
    }

    @Override
    public void setSortOrder(int[] sortOrder) {
        if (sortOrder.length != items.size()) {
            Log.e("scenes", "setSortOrder length wrong");
            return;
        }

        List<Scene> copy = items;
        items = new ArrayList<>();

        // Assign new positions
        for (int i = 0; i < copy.size(); ++i) {
            items.add(copy.get(sortOrder[i]));
        }

        saveAll();
        notifyObservers(null, ObserverUpdateActions.ClearAndNewAction, -1);
    }

    @Override
    public String type() {
        return "scenes";
    }

    public Scene get(UUID sceneId) {
        for (Scene s : items)
            if (s.uuid.equals(sceneId))
                return s;
        return null;
    }
}
