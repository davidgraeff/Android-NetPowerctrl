package oly.netpowerctrl.plugins;

import java.util.ArrayList;

/**
 * Created by david on 03.01.14.
 */
public class PluginRemote {
    public static class RemoteIntValue {
        int min;
        int max;
        int value;
        String name;

        RemoteIntValue(String value_name,
                       int int_min_value,
                       int int_max_value,
                       int int_current_value) {
            this.min = int_min_value;
            this.max = int_max_value;
            this.value = int_current_value;
            this.name = value_name;
        }
    }

    public static class RemoteBooleanValue {
        boolean value;
        String name;

        RemoteBooleanValue(String value_name,
                           boolean current_value) {
            this.value = current_value;
            this.name = value_name;
        }
    }

    public ArrayList<RemoteIntValue> intValues = new ArrayList<RemoteIntValue>();
    public ArrayList<RemoteBooleanValue> booleanValues = new ArrayList<RemoteBooleanValue>();
    public int pluginId;
    public String serviceName;
    public String localized_name;

    PluginRemote(int pluginId, String serviceName, String localized_name) {
        this.pluginId = pluginId;
        this.localized_name = localized_name;
        this.serviceName = serviceName;
    }

    public void addRemoteIntValue(String value_name,
                                  int int_min_value,
                                  int int_max_value,
                                  int int_current_value) {
        intValues.add(new RemoteIntValue(value_name, int_min_value, int_max_value, int_current_value));
    }

    public void addRemoteBooleanValue(String value_name,
                                      boolean current_value) {
        booleanValues.add(new RemoteBooleanValue(value_name, current_value));
    }
}
