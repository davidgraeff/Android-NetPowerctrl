package oly.netpowerctrl.devices;

import android.support.annotation.NonNull;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import oly.netpowerctrl.data.AbstractBasePlugin;
import oly.netpowerctrl.utils.IOInterface;
import oly.netpowerctrl.utils.JSONHelper;


// An object of this class contains all information about a specific device like access data, name
public class Credentials implements Comparable<Credentials>, IOInterface {
    // Identity of the device
    public String pluginID;
    public String deviceUID; //   a device unique id e.g. the mac address associated with a device
    // User visible data of this device
    public String deviceName = ""; // Name of the device as reported by the device
    public String version = ""; // version of the device firmware
    public boolean enabled = true;
    // Access to the device
    public String userName = "";
    public String password = "";
    private AbstractBasePlugin mPlugin = null;
    // Temporary state variables
    private boolean mConfigured = false;
    private int mLastHashCode = 0;

    public Credentials() {
    }

    @Override
    public String getUid() {
        return deviceUID;
    }

    private int computeChangedCode() {
        return deviceName.hashCode() + version.hashCode() + userName.hashCode() + password.hashCode() + (mConfigured ? 1 : 0);
    }

    @Override
    public boolean hasChanged() {
        return mLastHashCode != computeChangedCode();
    }

    @Override
    public void resetChanged() {
        mLastHashCode = computeChangedCode();
    }

    public boolean isConfigured() {
        return mConfigured;
    }

    public void setConfigured(boolean configured) {
        if (configured && deviceUID == null)
            throw new RuntimeException("Credential cannot be set to configured without an UID!");
        this.mConfigured = configured;
        resetChanged();
    }

    @Override
    public int compareTo(@NonNull Credentials credentials) {
        if (credentials.deviceUID.equals(deviceUID))
            return 0;
        return 1;
    }

    /**
     * Every device belongs to a plugin. This method returns the corresponding plugin.
     *
     * @return The plugin this device belongs to. This is of type PluginInterface, cast it
     * to your desired plugin before use.
     */
    public AbstractBasePlugin getPlugin() {
        return mPlugin;
    }

    public void setPlugin(AbstractBasePlugin plugin) {
        this.mPlugin = plugin;
    }


    @Override
    public boolean equals(Object other) {
        return other instanceof Credentials && deviceUID.equals(((Credentials) other).deviceUID);
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
        writer.name("DeviceName").value(deviceName);
        writer.name("Type").value(pluginID);
        writer.name("DeviceUID").value(deviceUID);
        writer.name("UserName").value(userName);
        writer.name("Password").value(password);
        writer.name("version").value(version);
        writer.name("Enabled").value(enabled);
        writer.endObject();
        writer.close();
    }

    public void load(@NonNull JsonReader reader) throws IOException, ClassNotFoundException {
        reader.beginObject();
        mConfigured = true;
        while (reader.hasNext()) {
            String name = reader.nextName();
            assert name != null;
            switch (name) {
                case "DeviceName":
                    deviceName = reader.nextString();
                    break;
                case "DeviceUID":
                    deviceUID = reader.nextString();
                    break;
                case "UserName":
                    userName = reader.nextString();
                    break;
                case "Password":
                    password = reader.nextString();
                    break;
                case "version":
                    version = reader.nextString();
                    break;
                case "Enabled":
                    enabled = reader.nextBoolean();
                    break;
                case "Type":
                    pluginID = reader.nextString();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }

        reader.endObject();

        if (pluginID.isEmpty())
            throw new ClassNotFoundException();
        if (deviceUID == null || deviceUID.isEmpty())
            throw new IOException("No deviceUID known!");
    }

    @Override
    public void load(@NonNull InputStream input) throws IOException, ClassNotFoundException {
        load(new JsonReader(new InputStreamReader(input)));
    }

    @Override
    public void save(@NonNull OutputStream output) throws IOException {
        toJSON(JSONHelper.createWriter(output));
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
}
