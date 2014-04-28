package oly.netpowerctrl.datastructure;

import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anel.AnelPlugin;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.application_state.NetpowerctrlService;
import oly.netpowerctrl.application_state.PluginInterface;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.JSONHelper;

// An object of this class contains all the info about a specific device
public class DeviceInfo implements Comparable<DeviceInfo> {
    // Identity of the device
    public String pluginID;
    public String UniqueDeviceID; // a device unique id e.g. the mac address associated with a device

    // User visible name of this device. Version of the device firmware
    public String DeviceName; // name of the device as reported by the device
    public String Version;

    // Access to the device
    public String HostName;   // the hostname / ip address / android service name used to reach the device
    public String UserName;
    public String Password;

    // Specific data, should be removed from here
    public boolean DefaultPorts;
    public int SendPort;
    public int ReceivePort;
    public int HttpPort;
    public String Temperature;

    // Device Ports
    private Map<Integer, DevicePort> DevicePorts = new TreeMap<Integer, DevicePort>();

    // Temporary state variables
    public boolean configured = false;
    private boolean reachable = false;
    public String not_reachable_reason;
    private long updated = 0;
    private int last_hash_value;
    private int last_hash2_value;
    private boolean hasChanged = false;
    private WeakReference<PluginInterface> pluginInterface = null;

//    private class SemaphoreLoud extends Semaphore {
//        public SemaphoreLoud(int permits) {
//            super(permits);
//        }
//
//        @Override
//        public void acquireUninterruptibly() {
//            Log.w("SemaphoreLoud","acquireUninterruptibly");
//            super.acquireUninterruptibly();
//        }
//
//        @Override
//        public void release() {
//            Log.w("SemaphoreLoud", "release");
//            super.release();
//        }
//    }

    private final Semaphore lock = new Semaphore(1);

    private DeviceInfo(String pluginID) {
        this.pluginID = pluginID;
        DeviceName = "";
        HostName = "";
        UniqueDeviceID = UUID.randomUUID().toString();
        UserName = "";
        Password = "";
        DefaultPorts = true;
        SendPort = -1;
        ReceivePort = -1;
        HttpPort = 80;
        Temperature = "";
        Version = "";
    }

//    public DeviceInfo(DeviceInfo other) {
//        uuid = UUID.randomUUID();
//        DeviceName = other.DeviceName;
//        pluginID = other.pluginID;
//        HostName = other.HostName;
//        UniqueDeviceID = other.UniqueDeviceID;
//        UserName = other.UserName;
//        Password = other.Password;
//        DefaultPorts = other.DefaultPorts;
//        SendPort = other.SendPort;
//        ReceivePort = other.ReceivePort;
//        HttpPort = other.HttpPort;
//        Temperature = other.Temperature;
//        Version = other.Version;
//        configured = other.configured;
//        DevicePorts.clear();
//        other.lockDevicePorts(); // lock others devicePort list while iterating over it
//        for (Map.Entry<Integer, DevicePort> entry : other.DevicePorts.entrySet()) {
//            add(new DevicePort(entry.getValue()));
//        }
//        other.releaseDevicePorts();
//    }

    public static DeviceInfo createNewDevice(String pluginID) {
        DeviceInfo di = new DeviceInfo(pluginID);
        di.DeviceName = NetpowerctrlApplication.instance.getString(R.string.default_device_name);
        di.SendPort = SharedPrefs.getDefaultSendPort();
        di.ReceivePort = SharedPrefs.getDefaultReceivePort();
        return di;
    }

    public static DeviceInfo fromJSON(JsonReader reader) throws IOException, ClassNotFoundException {
        reader.beginObject();
        DeviceInfo di = new DeviceInfo("");
        di.configured = true;
        di.reachable = false;
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("uuid")) { // TODO obsolete
                String uuid = reader.nextString();
                if (di.UniqueDeviceID.isEmpty())
                    di.UniqueDeviceID = uuid;
            } else if (name.equals("DeviceName")) {
                di.DeviceName = reader.nextString();
            } else if (name.equals("HostName")) {
                di.HostName = reader.nextString();
            } else if (name.equals("UniqueDeviceID") || name.equals("MacAddress")) { // TODO obsolete
                di.UniqueDeviceID = reader.nextString();
            } else if (name.equals("UserName")) {
                di.UserName = reader.nextString();
            } else if (name.equals("Password")) {
                di.Password = reader.nextString();
            } else if (name.equals("Temperature")) {
                di.Temperature = reader.nextString();
            } else if (name.equals("Version")) {
                di.Version = reader.nextString();
            } else if (name.equals("DefaultPorts")) {
                di.DefaultPorts = reader.nextBoolean();
            } else if (name.equals("SendPort")) {
                di.SendPort = reader.nextInt();
            } else if (name.equals("Type")) {
                // // TODO obsolete
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
                        di.add(DevicePort.fromJSON(reader, di));
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

    public void setHasChanged() {
        hasChanged = true;
    }

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
        if (deviceInfo.UniqueDeviceID.equals(UniqueDeviceID))
            return 0;
        return 1;
    }

