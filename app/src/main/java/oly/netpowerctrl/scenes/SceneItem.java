package oly.netpowerctrl.scenes;

/**
 * Created by david on 02.09.14.
 */
public class SceneItem {
    public String uuid;
    public int command;

    public SceneItem() {
    }

    public SceneItem(String uuid, int command) {
        this.uuid = uuid;
        this.command = command;
    }
}
