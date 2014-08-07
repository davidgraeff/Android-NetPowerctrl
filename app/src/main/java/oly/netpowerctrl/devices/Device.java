package oly.netpowerctrl.devices;

import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.application_state.NetpowerctrlService;
import oly.netpowerctrl.application_state.PluginInterface;
import oly.netpowerctrl.device_ports.DevicePort;
import oly.netpowerctrl.utils.JSONHelper;

// An object of this class contains all the info about a specific device
public class Device implements Comparable<Device> {
    // Device Ports
    private final Map<Integer, DevicePort> DevicePorts = new TreeMap<>();
    private final Semaphore lock = new Semaphore(1);
    // Identity of the device
    public String pluginID;
    //   a device unique id e.g. the mac address associated with a device
    public String UniqueDeviceID = UUID.randomUUID().toString();
    // User visible data of this device
    public String DeviceName = ""; // Name of the device as reported by the device
    public String Version = ""; // Version of the device firmware
    // Access to the device
    public String UserName = "";
    public String Password = "";
    // Additional features
    public List<DeviceFeature> Features = new ArrayList<>();

    // Connections to the destination device. This is prioritized, the first reachable connection
    // is preferred before the second reachable etc.
    public List<DeviceConnection> DeviceConnections = new ArrayList<>();
    // Temporary state variables
    public boolean configured = false;
    DeviceConnection cached_deviceConnection;
    private boolean enabled = true;
    private long updated = 0;
    private boolean hasChanged = false;
    private PluginInterface pluginInterface = null;

    private Device(String pluginID) {
        this.pluginID = pluginID;
    }

    public static Device createNewDevice(String pluginID) {
        Device di = new Device(pluginID);
        di.DeviceName = NetpowerctrlApplication.instance.getString(R.string.default_device_name);
        return di;
    }

