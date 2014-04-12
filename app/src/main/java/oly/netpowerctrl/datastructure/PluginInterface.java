package oly.netpowerctrl.datastructure;

import oly.netpowerctrl.network.DevicePortRenamed;

/**
 * Created by david on 10.04.14.
 */
public interface PluginInterface {
    void finish();

    void requestData();

    void requestData(DeviceInfo di);

    void execute(DevicePort port, final int command, ExecutionFinished callback);

    void rename(DevicePort port, final String new_name, final DevicePortRenamed callback);

    void addToTransaction(DevicePort port, final int command);

    void executeTransaction(ExecutionFinished callback);

    String getPluginID();

    /**
     * Restart receiving units of the plugin. Necessary for creating/configuring new devices (with
     * for example changed receive ports)
     *
     * @param device Maybe null if you want to restart all receiving units of the plugin or name a
     *               device to start only the receiving unit responsible for that device.
     */
    void prepareForDevices(DeviceInfo device);
}
