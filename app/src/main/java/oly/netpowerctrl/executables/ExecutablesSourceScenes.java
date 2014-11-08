package oly.netpowerctrl.executables;

import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.data.onDataLoaded;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.scenes.Scene;

/**
 * Created by david on 07.07.14.
 */
public class ExecutablesSourceScenes extends ExecutablesSourceBase implements onCollectionUpdated<Object, Scene>, onDataLoaded {
    public ExecutablesSourceScenes(ExecutablesSourceChain executablesSourceChain) {
        super(executablesSourceChain);
    }

    @Override
    public int doCountIfGroup(UUID uuid) {
        int c = 0;
        List<Scene> scenes = AppData.getInstance().sceneCollection.getItems();
        for (Scene scene : scenes)
            if ((!hideNotReachable || scene.isReachable()) && (uuid == null && scene.groups.size() == 0 || scene.groups.contains(uuid)))
                ++c;
        return c;
    }

    @Override
    public void fullUpdate(ExecutablesBaseAdapter adapter) {
        if (adapter == null) {
            automaticUpdatesDisable();
        } else {
            List<Scene> scenes = AppData.getInstance().sceneCollection.getItems();
            for (Scene scene : scenes)
                if (scene.isReachable())
                    adapter.addItem(scene, DevicePort.TOGGLE);
        }
    }

    @Override
    protected void automaticUpdatesDisable() {
        AppData.getInstance().sceneCollection.unregisterObserver(this);
    }

    @Override
    protected void automaticUpdatesEnable() {
        // If no data has been loaded so far, wait for load action to be completed before
        // registering to deviceCollection changes.
        if (!AppData.isDataLoaded())
            AppData.observersOnDataLoaded.register(this);
        else {
            AppData.getInstance().sceneCollection.registerObserver(this);
        }
    }

    @Override
    public boolean onDataLoaded() {
        if (automaticUpdatesEnabled)
            automaticUpdatesEnable();
        return false;
    }

    @Override
    public boolean updated(Object collection, Scene scene, ObserverUpdateActions action, int position) {
        if (adapterWeakReference == null || scene == null)
            return true;

        ExecutablesBaseAdapter adapter = adapterWeakReference.get();
        if (adapter == null) {
            return true;
        }

        if (action == ObserverUpdateActions.RemoveAction) {
            adapter.removeAt(findPositionByUUid(adapter, scene.getUid()));
        } else if (action == ObserverUpdateActions.AddAction || action == ObserverUpdateActions.UpdateAction) {
            if (scene.isReachable())
                adapter.addItem(scene, DevicePort.TOGGLE);
            else
                adapter.removeAt(findPositionByUUid(adapter, scene.getUid()));
        } else if (action == ObserverUpdateActions.ClearAndNewAction || action == ObserverUpdateActions.RemoveAllAction) {
            updateNow();
            return true;
        }

        if (onChangeListener != null)
            onChangeListener.sourceChanged();

        return true;
    }

    private int findPositionByUUid(ExecutablesBaseAdapter adapter, String uuid) {
        if (uuid == null)
            return -1;

        int i = -1;
        for (ExecutableAdapterItem info : adapter.mItems) {
            ++i;
            String uid = info.getExecutableUid();
            if (uid == null) // skip header items
                continue;
            if (uid.equals(uuid))
                return i;
        }

        return -1;
    }
}
