package oly.netpowerctrl.device_base.device;

import android.support.annotation.NonNull;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import oly.netpowerctrl.device_base.data.JSONHelper;
import oly.netpowerctrl.device_base.data.StorableInterface;


// An object of this class contains all the info about a specific device
public class Device implements Comparable<Device>, StorableInterface {
    // Connections to the destination device. This is prioritized, the first reachable connection
    // is preferred before the second reachable etc.
    public final List<DeviceConnection> DeviceConnections = new ArrayList<>();
    // Device Ports
    private final Map<Integer, DevicePort> DevicePorts = new TreeMap<>();
    private final Semaphore lock = new Semaphore(1);
    // Identity of the device
    public String pluginID;
    // User visible data of this device
    public String DeviceName = ""; // Name of the device as reported by the device
    public String Version = ""; // Version of the device firmware
    // Access to the device
    public String UserName = "";
    public String Password = "";
    // Additional features
    public List<DeviceFeatureInterface> Features = new ArrayList<>();
    // Temporary state variables
    public boolean configured = false;
    DeviceConnection cached_deviceConnection;
    //   a device unique id e.g. the mac address associated with a device
    private String UniqueDeviceID = UUID.randomUUID().toString();
    private boolean enabled = true;
    private long updated = 0;
    private boolean hasChanged = false;
    private Object pluginInterface = null;

    // Invalid Device
    @SuppressWarnings("unused")
    public Device() {
    }

    public Device(String pluginID) {
        this.pluginID = pluginID;
    }

