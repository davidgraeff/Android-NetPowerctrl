package oly.netpowerctrl.executables;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.scenes.Scene;

/**
 * Created by david on 07.07.14.
 */
public class AdapterSourceInputDemo extends AdapterSourceInput {
    private List<Scene> demoItems = new ArrayList<>();

    public AdapterSourceInputDemo() {
        demoItems.add(createScene("Demo 1", true, false));
        demoItems.add(createScene("Demo 2", false, false));
    }

    private Scene createScene(String name, boolean isReachable, boolean isOn) {
        Scene scene = Scene.createNewSzene();
        scene.sceneName = name;
        scene.setMaximumValue(1);
        scene.setCurrentValue(isOn ? 1 : 0);
        scene.setReachable(isReachable);
        return scene;
    }

    @Override
    public void doUpdateNow(@NonNull ExecutablesBaseAdapter adapter) {
        for (Scene scene : demoItems)
            adapter.addItem(scene, DevicePort.TOGGLE);
    }

    @Override
    void onStart(AppData appData) {

    }

    @Override
    void onFinish() {

    }
}