    public static Device fromJSON(JsonReader reader) throws IOException, ClassNotFoundException {
        reader.beginObject();
        Device di = new Device("");
        di.configured = true;
        while (reader.hasNext()) {
            String name = reader.nextName();
            assert name != null;
            switch (name) {
                case "DeviceName":
                    di.DeviceName = reader.nextString();
                    break;
                case "UniqueDeviceID":
                    di.UniqueDeviceID = reader.nextString();
                    break;
                case "UserName":
                    di.UserName = reader.nextString();
                    break;
                case "Password":
                    di.Password = reader.nextString();
                    break;
                case "Version":
                    di.Version = reader.nextString();
                    break;
                case "Enabled":
                    di.enabled = reader.nextBoolean();
                    break;
                case "Type":
                    di.pluginID = reader.nextString();
                    break;
                case "Features":
                    di.Features.clear();
                    reader.beginArray();
                    while (reader.hasNext()) {
                        DeviceFeature feature = DeviceFeatureFabric.fromJSON(reader);
                        if (feature != null)
                            di.Features.add(feature);
                    }
                    reader.endArray();
                    break;
                case "Connections":
                    di.DeviceConnections.clear();
                    reader.beginArray();
                    while (reader.hasNext()) {
                        DeviceConnection connection = DeviceConnectionFabric.fromJSON(reader, di);
                        if (connection != null) {
                            di.DeviceConnections.add(connection);
                            di.setReachable(di.DeviceConnections.size() - 1);
                        }
                    }
                    reader.endArray();
                    break;
                case "DevicePorts":
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
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }

        reader.endObject();
        di.compute_first_reachable();

        if (di.pluginID.isEmpty())
            throw new ClassNotFoundException();

        NetpowerctrlService service = NetpowerctrlService.getService();
        if (service != null)
            di.pluginInterface = service.getPluginByID(di.pluginID);
        return di;
    }

    public void setHasChanged() {
        hasChanged = true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        compute_first_reachable();
    }

    private void compute_first_reachable() {
        if (!enabled) {
            cached_deviceConnection = null;
            return;
        }

        for (DeviceConnection deviceConnection : DeviceConnections)
            if (deviceConnection.isReachable()) {
                cached_deviceConnection = deviceConnection;
                return;
            }
    }

    public DeviceConnection getFirstReachableConnection() {
        return cached_deviceConnection;
    }

    public DeviceConnection getFirstReachableConnection(String protocol) {
        for (DeviceConnection deviceConnection : DeviceConnections)
            if (deviceConnection.isReachable() && deviceConnection.getProtocol().equals(protocol)) {
                return deviceConnection;
            }
        return null;
    }

    public void setReachable(int index) {
        DeviceConnections.get(index).setReachable();
        compute_first_reachable();
    }

    /**
     * Set all device connections to not reachable, which match the given protocol.
     *
     * @param protocol             A protocol like UDP or HTTP
     * @param not_reachable_reason The not reachable reason. Set to null to make the connections
     *                             reachable instead.
     */
    public void setNotReachable(String protocol, String not_reachable_reason) {
        for (oly.netpowerctrl.devices.DeviceConnection di : DeviceConnections)
            if (di.getProtocol().equals(protocol)) {
                di.setNotReachable(not_reachable_reason);
                break;
            }

        compute_first_reachable();
    }

    public void setNotReachable(int index, String not_reachable_reason) {
        DeviceConnections.get(index).setNotReachable(not_reachable_reason);
        compute_first_reachable();
    }

    public void setNotReachableAll(String not_reachable_reason) {
        for (oly.netpowerctrl.devices.DeviceConnection di : DeviceConnections)
            di.setNotReachable(not_reachable_reason);
        cached_deviceConnection = null;
        hasChanged = true;
    }

    @Override
    public int compareTo(Device device) {
        if (device.UniqueDeviceID.equals(UniqueDeviceID))
            return 0;
        return 1;
    }

    /**
     * Every device belongs to a plugin. This method returns the corresponding plugin.
     *
     * @return The plugin this device belongs to.
     */
    public PluginInterface getPluginInterface() {
        return pluginInterface;
    }

    public void setPluginInterface(PluginInterface pluginInterface) {
        this.pluginInterface = pluginInterface;
    }

    /**
     * Copy values from another device.
     *
     * @param other Another DeviceInfo object from where to copy data from.
     * @return Return true if this object copied newer values of "other" or if setHasChanged
     * has been called before.
     */
    public boolean copyValuesFromUpdated(Device other) {
        if (other != this) {
            updated = other.updated;
            // Use or for the first assignment, to allow setHasChanged to be called on the device
            hasChanged |= copyFreshDevicePorts(other.DevicePorts);
            hasChanged |= copyFeatures(other);
            hasChanged |= !Version.equals(other.Version);
            Version = other.Version;
            hasChanged |= copyConnections(other);
        }
        // Else: Same object, but the values may have changed since the last call to "copyValuesFromUpdated"
        // This is indicated by the boolean value hasChanged which may be set now.

        boolean hasChangedL = hasChanged;
        hasChanged = false; // Reset flag
        return hasChangedL;
    }

    /**
     * This will update reachable information of all device connections, which
     * are also used in the updated device.
     *
     * @param updated Updated device
     * @return
     */
    private boolean copyConnections(Device updated) {
        // If no plugin object reference is known, we abort here. DeviceConnections
        // are not of any use if we have no known plugin to execute actions on.
        if (pluginInterface == null) {
            if (updated.pluginInterface == null) {
                setNotReachableAll(NetpowerctrlApplication.instance.getString(R.string.error_plugin_not_installed));
                return true;
            }
            // Update plugin object reference
            pluginInterface = updated.pluginInterface;
        }

        boolean changed = false;
        // update each of the existing connections
        for (DeviceConnection di : DeviceConnections) {
            for (DeviceConnection di_other : updated.DeviceConnections) {
                // we identify connections by their listening port.
                // Hostnames would a better matching metric but may differ,
                // because the updated device is probably
                if (di.getListenPort() == di_other.getListenPort()) {
                    if (di.isReachable() == di_other.isReachable())
                        break;
                    di.setNotReachable(di_other.getNotReachableReason());
                    changed = true;
                }
            }
        }
        compute_first_reachable();

        return changed;
    }

    private boolean copyFeatures(Device other) {
        //TODO copyFeatures
        if (other.Features.size() > Features.size())
            this.Features = other.Features;
        return false;
    }

    public boolean copyFreshDevicePorts(Map<Integer, DevicePort> other_devicePorts) {
        lock.acquireUninterruptibly();
        boolean hasChanged = false;
        // Iterators
        Iterator<Map.Entry<Integer, DevicePort>> other_ports_iterator = other_devicePorts.entrySet().iterator();
        Set<Integer> current_port_ids = new TreeSet<>(DevicePorts.keySet());

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

        lock.release();
        return hasChanged;
    }

    /**
     * Return true if this and the other DeviceInfo are the same configured DeviceInfo.
     *
     * @param other Compare to other DeviceInfo
     * @return
     */
    @SuppressWarnings("unused")
    public boolean equalsByUniqueID(Device other) {
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
        writer.name("UniqueDeviceID").value(UniqueDeviceID);
        writer.name("UserName").value(UserName);
        writer.name("Password").value(Password);
        writer.name("Version").value(Version);
        writer.name("Enabled").value(enabled);

        writer.name("Features").beginArray();
        for (DeviceFeature deviceFeature : Features) {
            deviceFeature.toJSON(writer);
        }
        writer.endArray();

        writer.name("Connections").beginArray();
        for (DeviceConnection deviceConnection : DeviceConnections) {
            deviceConnection.toJSON(writer);
        }
        writer.endArray();

        writer.name("DevicePorts").beginArray();
        for (Map.Entry<Integer, DevicePort> entry : DevicePorts.entrySet()) {
            entry.getValue().toJSON(writer);
        }
        writer.endArray();

        writer.endObject();
    }

    public long getUpdatedTime() {
        return updated;
    }

    public void setUpdatedNow() {
        updated = System.currentTimeMillis();
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

    public boolean isNetworkDevice() {
        PluginInterface pi = getPluginInterface();
        return pi != null && pi.isNetworkPlugin();
    }

    public int count() {
        return DevicePorts.size();
    }

    public boolean hasFeatures() {
        return Features.size() > 0;
    }

    public String getFeatureString() {
        String f = "";
        for (DeviceFeature feature : Features)
            f += feature.getString() + " ";
        return f;
    }

    public String getNotReachableReasons() {
        if (!enabled)
            return NetpowerctrlApplication.instance.getString(R.string.error_device_disabled);
        String f = "";
        for (DeviceConnection connection : DeviceConnections)
            f += connection.getNotReachableReason() + " ";
        return f;
    }

    public void removeConnection(DeviceConnection removeConnection) {
        for (int i = 0; i < DeviceConnections.size(); ++i) {
            final DeviceConnection connection = DeviceConnections.get(i);
            if (connection.getDestinationHost().equals(removeConnection.getDestinationHost()) &&
                    connection.getListenPort() == removeConnection.getListenPort() &&
                    connection.getProtocol().equals(removeConnection.getProtocol())) {
                DeviceConnections.remove(i);
                return;
            }
        }
        compute_first_reachable();
    }

    public void addConnection(DeviceConnection newConnection) {
        for (DeviceConnection connection : DeviceConnections)
            if (connection.getDestinationHost().equals(newConnection.getDestinationHost()) &&
                    connection.getListenPort() == newConnection.getListenPort() &&
                    connection.getProtocol().equals(newConnection.getProtocol()))
                return;

        if (newConnection instanceof DeviceConnectionUDP) {
            DeviceConnections.add(0, newConnection);
            setReachable(0);
        } else {
            DeviceConnections.add(newConnection);
            setReachable(DeviceConnections.size() - 1);
        }
    }
}