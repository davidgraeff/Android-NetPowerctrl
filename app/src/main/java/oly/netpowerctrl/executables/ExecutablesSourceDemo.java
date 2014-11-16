package oly.netpowerctrl.executables;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.scenes.Scene;

/**
 * Created by david on 07.07.14.
 */
public class ExecutablesSourceDemo extends ExecutablesSourceBase {
    private List<Scene> demoItems = new ArrayList<>();

    public ExecutablesSourceDemo(ExecutablesSourceChain executablesSourceChain) {
        super(executablesSourceChain);
        demoItems.add(createScene("Demo 1", true, false));
        demoItems.add(createScene("Demo 2", false, false));
    }

    @Override
    public int doCountIfGroup(UUID uuid) {
        return 2;
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
    public void fullUpdate(ExecutablesBaseAdapter adapter) {
        if (adapter != null) {
            for (Scene scene : demoItems)
                adapter.addItem(scene, DevicePort.TOGGLE);
        }
    }

    @Override
    protected void automaticUpdatesDisable() {
    }

    @Override
    protected void automaticUpdatesEnable() {
    }
}
