package oly.netpowerctrl.executables;

import java.util.UUID;

import oly.netpowerctrl.data.Executable;

/**
 * Created by david on 07.07.14.
 */
public class ExecutableAdapterItem {
    // unique id for the gridView
    public final long id;
    /// Group related
    public int groupItems = 0;
    public Executable executable;
    String groupName;
    // If you change a DevicePort's value, that new value may be stored in
    // command_value instead overwriting DevicePort's value. The implementation
    // depends on the child class.
    int command_value;
    private boolean marked_removed = false;
    private UUID group = null;
    private groupTypeEnum groupType = groupTypeEnum.NOGROUP_TYPE;

    public ExecutableAdapterItem(Executable executable, int command_value, long id) {
        this.id = id;
        this.executable = executable;
        this.command_value = command_value;
    }

    // A group item
    public ExecutableAdapterItem(UUID group, String name, int id) {
        this.id = id;
        this.groupName = name;
        this.group = group;
        this.groupType = groupTypeEnum.GROUP_TYPE;
    }

    public int getCommand_value() {
        return command_value;
    }

    public void setCommand_value(int command_value) {
        this.command_value = command_value;
    }

    public void clearMarkRemoved() {
        marked_removed = false;
    }

    /**
     * Mark item to be removed. Group header items are not affected
     */
    public void markRemoved() {
//        if (group != null)
//            return;
        marked_removed = true;
    }

    public boolean isMarkedRemoved() {
        return marked_removed;
    }

    public boolean isGroup(UUID group) {
        return (groupType == groupTypeEnum.GROUP_TYPE)
                && group.equals(this.group);
    }

    public groupTypeEnum groupType() {
        return groupType;
    }

    void setGroupType(groupTypeEnum groupType) {
        this.groupType = groupType;
    }

    public UUID groupID() {
        return group;
    }

    public int getItemViewType() {
        if (groupType() == ExecutableAdapterItem.groupTypeEnum.NOGROUP_TYPE)
            return executable.getType().ordinal();
        else
            return groupType().ordinal() + 100;
    }

    public String getExecutableUid() {
        if (executable == null)
            return null;
        return executable.getUid();
    }

    public Executable getExecutable() {
        return executable;
    }

    public void setExecutable(Executable executable) {
        this.executable = executable;
    }


    public enum groupTypeEnum {NOGROUP_TYPE, GROUP_TYPE}
}
