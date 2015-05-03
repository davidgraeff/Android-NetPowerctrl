package oly.netpowerctrl.executables.adapter;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.network.ReachabilityStates;
import oly.netpowerctrl.scenes.Scene;

;

/**
 * Created by david on 07.07.14.
 */
public class AdapterSourceInputDemo extends AdapterSourceInput {
    private List<Scene> demoItems = new ArrayList<>();

    public AdapterSourceInputDemo() {
        demoItems.add(createScene("Demo 1", ReachabilityStates.Reachable, false));
        demoItems.add(createScene("Demo 2", ReachabilityStates.NotReachable, false));
    }

    private Scene createScene(String name, ReachabilityStates isReachable, boolean isOn) {
        Scene scene = Scene.createNewScene();
        scene.title = name;
        scene.max_value = (1);
        scene.current_value = (isOn ? 1 : 0);
        scene.setReachable(isReachable);
        return scene;
    }

    @Override
    public void doUpdateNow() {
        for (Scene scene : demoItems)
            adapterSource.addItem(scene, Executable.TOGGLE);
    }

    @Override
    void onStart(DataService dataService) {
        doUpdateNow();
    }

    @Override
    void onFinish() {

    }
}
