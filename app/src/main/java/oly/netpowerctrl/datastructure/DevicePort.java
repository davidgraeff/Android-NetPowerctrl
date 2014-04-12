package oly.netpowerctrl.datastructure;

import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.utils.Icons;

/**
 * Base class for actions (IO, Outlet, ...)
 */
public final class DevicePort implements Comparable {
    // Some value constants
    public static final int OFF = 0;
    public static final int ON = 1;
    public static final int TOGGLE = -1;
    // Action specific
    public int current_value = 0;
    public int max_value = ON;
    public int min_value = OFF;
    public boolean Hidden = false;
    public boolean Disabled = false;
    public UUID uuid = UUID.randomUUID(); // unique identity among all device ports
    public List<UUID> groups = new ArrayList<UUID>();
    public long id = 0; // unique identity among device ports on this device
    // last_command_timecode: Updated after name has been send.
    // Used to disable control in list until ack from device has been received.
    public long last_command_timecode = 0;
    // UI specific
    public int positionRequest;

    // Device
    public DeviceInfo device;
    protected DevicePortType ui_type;
    private String Description = "";


    public DevicePort(DeviceInfo di, DevicePortType ui_type) {
        device = di;
        this.ui_type = ui_type;
    }

    public static DevicePort fromJSON(JsonReader reader, DeviceInfo di)
            throws IOException, ClassNotFoundException {
        reader.beginObject();
        DevicePort oi = new DevicePort(di, DevicePortType.TypeUnknown);
        oi.last_command_timecode = di.getUpdatedTime();

        while (reader.hasNext()) {
            String name = reader.nextName();

            assert name != null;

            if (name.equals("Type")) {
                int t = reader.nextInt();
                if (t > DevicePortType.values().length)
                    throw new ClassNotFoundException();
                oi.ui_type = DevicePortType.values()[t];
            } else if (name.equals("Description")) {
                oi.Description = reader.nextString();
            } else if (name.equals("Value")) {
                oi.current_value = reader.nextInt();
            } else if (name.equals("max_value")) {
                oi.max_value = reader.nextInt();
            } else if (name.equals("min_value")) {
                oi.min_value = reader.nextInt();
            } else if (name.equals("Disabled")) {
                oi.Disabled = reader.nextBoolean();
            } else if (name.equals("Hidden")) {
                oi.Hidden = reader.nextBoolean();
            } else if (name.equals("positionRequest")) {
                oi.positionRequest = reader.nextInt();
            } else if (name.equals("id")) {
                oi.id = reader.nextLong();
            } else if (name.equals("uuid")) {
                oi.uuid = UUID.fromString(reader.nextString());
            } else if (name.equals("groups")) {
                oi.groups.clear();
                reader.beginArray();
                while (reader.hasNext()) {
                    oi.groups.add(UUID.fromString(reader.nextString()));
                }
                reader.endArray();
            } else {
                reader.skipValue();
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
        return (id == other.id) && device.equals(device);
    }

    public boolean copyValuesIfMatching(DevicePort source_oi) {
        if (equals(source_oi)) {
            current_value = source_oi.current_value;
            max_value = source_oi.max_value;
            min_value = source_oi.min_value;
            Disabled = source_oi.Disabled;
            setDescription(source_oi.getDescription());
            return true;
        }
        return false;
    }

    public DevicePortType getType() {
        return ui_type;
    }

    protected void clone(DevicePort other) {
        Description = other.Description;
        current_value = other.current_value;
        max_value = other.max_value;
        min_value = other.min_value;
        Disabled = other.Disabled;
        Hidden = other.Hidden;
        positionRequest = other.positionRequest;
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
        writer.endObject();
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
    public void setDescription(String desc) {
        Description = desc;
    }

    public void addToGroup(UUID uuid) {
        if (!groups.contains(uuid))
            groups.add(uuid);
    }

    public enum DevicePortType {
        TypeUnknown, TypeRangedValue, TypeToggle, TypeButton
    }

    public Icons.IconState getIconState() {
        Icons.IconState t = Icons.IconState.StateOff;
        if (current_value != min_value &&
                (getType() == DevicePort.DevicePortType.TypeToggle ||
                        getType() == DevicePort.DevicePortType.TypeRangedValue))
            t = Icons.IconState.StateOn;
        return t;
    }
}
