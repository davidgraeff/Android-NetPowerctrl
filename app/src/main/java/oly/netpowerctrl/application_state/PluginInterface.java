package oly.netpowerctrl.application_state;

import android.content.Context;

import oly.netpowerctrl.alarms.Alarm;
import oly.netpowerctrl.alarms.TimerController;
import oly.netpowerctrl.device_ports.DevicePort;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.devices.DeviceConnection;
import oly.netpowerctrl.network.AsyncRunnerResult;
import oly.netpowerctrl.network.ExecutionFinished;

/**
 * This interface defines a plugin
 */
public interface PluginInterface {
    ////////////// Life cycle //////////////
    void onDestroy();

    void onStart(NetpowerctrlService service);

    ////////////// Request data and execute //////////////
    void requestData();

    void requestData(DeviceConnection ci);

    void execute(DevicePort port, final int command, ExecutionFinished callback);

    void addToTransaction(DevicePort port, final int command);

    void executeTransaction(ExecutionFinished callback);

    void rename(DevicePort port, final String new_name, final AsyncRunnerResult callback);

    ////////////// Auxilary //////////////
    String getPluginID();

    void openConfigurationPage(Device device, Context context);

    ////////////// Reduce power consumption //////////////

    /**
     * Restart receiving units of the plugin. Necessary for creating/configuring new devices (with
     * for example changed receive ports)
     *
     * @param device Maybe null if you want to restart all receiving units of the plugin or name a
     *               device to start only the receiving unit responsible for that device.
     */
    void enterFullNetworkState(Device device);

    void enterNetworkReducedState();

    boolean isNetworkPlugin();

    ////////////// Alarms //////////////
    Alarm getNextFreeAlarm(DevicePort port, int type);

    void saveAlarm(Alarm alarm, final AsyncRunnerResult callback);

    void removeAlarm(Alarm alarm, final AsyncRunnerResult callback);

    void requestAlarms(DevicePort port, TimerController timerController);
}
