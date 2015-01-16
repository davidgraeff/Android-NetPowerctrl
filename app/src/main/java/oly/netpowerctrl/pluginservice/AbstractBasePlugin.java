package oly.netpowerctrl.pluginservice;

import android.content.Context;
import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;

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
    protected onPluginReady pluginReady = null;
    protected WeakReference<onPluginFinished> pluginFinished = new WeakReference<>(null);

    protected AbstractBasePlugin(PluginService pluginService) {
        this.pluginService = pluginService;
    }

    protected abstract void checkReady();

    protected void registerReadyObserver(onPluginReady pluginReady) {
        this.pluginReady = pluginReady;
        checkReady();
    }

    protected void registerFinishedObserver(onPluginFinished pluginFinished) {
        this.pluginFinished = new WeakReference<>(pluginFinished);
    }

    public PluginService getPluginService() {
        return pluginService;
    }

    ////////////// Life cycle //////////////
    abstract public void onDestroy();

    abstract public void onStart(Context context);

    ////////////// Request data and executeToggle //////////////
    abstract public void requestData(DeviceQuery deviceQuery);

    abstract public void requestData(Device device, int device_connection_id);

    abstract public boolean execute(DevicePort port, final int command, onExecutionFinished callback);

    abstract public void addToTransaction(DevicePort port, final int command);

    abstract public void executeTransaction(onExecutionFinished callback);

    abstract public void rename(DevicePort port, final String new_name, final onHttpRequestResult callback);

    ////////////// Auxiliary //////////////
    abstract public String getPluginID();

    abstract public String getLocalizedName();

    abstract public void openConfigurationPage(Device device, Context context);

    abstract public void showConfigureDeviceScreen(Device device);

    @Nullable
    abstract public EditDeviceInterface openEditDevice(Device device);

    abstract public void devicesChanged();

    abstract public boolean isStarted();

    ////////////// Alarms //////////////
    abstract public Timer getNextFreeAlarm(DevicePort port, int type, int command);

    abstract public void saveAlarm(Timer timer, final onHttpRequestResult callback);

    abstract public void removeAlarm(Timer timer, final onHttpRequestResult callback);

    abstract public void requestAlarms(DevicePort port, TimerCollection timerCollection);
}
