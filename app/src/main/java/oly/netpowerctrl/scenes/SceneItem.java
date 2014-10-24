package oly.netpowerctrl.scenes;

import java.util.UUID;

/**
 * Created by david on 02.09.14.
 */
public class SceneItem {
    public String uuid;
    public int command;

    public SceneItem() {
        uuid = UUID.randomUUID().toString();
    }

    public SceneItem(String uuid, int command) {
        this.uuid = uuid;
        this.command = command;
    }
}
