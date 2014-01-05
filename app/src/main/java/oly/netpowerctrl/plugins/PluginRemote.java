package oly.netpowerctrl.plugins;

import java.util.ArrayList;

/**
 * Created by david on 03.01.14.
 */
public class PluginRemote {
    public static class RemoteValue {
        int min;
        int max;
        int value;
        String name;

        int itemId;
    }

    public ArrayList<RemoteValue> values = new ArrayList<RemoteValue>();
    public int pluginId;


}
