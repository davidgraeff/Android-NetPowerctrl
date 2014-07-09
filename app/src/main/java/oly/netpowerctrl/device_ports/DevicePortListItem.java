package oly.netpowerctrl.device_ports;

import oly.netpowerctrl.devices.DevicePort;

/**
 * Created by david on 07.07.14.
 */
class DevicePortListItem {
    // unique id for the gridView
    public final long id;
    DevicePort port;
    String displayText;
    // If you change a DevicePort's value, that new value may be stored in
    // command_value instead overwriting DevicePort's value. The implementation
    // depends on the child class.
    int command_value;
    boolean marked_removed = false;

    DevicePortListItem(DevicePort oi, int command_value, long id) {
        this.id = id;
        this.port = oi;
        this.command_value = command_value;
        displayText = oi.device.DeviceName + ": " + oi.getDescription();
    }

    public boolean isEnabled() {
        return port.last_command_timecode <= port.device.getUpdatedTime();
    }

    public void clearState() {
        marked_removed = false;
    }

    public void setPort(DevicePort oi) {
        this.port = oi;
        displayText = oi.device.DeviceName + ": " + oi.getDescription();
    }

    public void markRemoved() {
        marked_removed = true;
    }

    public boolean isMarkedRemoved() {
        return marked_removed;
    }
}
