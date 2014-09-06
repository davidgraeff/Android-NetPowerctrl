package oly.netpowerctrl.listen_service;

import android.content.Context;

import oly.netpowerctrl.device_ports.DevicePort;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.devices.DeviceConnection;
import oly.netpowerctrl.devices.EditDeviceInterface;
import oly.netpowerctrl.network.onAsyncRunnerResult;
import oly.netpowerctrl.network.onExecutionFinished;
import oly.netpowerctrl.timer.Timer;
import oly.netpowerctrl.timer.TimerController;

/**
 * This interface defines a plugin
 */
public interface PluginInterface {
    ////////////// Life cycle //////////////
    void onDestroy();

    void onStart(ListenService service);

    ////////////// Request data and execute //////////////
    void requestData();

    void requestData(DeviceConnection ci);

    void execute(DevicePort port, final int command, onExecutionFinished callback);

    void addToTransaction(DevicePort port, final int command);

    void executeTransaction(onExecutionFinished callback);

    void rename(DevicePort port, final String new_name, final onAsyncRunnerResult callback);

    ////////////// Auxilary //////////////
    String getPluginID();

    void openConfigurationPage(Device device, Context context);

    void showConfigureDeviceScreen(Device device);

    EditDeviceInterface openEditDevice(Device device);

    ////////////// Reduce power consumption //////////////

    /**
     * Restart receiving units of the plugin. Necessary for creating/configuring new devices (with
     * for example changed receive ports)
     *
     * @param context
     * @param device  Maybe null if you want to restart all receiving units of the plugin or name a
     */
    void enterFullNetworkState(Context context, Device device);

    void enterNetworkReducedState(Context context);

    boolean isNetworkPlugin();

    ////////////// Alarms //////////////
    Timer getNextFreeAlarm(DevicePort port, int type);

    void saveAlarm(Timer timer, final onAsyncRunnerResult callback);

    void removeAlarm(Timer timer, final onAsyncRunnerResult callback);

    void requestAlarms(DevicePort port, TimerController timerController);

}
