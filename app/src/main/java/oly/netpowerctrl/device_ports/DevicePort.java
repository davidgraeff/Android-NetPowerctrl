package oly.netpowerctrl.device_ports;

import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.devices.Device;

/**
 * Base class for actions (IO, Outlet, ...)
 */
public final class DevicePort implements Comparable {
    // Some value constants
    public static final int OFF = 0;
    public int min_value = OFF;
    public static final int ON = 1;
    public int max_value = ON;
    public static final int TOGGLE = -1;
    public static final int INVALID = -2;
    public final List<UUID> groups = new ArrayList<>();
    // Device
    public final Device device;
    // Action specific
    public int current_value = 0;
    public boolean Hidden = false;
    public boolean Disabled = false;
    public UUID uuid = UUID.randomUUID(); // unique identity among all device ports
    public int id = 0; // unique identity among device ports on this device
    // last_command_timecode: Updated after name has been send.
    // Used to disable control in list until ack from device has been received.
    public long last_command_timecode = 0;
    // UI specific
    public int positionRequest;
    private DevicePortType ui_type;
    private String Description = "";
    private List<UUID> slaves = new ArrayList<>();


    public DevicePort(Device di, DevicePortType ui_type) {
        device = di;
        this.ui_type = ui_type;
    }

    public DevicePort(DevicePort other) {
        Description = other.Description;
        current_value = other.current_value;
        max_value = other.max_value;
        min_value = other.min_value;
        Disabled = other.Disabled;
        Hidden = other.Hidden;
        positionRequest = other.positionRequest;
        ui_type = other.ui_type;
        device = other.device;
        slaves = other.slaves;
        id = other.id;
        // do not copy last_command_timecode! This value is set by the execute(..) methods
    }

    public static DevicePort fromJSON(JsonReader reader, Device di)
            throws IOException, ClassNotFoundException {
        reader.beginObject();
        DevicePort oi = new DevicePort(di, DevicePortType.TypeUnknown);
        oi.last_command_timecode = di.getUpdatedTime();

        while (reader.hasNext()) {
            String name = reader.nextName();

            assert name != null;

            switch (name) {
                case "Type":
                    int t = reader.nextInt();
                    if (t > DevicePortType.values().length)
                        throw new ClassNotFoundException();
                    oi.ui_type = DevicePortType.values()[t];
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
                case "Hidden":
                    oi.Hidden = reader.nextBoolean();
                    break;
                case "positionRequest":
                    oi.positionRequest = reader.nextInt();
                    break;
                case "id":
                    oi.id = reader.nextInt();
                    break;
                case "uuid":
                    oi.uuid = UUID.fromString(reader.nextString());
                    break;
                case "slaves":
                    oi.slaves.clear();
                    reader.beginArray();
                    while (reader.hasNext()) {
                        oi.slaves.add(UUID.fromString(reader.nextString()));
                    }
                    reader.endArray();
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
    public int compareTo(Object o) {
        DevicePort other = (DevicePort) o;
        if (other.equals(this))
            return 0;

        if (positionRequest == 0 && other.positionRequest == 0) {
            return getDescription().compareTo(other.getDescription());
        } else {
            return positionRequest < other.positionRequest ? -1 : 1;
        }
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
        hasChanged |= setDescription(source_oi.getDescription());
        return hasChanged;
    }

    public DevicePortType getType() {
        return ui_type;
    }


    public void toJSON(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("Type").value(ui_type.ordinal());
        writer.name("Description").value(Description);
        writer.name("Value").value(current_value);
        writer.name("max_value").value(max_value);
        writer.name("min_value").value(min_value);
        writer.name("Disabled").value(Disabled);
        writer.name("Hidden").value(Hidden);
        writer.name("positionRequest").value(positionRequest);
        writer.name("id").value(id);
        writer.name("uuid").value(uuid.toString());
        writer.name("groups").beginArray();
        for (UUID group_uuid : groups)
            writer.value(group_uuid.toString());
        writer.endArray();
        writer.name("slaves").beginArray();
        for (UUID slave_uuid : slaves)
            writer.value(slave_uuid.toString());
        writer.endArray();
        writer.endObject();
    }

    public String debugOut() {
        return Description + " " + String.valueOf(current_value);
    }

    public String getDescription() {
        return this.Description;
    }

    /**
     * Please be aware that if you do not request the description
     * change directly on the device, the change you make here will
     * be overridden on next device update!
     *
     * @param desc The new descriptive name.
     */
    public boolean setDescription(String desc) {
        boolean hasChanged = !desc.equals(Description);
        Description = desc;
        return hasChanged;
    }

    public void addToGroup(UUID uuid) {
        if (!groups.contains(uuid))
            groups.add(uuid);
    }

    public List<UUID> getSlaves() {
        return slaves;
    }

    public void setSlaves(List<UUID> slaves) {
        this.slaves = slaves;
    }

    public int getCurrentValueToggled() {
        return current_value > min_value ? min_value : max_value;
    }

    public LoadStoreIconData.IconState getIconState() {
        LoadStoreIconData.IconState t = LoadStoreIconData.IconState.StateOff;
        if (current_value != min_value &&
                (getType() == DevicePort.DevicePortType.TypeToggle ||
                        getType() == DevicePort.DevicePortType.TypeRangedValue))
            t = LoadStoreIconData.IconState.StateOn;
        return t;
    }

    public enum DevicePortType {
        TypeUnknown, TypeRangedValue, TypeToggle, TypeButton
    }
}
