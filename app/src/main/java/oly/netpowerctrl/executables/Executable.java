package oly.netpowerctrl.executables;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Set;
import java.util.TreeSet;

import oly.netpowerctrl.credentials.Credentials;
import oly.netpowerctrl.data.AbstractBasePlugin;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.network.ReachabilityStates;
import oly.netpowerctrl.network.onExecutionFinished;
import oly.netpowerctrl.utils.IOInterface;
import oly.netpowerctrl.utils.JSONHelper;

;

/**
 * An executable represents an outlet, IO-Port, Sensor or whatever. It has a state (@see Executable.current_value),
 * a range for its valid values (min_value, max_value) and always belongs to a Plugin and a device.
 */
public class Executable implements Comparable, IOInterface {
    // Some value constants

    // Values
    public int min_value = 0;
    public int max_value = 0;
    public int current_value = 0;
    // The device this port belongs to.
    @Nullable
    public String deviceUID;
    // Type of this executable
    public ExecutableType ui_type = ExecutableType.TypeUnknown;
    public String title = "";
    // Executable groups
    protected Set<String> group_uids = new TreeSet<>();
    // unique identity among all device ports: Generated by device_unique_id + id
    protected String uid;
    // Executables may be hidden by the user. They are not shown in the user interface but can still be activated by scenes etc.
    protected boolean hidden = false;

    ////// Cached values
    protected int last_hash_code = 0;
    // Used to disable control in list until ack from device has been received.
    private boolean isSaveable = true;
    private Credentials credentials;
    private boolean cached_executionInProgress;
    private ReachabilityStates cached_reachabilityStates = ReachabilityStates.NotReachable;
    private boolean cached_ReachabilityHasChanged = false;

    public Executable() {
    }

    public boolean isSaveable() {
        return this.isSaveable;
    }

    public void setIsSaveable(boolean isSaveable) {
        this.isSaveable = isSaveable;
    }


    /**
     * Notice: Only call this method if the NetpowerctrlService service is running!
     *
     * @param dataService DataService
     * @param command     The command to executeToggle
     * @param callback    The callback for the execution-done messages
     */
    public void execute(@NonNull final DataService dataService, final int command, @Nullable final onExecutionFinished callback) {
        if (credentials != null) {
            credentials.getPlugin().execute(this, command, callback);
        } else {
            if (callback != null) {
                callback.addFail();
            }
        }
    }

    public final Set<String> getGroupUIDs() {
        return group_uids;
    }

    public final String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public final ExecutableType getType() {
        return ui_type;
    }

    public String getTitle() {
        return this.title;
    }

    public final int getCurrentValue() {
        return current_value;
    }

    public final int getMaximumValue() {
        return max_value;
    }

    public final int getMinimumValue() {
        return min_value;
    }

    public int getCurrentValueToggled() {
        int c = (current_value + 1) % (max_value + 1);
        if (c < min_value) c = min_value;
        return c;
    }

    /**
     * Set a new title for this executable. This will also propagate the new title
     * to the plugin which belongs to this executable. If the target device is capable
     * of changing a port name the given title is send to the target device. The result
     * will be given via the callback object.
     *
     * @param title    The new title. Beware of non ascii characters.
     * @param callback A callback interface or null.
     */
    public void setTitle(String title, @NonNull onNameChangeResult callback) {
        if (title.equals(this.title)) return;

        if (credentials != null && credentials.getPlugin().supportProperty(AbstractBasePlugin.Properties.RemoteRename)) {
            callback.onNameChangeStart(this);
            credentials.getPlugin().setTitle(this, title, callback);
        } else
            this.title = title;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }

    public final void load(JsonReader reader, boolean noBeginObject) throws IOException, ClassNotFoundException {
        if (!noBeginObject) reader.beginObject();
        cached_executionInProgress = false;

        while (reader.hasNext()) {
            String name = reader.nextName();

            assert name != null;

            switch (name) {
                case "Type":
                    int t = reader.nextInt();
                    if (t > ExecutableType.values().length)
                        throw new ClassNotFoundException();
                    ui_type = ExecutableType.values()[t];
                    break;
                case "UID":
                    uid = reader.nextString();
                    break;
                case "DeviceUID": // Optional, only if needCredentials()==true
                    deviceUID = reader.nextString();
                    break;
                case "Title":
                    title = reader.nextString();
                    break;
                case "Value":
                    current_value = reader.nextInt();
                    break;
                case "max_value":
                    max_value = reader.nextInt();
                    break;
                case "min_value":
                    min_value = reader.nextInt();
                    break;
                case "Hidden":
                    hidden = reader.nextBoolean();
                    break;
                case "Groups":
                    group_uids.clear();
                    reader.beginArray();
                    while (reader.hasNext()) {
                        group_uids.add(reader.nextString());
                    }
                    reader.endArray();
                    break;
                default:
                    loadValue(name, reader);
                    break;
            }
        }

        reader.endObject();

        if (title.isEmpty())
            throw new ClassNotFoundException();
    }