    /**
     * Getting the state of has_changed will automatically
     * reset the flag!
     *
     * @return
     */
    public boolean isHasChanged() {
        boolean hasChangedL = hasChanged;
        hasChanged = false; // Reset flag
        return hasChangedL;
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

    public String getUniqueDeviceID() {
        return UniqueDeviceID;
    }

    /**
     * Set the unique device id. This may be a mac address or internal device id.
     * You may not set this to null!
     *
     * @param uniqueDeviceID The unique id
     */
    public void setUniqueDeviceID(String uniqueDeviceID) {
        if (uniqueDeviceID == null)
            throw new RuntimeException("Device::setUniqueDeviceID should not be null. Use makeTemporaryDevice instead.");
        UniqueDeviceID = uniqueDeviceID;
    }

    public void makeTemporaryDevice() {
        UniqueDeviceID = null;
    }

    public void compute_first_reachable() {
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

    /**
     * Set all device connections to not reachable, which match the given protocol.
     *
     * @param protocol             A protocol like UDP or HTTP
     * @param not_reachable_reason The not reachable reason. Set to null to make the connections
     * @param clearReachability    Clear the used counter and make isReachable() return false therefore.
     */
    public void setStatusMessage(String protocol, String not_reachable_reason, boolean clearReachability) {
        for (DeviceConnection di : DeviceConnections)
            if (di.getProtocol().equals(protocol)) {
                di.setStatusMessage(not_reachable_reason, clearReachability);
                break;
            }

        if (clearReachability)
            compute_first_reachable();
    }

    public void setStatusMessage(DeviceConnection deviceConnection, String not_reachable_reason, boolean clearReachability) {
        deviceConnection.setStatusMessage(not_reachable_reason, clearReachability);
        if (clearReachability)
            compute_first_reachable();
    }

    public void setStatusMessageAllConnections(String not_reachable_reason) {
        for (DeviceConnection di : DeviceConnections)
            di.setStatusMessage(not_reachable_reason, true);
        cached_deviceConnection = null;
        setHasChanged();
    }

    @Override
    public int compareTo(@NonNull Device device) {
        if (device.UniqueDeviceID.equals(UniqueDeviceID))
            return 0;
        return 1;
    }

    /**
     * Every device belongs to a plugin. This method returns the corresponding plugin.
     *
     * @return The plugin this device belongs to. This is of type PluginInterface, cast it
     * to your desired plugin before use.
     */
    public Object getPluginInterface() {
        return pluginInterface;
    }

    public void setPluginInterface(Object pluginInterface) {
        this.pluginInterface = pluginInterface;
    }

    /**
     * Copy values from another device.
     *
     * @param other Another DeviceInfo object from where to copy data from.
     * @return Return true if this object copied newer values of "other" or if setHasChanged
     * has been called before.
     */
    public void copyValuesFromUpdated(@NonNull Device other) {
        if (other.pluginInterface == null && pluginInterface == null)
            throw new RuntimeException("Device::copyValuesFromUpdated: pluginInterface not set!");

        if (pluginInterface == null) {
            // Update plugin object reference
            pluginInterface = other.pluginInterface;
        }

        updated = other.updated;
        // Use "or" even for the first assignment, to allow setHasChanged to be called on the device before.
        hasChanged |= copyFreshDevicePorts(other.DevicePorts);
        hasChanged |= copyFeatures(other);
        hasChanged |= !Version.equals(other.Version);
        Version = other.Version;
    }

    /**
     * This will update reachable information of all device connections, which
     * are also used in the updated device.
     *
     * @param other_deviceConnections Updated device connections
     */
    public void replaceAutomaticAssignedConnections(@NonNull List<DeviceConnection> other_deviceConnections) {
        if (other_deviceConnections == DeviceConnections)
            return;

        // Remove non used assigned-by-device connections
        Iterator<DeviceConnection> it = DeviceConnections.iterator();
        while (it.hasNext()) {
            if (it.next().isAssignedByDevice())
                it.remove();
        }

        // add connections
        for (DeviceConnection deviceConnection : other_deviceConnections) {
            DeviceConnections.add(deviceConnection);
        }

        compute_first_reachable();

        if (other_deviceConnections.size() > 0)
            setHasChanged();
    }

    private boolean copyFeatures(@NonNull Device other) {
        if (other.Features.size() > Features.size())
            this.Features = other.Features;
        return false;
    }

    public boolean copyFreshDevicePorts(@NonNull Map<Integer, DevicePort> other_devicePorts) {
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
     * @return Return true if unique id is equal.
     */
    @SuppressWarnings("unused")
    public boolean equalsByUniqueID(@NonNull Device other) {
        return UniqueDeviceID != null && UniqueDeviceID.equals(other.UniqueDeviceID);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Device && UniqueDeviceID != null && UniqueDeviceID.equals(((Device) other).UniqueDeviceID);
    }

    /**
     * Return the json representation of this scene
     *
     * @return JSON String
     */
    @Override
    public String toString() {
        try {
            JSONHelper h = new JSONHelper();
            toJSON(h.createWriter());
            return h.getString();
        } catch (IOException ignored) {
            return null;
        }
    }

    private void toJSON(@NonNull JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("DeviceName").value(DeviceName);
        writer.name("Type").value(pluginID);
        writer.name("UniqueDeviceID").value(UniqueDeviceID);
        writer.name("UserName").value(UserName);
        writer.name("Password").value(Password);
        writer.name("Version").value(Version);
        writer.name("Enabled").value(enabled);

        writer.name("Features").beginArray();
        for (DeviceFeatureInterface deviceFeature : Features) {
            deviceFeature.toJSON(writer);
        }
        writer.endArray();

        writer.name("Connections").beginArray();
        for (DeviceConnection deviceConnection : DeviceConnections) {
            deviceConnection.toJSON(writer);
        }
        writer.endArray();

        lockDevicePorts();
        writer.name("DevicePorts").beginArray();
        for (Map.Entry<Integer, DevicePort> entry : DevicePorts.entrySet()) {
            entry.getValue().toJSON(writer, false);
        }
        writer.endArray();
        releaseDevicePorts();

        writer.endObject();

        writer.close();
    }

    public long getUpdatedTime() {
        return updated;
    }

    public void setUpdatedNow() {
        updated = System.currentTimeMillis();
    }

    /**
     * Add or update a DevicePort of this device.
     *
     * @param devicePort The new or updated device port.
     * @return Return true if this is an update otherwise false.
     */
    public boolean putPort(@NonNull DevicePort devicePort) {
        return DevicePorts.put(devicePort.id, devicePort) != null;
    }

    public DevicePort getFirst() {
        Iterator<Map.Entry<Integer, DevicePort>> it = DevicePorts.entrySet().iterator();
        if (!it.hasNext())
            return null;
        return it.next().getValue();
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

    /**
     * Remove DevicePort by ID
     *
     * @param id The index/id of the device port.
     */
    public void remove(int id) {
        DevicePorts.remove(id);
    }

    public String getFeatureString() {
        String f = "";
        for (DeviceFeatureInterface feature : Features)
            f += feature.getString() + " ";
        return f;
    }

    public boolean isReachable() {
        if (!enabled || pluginInterface == null)
            return false;
        for (DeviceConnection connection : DeviceConnections) {
            if (connection.isReachable())
                return true;
        }
        return false;
    }

    public void setReachable(int index) {
        DeviceConnections.get(index).connectionUsed();
        compute_first_reachable();
    }

    public void removeConnection(@NonNull DeviceConnection removeConnection) {
        for (int i = 0; i < DeviceConnections.size(); ++i) {
            final DeviceConnection connection = DeviceConnections.get(i);
            if (connection.equals(removeConnection)) {
                DeviceConnections.remove(i);
                return;
            }
        }
        compute_first_reachable();
    }

    public void addConnection(@NonNull DeviceConnection newConnection) {
        for (DeviceConnection connection : DeviceConnections)
            if (connection.equals(newConnection))
                return;

        if (newConnection instanceof DeviceConnectionUDP) {
            DeviceConnections.add(0, newConnection);
            setReachable(0);
        } else {
            DeviceConnections.add(newConnection);
            setReachable(DeviceConnections.size() - 1);
        }
    }

    public InetAddress[] getHostnameIPs(boolean lookupDNSName) {
        List<InetAddress> addresses = new ArrayList<>();
        for (DeviceConnection connection : DeviceConnections) {
            Collections.addAll(addresses, connection.getHostnameIPs(lookupDNSName));
        }

        InetAddress[] a = new InetAddress[addresses.size()];
        addresses.toArray(a);
        return a;
    }

    // This has to be executed in another thread not the gui thread if lookupDNSName is set.
    public boolean hasAddress(InetAddress[] hostnameIPs, boolean lookupDNSName) {
        if (hostnameIPs == null || hostnameIPs.length == 0)
            return false;

        for (DeviceConnection connection : DeviceConnections) {
            if (connection.hasAddress(hostnameIPs, lookupDNSName))
                return true;
        }

        return false;
    }

    @Override
    public StorableDataType getDataType() {
        return StorableDataType.JSON;
    }

    @Override
    public String getStorableName() {
        return UniqueDeviceID;
    }

    @Override
    public void load(@NonNull JsonReader reader) throws IOException, ClassNotFoundException {
        reader.beginObject();
        configured = true;
        while (reader.hasNext()) {
            String name = reader.nextName();
            assert name != null;
            switch (name) {
                case "DeviceName":
                    DeviceName = reader.nextString();
                    break;
                case "UniqueDeviceID":
                    UniqueDeviceID = reader.nextString();
                    break;
                case "UserName":
                    UserName = reader.nextString();
                    break;
                case "Password":
                    Password = reader.nextString();
                    break;
                case "Version":
                    Version = reader.nextString();
                    break;
                case "Enabled":
                    enabled = reader.nextBoolean();
                    break;
                case "Type":
                    pluginID = reader.nextString();
                    break;
                case "Features":
                    Features.clear();
                    reader.beginArray();
                    while (reader.hasNext()) {
                        DeviceFeatureInterface feature = DeviceFeatureFabric.fromJSON(reader);
                        if (feature != null)
                            Features.add(feature);
                    }
                    reader.endArray();
                    break;
                case "Connections":
                    DeviceConnections.clear();
                    reader.beginArray();
                    while (reader.hasNext()) {
                        DeviceConnection connection = DeviceConnectionFabric.fromJSON(reader, this);
                        if (connection != null) {
                            DeviceConnections.add(connection);
                        }
                    }
                    reader.endArray();
                    break;
                case "DevicePorts":
                    DevicePorts.clear();
                    reader.beginArray();
                    while (reader.hasNext()) {
                        try {
                            putPort(DevicePort.fromJSON(reader, this));
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
        compute_first_reachable();

        if (pluginID.isEmpty())
            throw new ClassNotFoundException();
    }

    @Override
    public void load(@NonNull InputStream input) throws IOException, ClassNotFoundException {
        load(new JsonReader(new InputStreamReader(input)));
    }

    @Override
    public void save(@NonNull OutputStream output) throws IOException {
        toJSON(JSONHelper.createWriter(output));
    }
}
