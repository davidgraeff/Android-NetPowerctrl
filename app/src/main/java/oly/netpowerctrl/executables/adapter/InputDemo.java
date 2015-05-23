package oly.netpowerctrl.executables.adapter;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.executables.ExecutableAndCommand;
import oly.netpowerctrl.network.ReachabilityStates;
import oly.netpowerctrl.scenes.Scene;

/**
 * Adapter with some demo executables.
 */
public class InputDemo extends AdapterInput {
    private List<Scene> demoItems = new ArrayList<>();

    public InputDemo() {
        demoItems.add(createScene("Demo 1", ReachabilityStates.Reachable, false));
        demoItems.add(createScene("Demo 2", ReachabilityStates.NotReachable, false));
    }

    private Scene createScene(String name, ReachabilityStates isReachable, boolean isOn) {
        Scene scene = Scene.createNewScene();
        scene.title = name;
        scene.min_value = 0;
        scene.max_value = 1;
        scene.current_value = (isOn ? 1 : 0);
        scene.setReachable(isReachable);
        return scene;
    }

    @Override
    public void doUpdateNow() {
        for (Scene scene : demoItems)
            adapterSource.addItem(scene, ExecutableAndCommand.TOGGLE);
    }

    @Override
    void onStart(DataService dataService) {
        doUpdateNow();
    }

    @Override
    void onFinish() {

    }
}