    protected void loadValue(String name, JsonReader reader) throws IOException {
        reader.skipValue();
    }

    @Override
    public void load(@NonNull InputStream input) throws IOException, ClassNotFoundException {
        load(new JsonReader(new InputStreamReader(input)), false);
    }

    @Override
    public void save(@NonNull OutputStream output) throws IOException {
        toJSON(JSONHelper.createWriter(output));
    }

    public int computeChangedCode() {
        return (ui_type.hashCode() + uid.hashCode() + current_value + max_value + min_value + (hidden ? 1 : 0) + group_uids.hashCode());
    }

    /**
     * An executable may not always be safeable, for example if it is a homescreen icon for execution.
     *
     * @return
     */
    @Override
    public boolean hasChanged() {
        return isSaveable && last_hash_code != computeChangedCode();
    }

    @Override
    public void setHasChanged() {
        last_hash_code = 0;
    }

    @Override
    public void resetChanged() {
        last_hash_code = computeChangedCode();
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

    public boolean executionInProgress() {
        return cached_executionInProgress;
    }

    public void setExecutionInProgress(boolean cached_executionInProgress) {
        this.cached_executionInProgress = cached_executionInProgress;
    }

    public int compareTo(@NonNull Object o) {
        Executable other = (Executable) o;
        if (other.equals(this))
            return 0;

        return getTitle().compareTo(other.getTitle());
    }

    public boolean equals(Executable other) {
        return (other != null) && uid.equals(other.getUid());
    }

    public final void toJSON(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("ObjectType").value(getClass().getName());
        writer.name("Type").value(ui_type.ordinal());
        writer.name("Title").value(title);
        writer.name("Value").value(current_value);
        writer.name("max_value").value(max_value);
        writer.name("min_value").value(min_value);
        writer.name("Hidden").value(hidden);
        writer.name("UID").value(uid);
        if (needCredentials()) {
            if (deviceUID == null)
                throw new RuntimeException();
            writer.name("DeviceUID").value(deviceUID);
        }
        saveValue(writer);
        writer.name("Groups").beginArray();
        for (String groupUID : group_uids)
            writer.value(groupUID);
        writer.endArray();
        writer.endObject();
        writer.close();
    }

    protected void saveValue(JsonWriter writer) throws IOException {
    }

    public String getDescription() {
        return credentials.getDeviceName();
    }

    public ReachabilityStates reachableState() {
        return cached_reachabilityStates;
    }

    public void addToGroup(String groupUID) {
        group_uids.add(groupUID);
        setHasChanged();
    }

    public boolean needCredentials() {
        return true;
    }

    @Nullable
    public Credentials getCredentials() {
        return credentials;
    }

    /**
     * Set the credentials object of this executable. This is a cached object and can also be
     * acquired by DataService.getService().credentials.getByUID(executable.deviceUID).
     * <p/>
     * Be aware to call this only after the executable uid has been set.
     *
     * @param credentials The credentials object that fits to the deviceUID of this executable.
     */
    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
        this.deviceUID = credentials.getUid();
    }

    /**
     * Update the cached reachability state. This should be called by the executionCollection
     * which is called by the ioConnectionCollection.
     *
     * @param new_state The new reachability state
     */
    public void updateCachedReachability(ReachabilityStates new_state) {
        //Log.w("executable", "reachability " + new_state.name());
        ReachabilityStates a = cached_reachabilityStates;
        cached_reachabilityStates = new_state;
        cached_ReachabilityHasChanged |= new_state != a;
    }

    public void destroy(DataService dataService) {
        credentials = null;
        //uid = null;
    }

    public boolean hasReachabilityChanged() {
        return cached_ReachabilityHasChanged;
    }

    public void resetReachabilityHasChangedFlag() {
        cached_ReachabilityHasChanged = false;
    }
}