    /**
     * Get "execution engine" for this device info, either by an existing reference to it or
     * by requesting it by the plugin controller.
     *
     * @return
     */
    public PluginInterface getPluginInterface(NetpowerctrlService service) {
        PluginInterface pi = pluginInterface != null ? pluginInterface.get() : null;

        if (pi == null) {
            pi = service.getPluginInterface(this);
            pluginInterface = new WeakReference<PluginInterface>(pi);
        }

        return pi;
    }

    /**
     * @param other Another DeviceInfo object from where to copy data from.
     * @return Return true if this object copied newer values of "other".
     */
    public boolean copyFreshValues(DeviceInfo other) {
        lock.acquireUninterruptibly();
        updated = other.updated;

        if (other != this) {
            hasChanged = false;

            // Iterators
            Iterator<Map.Entry<Integer, DevicePort>> other_ports_iterator = other.DevicePorts.entrySet().iterator();
            Set<Integer> current_port_ids = new TreeSet<Integer>(DevicePorts.keySet());

            // Update each current devicePort.
            while (other_ports_iterator.hasNext()) {
                Map.Entry<Integer, DevicePort> entry = other_ports_iterator.next();
                DevicePort current_devicePort = DevicePorts.get(entry.getKey());

                if (current_devicePort == null) { // add missing device port
                    DevicePorts.put(entry.getKey(), new DevicePort(entry.getValue()));
                    hasChanged = true;
                } else { // update port
                    hasChanged |= current_devicePort.copyValues(entry.getValue());
                }
                current_port_ids.remove(entry.getKey());
            }

            // There are ports in this deviceInfo that have no updates. Remove those now.
            if (current_port_ids.size() > 0) {
                hasChanged = true;
                for (Integer i : current_port_ids) {
                    DevicePorts.remove(i);
                }
            }

            hasChanged |= !HostName.equals(other.HostName);
            HostName = other.HostName;
            hasChanged |= HttpPort != other.HttpPort;
            HttpPort = other.HttpPort;
            hasChanged |= !Temperature.equals(other.Temperature);
            Temperature = other.Temperature;
            hasChanged |= !Version.equals(other.Version);
            Version = other.Version;
            hasChanged |= reachable != other.reachable;
            reachable = other.reachable;
            if (not_reachable_reason != null)
                hasChanged |= !not_reachable_reason.equals(other.not_reachable_reason);
            else if (other.not_reachable_reason != null)
                hasChanged |= true;

            not_reachable_reason = other.not_reachable_reason;
        }
        // Else: Same object, but the values may have changed since the last call to "copyFreshValues"

        lock.release();

        boolean hasChangedL = hasChanged;
        hasChanged = false;
        return hasChangedL;
    }

    /**
     * Return true if both DeviceInfo objects refer to the same device, preferable if both
     * have a mac address set.
     *
     * @param other Compare to other DeviceInfo
     * @return
     */
    @SuppressWarnings("unused")
    public boolean equalsByHostname(DeviceInfo other) {
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
    public boolean equalsByUniqueID(DeviceInfo other) {
        return UniqueDeviceID.equals(other.UniqueDeviceID);
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
        writer.name("DeviceName").value(DeviceName);
        writer.name("Type").value(pluginID);
        writer.name("HostName").value(HostName);
        writer.name("UniqueDeviceID").value(UniqueDeviceID);
        writer.name("UserName").value(UserName);
        writer.name("Password").value(Password);
        writer.name("Temperature").value(Temperature);
        writer.name("Version").value(Version);
        writer.name("DefaultPorts").value(DefaultPorts);
        writer.name("SendPort").value(SendPort);
        writer.name("ReceivePort").value(ReceivePort);
        writer.name("HttpPort").value(HttpPort);

        writer.name("DevicePorts").beginArray();
        assert DevicePorts != null;
        for (Map.Entry<Integer, DevicePort> entry : DevicePorts.entrySet()) {
            entry.getValue().toJSON(writer);
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

    public void setUpdatedNow() {
        updated = System.currentTimeMillis();
    }

    public void setUpdatedNever() {
        updated = 0;
    }

    public void add(DevicePort oi) {
        DevicePorts.put(oi.id, oi);
    }

    public void addSafe(DevicePort oi) {
        lock.acquireUninterruptibly();
        DevicePorts.put(oi.id, oi);
        lock.release();
    }

    public DevicePort getFirst() {
        Iterator<Map.Entry<Integer, DevicePort>> it = DevicePorts.entrySet().iterator();
        if (!it.hasNext())
            return null;
        return it.next().getValue();
    }

    public DevicePort getByID(int id) {
        return DevicePorts.get(id);
    }

    public void lockDevicePorts() {
        lock.acquireUninterruptibly();
    }

    public void releaseDevicePorts() {
        lock.release();
    }

    public Iterator<DevicePort> getDevicePortIterator() {
        return DevicePorts.values().iterator();
    }

    public Set<Integer> getDevicePortIDs() {
        return DevicePorts.keySet();
    }

    /**
     * Remove DevicePort by ID
     *
     * @param id
     */
    public void remove(Integer id) {
        DevicePorts.remove(id);
    }

    public boolean isNetworkDevice(NetpowerctrlService service) {
        PluginInterface pi = getPluginInterface(service);
        if (pi == null)
            return false;
        return pi.isNetworkPlugin();
    }
}
