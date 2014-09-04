package oly.netpowerctrl.scenes;

import java.util.UUID;

/**
 * Created by david on 02.09.14.
 */
public class SceneItem {
    public UUID uuid = UUID.randomUUID();
    public int command;

    public SceneItem() {
    }

    public SceneItem(UUID uuid, int command) {
        this.uuid = uuid;
        this.command = command;
    }
}
