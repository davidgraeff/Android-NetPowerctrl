package oly.netpowerctrl.data;

import oly.netpowerctrl.device_base.device.Device;

/**
 * Implemented by PluginService
 */
public interface WakeUpDeviceInterface {
    boolean wakeupPlugin(Device device);
}
