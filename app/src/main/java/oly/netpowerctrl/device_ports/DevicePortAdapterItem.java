package oly.netpowerctrl.device_ports;

import java.util.UUID;

/**
 * Created by david on 07.07.14.
 */
class DevicePortAdapterItem {
    // unique id for the gridView
    public final long id;
    /// Group related
    public int groupItems = 0;
    DevicePort port;
    String displayText;
    // If you change a DevicePort's value, that new value may be stored in
    // command_value instead overwriting DevicePort's value. The implementation
    // depends on the child class.
    int command_value;
    boolean marked_removed = false;
    private UUID group;
    private groupTypeEnum groupType = groupTypeEnum.NOGROUP_TYPE;

    DevicePortAdapterItem(DevicePort oi, int command_value, long id) {
        this.id = id;
        this.port = oi;
        this.command_value = command_value;
        displayText = oi.device.DeviceName + ": " + oi.getDescription();
    }

    // A group item
    public DevicePortAdapterItem(UUID group, String name, int id) {
        this.id = id;
        this.port = null;
        this.command_value = 0;
        this.displayText = name;
        this.group = group;
        this.groupType = groupTypeEnum.GROUP_TYPE;
    }

    public static DevicePortAdapterItem createGroupSpan(DevicePortAdapterItem c, int id) {
        DevicePortAdapterItem new_item = new DevicePortAdapterItem(c.group, c.displayText, id);
        new_item.setGroupType(groupTypeEnum.GROUP_SPAN_TYPE);
        return new_item;
    }

    public static DevicePortAdapterItem createGroupPreFillElemenet(DevicePortAdapterItem c, int id) {
        DevicePortAdapterItem new_item = new DevicePortAdapterItem(c.group, c.displayText, id);
        new_item.setGroupType(groupTypeEnum.PRE_GROUP_FILL_ELEMENT_TYPE);
        return new_item;
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

    /**
     * Mark item to be removed. Group header items are not affected
     */
    public void markRemoved() {
        if (group != null)
            return;
        marked_removed = true;
    }

    public boolean isMarkedRemoved() {
        return marked_removed;
    }

    public boolean isGroup(UUID group) {
        return groupType == groupTypeEnum.GROUP_TYPE && group.equals(this.group);
    }

    public groupTypeEnum groupType() {
        return groupType;
    }

    public void setGroupType(groupTypeEnum groupType) {
        this.groupType = groupType;
    }

    public enum groupTypeEnum {NOGROUP_TYPE, GROUP_TYPE, PRE_GROUP_FILL_ELEMENT_TYPE, GROUP_SPAN_TYPE}
}
