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
public abstract class AbstractBasePlugin {
    protected final PluginService pluginService;

    protected AbstractBasePlugin(PluginService pluginService) {
        this.pluginService = pluginService;
    }

    public PluginService getPluginService() {
        return pluginService;
    }

    ////////////// Life cycle //////////////
    abstract public void onDestroy();

    abstract public void onStart();

    ////////////// Request data and executeToggle //////////////
    abstract public void requestData();

    abstract public void requestData(Device device, int device_connection_id);

    abstract public boolean execute(DevicePort port, final int command, onExecutionFinished callback);

    abstract public void addToTransaction(DevicePort port, final int command);

    abstract public void executeTransaction(onExecutionFinished callback);

    abstract public void rename(DevicePort port, final String new_name, final onHttpRequestResult callback);

    ////////////// Auxiliary //////////////
    abstract public String getPluginID();

    abstract public void openConfigurationPage(Device device, Context context);

    abstract public void showConfigureDeviceScreen(Device device);

    @Nullable
    abstract public EditDeviceInterface openEditDevice(Device device);

    ////////////// Reduce power consumption //////////////

    /**
     * Restart receiving units of the plugin. Necessary for creating/configuring new devices (with
     * for example changed receive ports)
     *
     * @param context
     * @param device  Maybe null if you want to restart all receiving units of the plugin or name a
     */
    abstract public void enterFullNetworkState(Context context, Device device);

    abstract public void enterNetworkReducedState(Context context);

    abstract public boolean isNetworkReducedState();

    abstract public boolean isNetworkPlugin();

    ////////////// Alarms //////////////
    abstract public Timer getNextFreeAlarm(DevicePort port, int type, int command);

    abstract public void saveAlarm(Timer timer, final onHttpRequestResult callback);

    abstract public void removeAlarm(Timer timer, final onHttpRequestResult callback);

    abstract public void requestAlarms(DevicePort port, TimerCollection timerCollection);
}
