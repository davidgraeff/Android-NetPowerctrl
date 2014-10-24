package oly.netpowerctrl.devices;

import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.data.Executable;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.executables.ExecutableType;

/**
 * Base class for actions (IO, Outlet, ...)
 */
public final class DevicePort implements Comparable, Executable {
    // Some value constants
    public static final int OFF = 0;
    public int min_value = OFF;
    public static final int ON = 1;
    public int max_value = ON;
    public static final int TOGGLE = -1;
    public static final int INVALID = -2;
    public final Device device;
    public final List<UUID> groups;
    public int current_value = 0;
    public boolean Disabled = false;

    public int id = 0; // unique identity among device ports on this device
    // Used to disable control in list until ack from device has been received.
    public long last_command_timecode = 0; // Updated after name has been send.
    private String uuid; // unique identity among all device ports
    // UI specific
    private ExecutableType ui_type;
    private String Description = "";

    public DevicePort(Device di, ExecutableType ui_type) {
        device = di;
        this.ui_type = ui_type;
        groups = new ArrayList<>();
        uuid = UUID.randomUUID().toString();
    }

    public DevicePort(DevicePort other) {
        device = other.device;
        groups = new ArrayList<>(other.groups);

        max_value = other.max_value;
        min_value = other.min_value;
        current_value = other.current_value;
        Disabled = other.Disabled;

        id = other.id;
        uuid = UUID.randomUUID().toString();

        last_command_timecode = other.last_command_timecode;

        Description = other.Description;
        ui_type = other.ui_type;
        // do not copy last_command_timecode! This value is set by the executeToggle(..) methods
    }

    public static DevicePort fromJSON(JsonReader reader, Device di)
            throws IOException, ClassNotFoundException {
        reader.beginObject();
        DevicePort oi = new DevicePort(di, ExecutableType.TypeUnknown);
        oi.last_command_timecode = di.getUpdatedTime();

        while (reader.hasNext()) {
            String name = reader.nextName();

            assert name != null;

            switch (name) {
                case "Type":
                    int t = reader.nextInt();
                    if (t > ExecutableType.values().length)
                        throw new ClassNotFoundException();
                    oi.ui_type = ExecutableType.values()[t];
                    break;
                case "Description":
                    oi.Description = reader.nextString();
                    break;
                case "Value":
                    oi.current_value = reader.nextInt();
                    break;
                case "max_value":
                    oi.max_value = reader.nextInt();
                    break;
                case "min_value":
                    oi.min_value = reader.nextInt();
                    break;
                case "Disabled":
                    oi.Disabled = reader.nextBoolean();
                    break;
                case "id":
                    oi.id = reader.nextInt();
                    break;
                case "uuid":
                    oi.uuid = reader.nextString();
                    break;
                case "groups":
                    oi.groups.clear();
                    reader.beginArray();
                    while (reader.hasNext()) {
                        oi.groups.add(UUID.fromString(reader.nextString()));
                    }
                    reader.endArray();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }

        reader.endObject();
        return oi;
    }

    @Override
    public List<UUID> getGroups() {
        return groups;
    }

    @Override
    public String getUid() {
        return uuid;
    }

    public boolean isEnabled() {
        return last_command_timecode <= device.getUpdatedTime();
    }

    @Override
    public int compareTo(Object o) {
        DevicePort other = (DevicePort) o;
        if (other.equals(this))
            return 0;

        return getTitle().compareTo(other.getTitle());
    }

    public boolean equals(DevicePort other) {
        return (other != null) && (id == other.id) && device.equalsByUniqueID(other.device);
    }

    /**
     * @param source_oi
     * @return Return true if values in this object have changed because of source_oi.
     */
    public boolean copyValues(DevicePort source_oi) {
        // We update the command timecode here, too.
        last_command_timecode = source_oi.device.getUpdatedTime();
        boolean hasChanged;
        hasChanged = current_value != source_oi.current_value;
        current_value = source_oi.current_value;
        hasChanged |= max_value != source_oi.max_value;
        max_value = source_oi.max_value;
        hasChanged |= min_value != source_oi.min_value;
        min_value = source_oi.min_value;
        hasChanged |= Disabled != source_oi.Disabled;
        Disabled = source_oi.Disabled;
        hasChanged |= setTitle(source_oi.getTitle());
        return hasChanged;
    }

    public ExecutableType getType() {
        return ui_type;
    }

    public void toJSON(JsonWriter writer, boolean addDeviceID) throws IOException {
        writer.beginObject();
        writer.name("Type").value(ui_type.ordinal());
        writer.name("Description").value(Description);
        writer.name("Value").value(current_value);
        writer.name("max_value").value(max_value);
        writer.name("min_value").value(min_value);
        writer.name("Disabled").value(Disabled);
        writer.name("id").value(id);
        writer.name("uuid").value(uuid);
        writer.name("groups").beginArray();
        for (UUID group_uuid : groups)
            writer.value(group_uuid.toString());
        writer.endArray();
        if (addDeviceID)
            writer.name("device_id").value(device.UniqueDeviceID);
        writer.endObject();
    }

//    public String debugOut() {
//        return Description + " " + String.valueOf(current_value);
//    }

    public String getDescription() {
        return device.DeviceName;
    }

    @Override
    public String getTitle() {
        return this.Description;
    }

    @Override
    public boolean isReachable() {
        return device.getFirstReachableConnection() != null;
    }

    /**
     * Please be aware that if you do not request the description
     * change directly on the device, the change you make here will
     * be overridden on next device update!
     *
     * @param desc The new descriptive name.
     */
    public boolean setTitle(String desc) {
        boolean hasChanged = !desc.equals(Description);
        Description = desc;
        return hasChanged;
    }

    public void addToGroup(UUID uuid) {
        if (!groups.contains(uuid))
            groups.add(uuid);
    }

    public int getCurrentValueToggled() {
        return current_value > min_value ? min_value : max_value;
    }

    public LoadStoreIconData.IconState getIconState() {
        LoadStoreIconData.IconState t = LoadStoreIconData.IconState.StateOff;
        if (current_value != min_value &&
                (getType() == ExecutableType.TypeToggle ||
                        getType() == ExecutableType.TypeRangedValue))
            t = LoadStoreIconData.IconState.StateOn;
        return t;
    }

}
