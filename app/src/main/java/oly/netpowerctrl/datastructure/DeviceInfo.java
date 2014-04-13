package oly.netpowerctrl.datastructure;

import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anel.AnelPlugin;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.application_state.PluginInterface;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.JSONHelper;

// An object of this class contains all the info about a specific device
public class DeviceInfo implements Comparable<DeviceInfo> {

    public UUID uuid;

    public String DeviceName; // name of the device as reported by UDP or configured by the user
    public String HostName;   // the hostname or ip address used to reach the device
    public String UniqueDeviceID; // the mac address as sent from the device

    public String UserName;
    public String Password;

    public boolean DefaultPorts;
    public int SendPort;
    public int ReceivePort;
    public int HttpPort;

    public List<DevicePort> DevicePorts;
    public String Temperature;
    public String FirmwareVersion;

    // Temporary state variables
    private boolean reachable = false;
    public String not_reachable_reason;
    public long updated = 0;
    public boolean configured = false;

    public boolean isReachable() {
        return reachable;
    }

    public void setReachable() {
        this.reachable = true;
        not_reachable_reason = "";
    }

    public void setNotReachable(String not_reachable_reason) {
        this.reachable = false;
        this.not_reachable_reason = not_reachable_reason;
    }

    @Override
    public int compareTo(DeviceInfo deviceInfo) {
        if (deviceInfo.uuid.equals(uuid))
            return 0;
        return 1;
    }

    private WeakReference<PluginInterface> pluginInterface = null;

    /**
     * Get "execution engine" for this device info, either by an existing reference to it or
     * by requesting it by the plugin controller.
     *
     * @return
     */
    public PluginInterface getPluginInterface() {
        PluginInterface pi = pluginInterface != null ? pluginInterface.get() : null;

        if (pi == null) {
            pi = NetpowerctrlApplication.getService().getPluginInterface(this);
            pluginInterface = new WeakReference<PluginInterface>(pi);
        }

        return pi;
    }

    public String pluginID;

    private DeviceInfo(String pluginID) {
        uuid = UUID.randomUUID();
        this.pluginID = pluginID;
        DeviceName = "";
        HostName = "";
        UniqueDeviceID = "";
        UserName = "";
        Password = "";
        DefaultPorts = true;
        SendPort = -1;
        ReceivePort = -1;
        HttpPort = 80;
        Temperature = "";
        FirmwareVersion = "";
        DevicePorts = new ArrayList<DevicePort>();
    }

    public static DeviceInfo createNewDevice(String pluginID) {
        DeviceInfo di = new DeviceInfo(pluginID);
        di.DeviceName = NetpowerctrlApplication.instance.getString(R.string.default_device_name);
        di.SendPort = SharedPrefs.getDefaultSendPort();
        di.ReceivePort = SharedPrefs.getDefaultReceivePort();
        return di;
    }

    public DeviceInfo(DeviceInfo other) {
        uuid = UUID.randomUUID();
        DeviceName = other.DeviceName;
        pluginID = other.pluginID;
        HostName = other.HostName;
        UniqueDeviceID = other.UniqueDeviceID;
        UserName = other.UserName;
        Password = other.Password;
        DefaultPorts = other.DefaultPorts;
        SendPort = other.SendPort;
        ReceivePort = other.ReceivePort;
        HttpPort = other.HttpPort;
        Temperature = other.Temperature;
        FirmwareVersion = other.FirmwareVersion;
        configured = other.configured;
        DevicePorts = new ArrayList<DevicePort>();
        for (DevicePort oi : other.DevicePorts) {
            DevicePort p = new DevicePort(this, oi.ui_type);
            p.clone(oi);
            DevicePorts.add(oi);
        }
    }

    /**
     * @param other Another DeviceInfo object from where to copy data from.
     * @return Return true if this object changed because of the values of "other".
     */
    public boolean copyFreshValues(DeviceInfo other) {
        if (other.equals(this)) {
            return false;
        }

        int hash_before = getHash();

        // Add all devicePorts from DeviceInfo other to a new list (so that we can modify)
        //List<DevicePort> new_devicePorts = new ArrayList<DevicePort>();
        //new_devicePorts.addAll(other.DevicePorts);

        // Iterators
        Iterator<DevicePort> current_iterator = DevicePorts.iterator();
        Iterator<DevicePort> new_iterator;

        // Update each current devicePort.
        while (current_iterator.hasNext()) {
            DevicePort current_devicePort = current_iterator.next();
            // Iterate over all new devicePorts to find the matching one.
            new_iterator = other.DevicePorts.iterator();
            boolean found = false;
            while (new_iterator.hasNext()) {
                DevicePort new_devicePort = new_iterator.next();
                // If update succeeded, remove entry from new_devicePorts list.
                if (current_devicePort.copyValuesIfMatching(new_devicePort)) {
                    found = true;
                    new_iterator.remove();
                    break;
                }
            }
            // If update failed because no matching devicePort can be found,
            // remove entry from the current list of devicePorts.
            if (!found)
                current_iterator.remove();
        }

        HostName = other.HostName;
        HttpPort = other.HttpPort;
        Temperature = other.Temperature;
        FirmwareVersion = other.FirmwareVersion;
        reachable = other.reachable;
        not_reachable_reason = other.not_reachable_reason;
        updated = other.updated;

        return getHash() != hash_before;
    }

