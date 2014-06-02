package oly.netpowerctrl.application_state;

import android.content.Context;

import oly.netpowerctrl.alarms.Alarm;
import oly.netpowerctrl.devices.DeviceInfo;
import oly.netpowerctrl.devices.DevicePort;
import oly.netpowerctrl.network.AsyncRunnerResult;
import oly.netpowerctrl.network.ExecutionFinished;

/**
 * This interface defines a plugin
 */
public interface PluginInterface {
    void finish();

    void requestData();

    void requestData(DeviceInfo di);

    void execute(DevicePort port, final int command, ExecutionFinished callback);

    void rename(DevicePort port, final String new_name, final AsyncRunnerResult callback);

    void addToTransaction(DevicePort port, final int command);

    void executeTransaction(ExecutionFinished callback);

    void requestAlarms(DeviceInfo di);

    String getPluginID();

    /**
     * Restart receiving units of the plugin. Necessary for creating/configuring new devices (with
     * for example changed receive ports)
     *
     * @param device Maybe null if you want to restart all receiving units of the plugin or name a
     *               device to start only the receiving unit responsible for that device.
     */
    void prepareForDevices(DeviceInfo device);

    void openConfigurationPage(DeviceInfo device, Context context);

    boolean isNetworkPlugin();

    Alarm getNextFreeAlarm(DevicePort port);

    void saveAlarm(Alarm alarm, final AsyncRunnerResult callback);
}
