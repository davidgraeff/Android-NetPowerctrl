package oly.netpowerctrl.pluginservice;

import android.content.Context;
import android.support.annotation.Nullable;

import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.devices.EditDeviceInterface;
import oly.netpowerctrl.network.onExecutionFinished;
import oly.netpowerctrl.network.onHttpRequestResult;
import oly.netpowerctrl.timer.Timer;
import oly.netpowerctrl.timer.TimerCollection;

/**
 * This interface defines a plugin
 */
public interface PluginInterface {
    ////////////// Life cycle //////////////
    void onDestroy();

    void onStart(PluginService service);

    ////////////// Request data and executeToggle //////////////
    void requestData();

    void requestData(Device device, int device_connection_id);

    void execute(DevicePort port, final int command, onExecutionFinished callback);

    void addToTransaction(DevicePort port, final int command);

    void executeTransaction(onExecutionFinished callback);

    void rename(DevicePort port, final String new_name, final onHttpRequestResult callback);

    ////////////// Auxilary //////////////
    String getPluginID();

    void openConfigurationPage(Device device, Context context);

    void showConfigureDeviceScreen(Device device);

    @Nullable
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

    boolean isNetworkReducedState();

    boolean isNetworkPlugin();

    ////////////// Alarms //////////////
    Timer getNextFreeAlarm(DevicePort port, int type);

    void saveAlarm(Timer timer, final onHttpRequestResult callback);

    void removeAlarm(Timer timer, final onHttpRequestResult callback);

    void requestAlarms(DevicePort port, TimerCollection timerCollection);
}