    /**
     * Return true if both DeviceInfo objects refer to the same device, preferable if both
     * have a mac address set.
     *
     * @param other Compare to other DeviceInfo
     * @return
     */
    @SuppressWarnings("unused")
    public boolean equalsFunctional(DeviceInfo other) {
        if (UniqueDeviceID.isEmpty() || other.UniqueDeviceID.isEmpty())
            return HostName.equals(other.HostName) && ReceivePort == other.ReceivePort &&
                    DevicePorts.size() == other.DevicePorts.size();
        else
            return UniqueDeviceID.equals(other.UniqueDeviceID);
    }

    /**
     * Return true if this and the other DeviceInfo are the same configured DeviceInfo.
     *
     * @param other Compare to other DeviceInfo
     * @return
     */
    @SuppressWarnings("unused")
    public boolean equals(DeviceInfo other) {
        return uuid.equals(other.uuid);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof DeviceInfo)
            return uuid.equals(((DeviceInfo) other).uuid);
        return false;
    }

    public static DeviceInfo fromJSON(JsonReader reader) throws IOException, ClassNotFoundException {
        reader.beginObject();
        DeviceInfo di = new DeviceInfo("");
        di.configured = true;
        di.reachable = false;
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("uuid")) {
                di.uuid = UUID.fromString(reader.nextString());
            } else if (name.equals("DeviceName")) {
                di.DeviceName = reader.nextString();
            } else if (name.equals("HostName")) {
                di.HostName = reader.nextString();
            } else if (name.equals("UniqueDeviceID") || name.equals("MacAddress")) {
                di.UniqueDeviceID = reader.nextString();
            } else if (name.equals("UserName")) {
                di.UserName = reader.nextString();
            } else if (name.equals("Password")) {
                di.Password = reader.nextString();
            } else if (name.equals("Temperature")) {
                di.Temperature = reader.nextString();
            } else if (name.equals("FirmwareVersion")) {
                di.FirmwareVersion = reader.nextString();
            } else if (name.equals("DefaultPorts")) {
                di.DefaultPorts = reader.nextBoolean();
            } else if (name.equals("SendPort")) {
                di.SendPort = reader.nextInt();
            } else if (name.equals("Type")) {
                // For compatibility
                if (reader.peek() == JsonToken.NUMBER)
                    di.pluginID = AnelPlugin.PLUGIN_ID;
                else
                    di.pluginID = reader.nextString();
            } else if (name.equals("ReceivePort")) {
                di.ReceivePort = reader.nextInt();
            } else if (name.equals("HttpPort")) {
                di.HttpPort = reader.nextInt();
            } else if (name.equals("DevicePorts")) {
                di.DevicePorts.clear();
                reader.beginArray();
                while (reader.hasNext()) {
                    try {
                        di.DevicePorts.add(DevicePort.fromJSON(reader, di));
                    } catch (ClassNotFoundException e) {
                        reader.skipValue();
                    }
                }
                reader.endArray();
            } else {
                reader.skipValue();
            }
        }

        reader.endObject();

        if (di.pluginID.isEmpty())
            throw new ClassNotFoundException();
        return di;
    }

    /**
     * Return the json representation of this scene
     *
     * @return JSON String
     */
    public String toJSON() {
        try {
            JSONHelper h = new JSONHelper();
            toJSON(h.createWriter());
            return h.getString();
        } catch (IOException ignored) {
            return null;
        }
    }

    public void toJSON(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("uuid").value(uuid.toString());
        writer.name("DeviceName").value(DeviceName);
        writer.name("Type").value(pluginID);
        writer.name("HostName").value(HostName);
        writer.name("UniqueDeviceID").value(UniqueDeviceID);
        writer.name("UserName").value(UserName);
        writer.name("Password").value(Password);
        writer.name("Temperature").value(Temperature);
        writer.name("FirmwareVersion").value(FirmwareVersion);
        writer.name("DefaultPorts").value(DefaultPorts);
        writer.name("SendPort").value(SendPort);
        writer.name("ReceivePort").value(ReceivePort);
        writer.name("HttpPort").value(HttpPort);

        writer.name("DevicePorts").beginArray();
        assert DevicePorts != null;
        for (DevicePort oi : DevicePorts) {
            oi.toJSON(writer);
        }
        writer.endArray();

        writer.endObject();
    }

    /**
     * Return true if the updated timestamp is after the given timestamp
     *
     * @param current_time A timestamp (milliseconds since Jan. 1, 1970)
     * @return Return true if the updated timestamp is after the given timestamp
     */
    public boolean updatedAfter(long current_time) {
        return updated > current_time;
    }

    public long getUpdatedTime() {
        return updated;
    }

    public int getHash() {
        int h = DeviceName.hashCode() + 512 * ReceivePort + 1024 * SendPort + UniqueDeviceID.hashCode();
        if (reachable) h += 1000;

        for (DevicePort port : DevicePorts) {
            h += port.current_value + port.getDescription().hashCode();
        }
        return h;
    }
}
