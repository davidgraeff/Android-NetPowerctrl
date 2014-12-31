package oly.netpowerctrl.executables;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.device_base.executables.ExecutableReachability;
import oly.netpowerctrl.scenes.Scene;

/**
 * Created by david on 07.07.14.
 */
public class AdapterSourceInputDemo extends AdapterSourceInput {
    private List<Scene> demoItems = new ArrayList<>();

    public AdapterSourceInputDemo() {
        demoItems.add(createScene("Demo 1", ExecutableReachability.Reachable, false));
        demoItems.add(createScene("Demo 2", ExecutableReachability.NotReachable, false));
    }

    private Scene createScene(String name, ExecutableReachability isReachable, boolean isOn) {
        Scene scene = Scene.createNewScene();
        scene.sceneName = name;
        scene.setMaximumValue(1);
        scene.setCurrentValue(isOn ? 1 : 0);
        scene.setReachable(isReachable);
        return scene;
    }

    @Override
    public void doUpdateNow() {
        for (Scene scene : demoItems)
            adapterSource.addItem(scene, DevicePort.TOGGLE);
    }

    @Override
    void onStart(AppData appData) {
        doUpdateNow();
    }

    @Override
    void onFinish() {

    }
}
