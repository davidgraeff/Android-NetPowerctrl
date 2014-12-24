package oly.netpowerctrl.executables;

import android.support.annotation.NonNull;

import java.util.List;

import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.scenes.Scene;
import oly.netpowerctrl.scenes.SceneCollection;

/**
 * Created by david on 07.07.14.
 */
public class AdapterSourceInputScenes extends AdapterSourceInput implements onCollectionUpdated<Object, Scene> {
    private SceneCollection sceneCollection = null;
    private boolean showNotReachableScenes = true;

    public void setShowNotReachableScenes(boolean showNotReachableScenes) {
        this.showNotReachableScenes = showNotReachableScenes;
    }

    @Override
    public void doUpdateNow(@NonNull ExecutablesBaseAdapter adapter) {
        if (sceneCollection != null) {
            List<Scene> scenes = sceneCollection.getItems();
            for (Scene scene : scenes)
                if (showNotReachableScenes || scene.isReachable())
                    adapter.addItem(scene, DevicePort.TOGGLE);
        }
    }

    @Override
    void onFinish() {
        if (sceneCollection != null) sceneCollection.unregisterObserver(this);
        sceneCollection = null;
    }

    @Override
    void onStart(AppData appData) {
        sceneCollection = appData.sceneCollection;
        sceneCollection.registerObserver(this);
    }

    @Override
    public boolean updated(@NonNull Object collection, Scene scene, @NonNull ObserverUpdateActions action, int position) {
        if (scene == null || adapterSource.ignoreUpdatesExecutable == scene)
            return true;

        ExecutablesBaseAdapter adapter = adapterSource.getAdapter();
        if (adapter == null) {
            return true;
        }

        if (action == ObserverUpdateActions.RemoveAction) {
            adapter.removeAt(findPositionByUUid(adapter, scene.getUid()));
        } else if (action == ObserverUpdateActions.AddAction || action == ObserverUpdateActions.UpdateAction) {
            adapter.addItem(scene, DevicePort.TOGGLE);
        } else if (action == ObserverUpdateActions.ClearAndNewAction || action == ObserverUpdateActions.RemoveAllAction) {
            adapterSource.updateNow();
            return true;
        }

        adapterSource.sourceChanged();

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
