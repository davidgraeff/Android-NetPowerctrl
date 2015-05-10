package oly.netpowerctrl.plugin_simpleudp;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.Locale;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AbstractBasePlugin;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.devices.Credentials;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.executables.ExecutableType;
import oly.netpowerctrl.executables.onNameChangeResult;
import oly.netpowerctrl.ioconnection.DeviceIOConnections;
import oly.netpowerctrl.ioconnection.IOConnection;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.network.UDPErrors;
import oly.netpowerctrl.network.UDPSend;
import oly.netpowerctrl.network.onExecutionFinished;

/**
 * For executing a name on a DevicePort or commands for multiple DevicePorts (bulk).
 * This is a specialized class for Anel devices.
 */
final public class SimpleUDPPlugin extends AbstractBasePlugin {
    public static final String PLUGIN_ID = "org.custom.simpleudp";
    private static final byte[] requestMessage = "DETECT\n".getBytes();

    public SimpleUDPPlugin(DataService dataService) {
        super(dataService);
    }


    /**
     * @param deviceUID The device unique id
     * @param id        From 1..8 (using the UDP numbering)
     * @return Return a unique id for an executable where {@link #extractIDFromExecutableUID(String)} can extract the id back.
     */
    public static String makeExecutableUID(String deviceUID, char id) {
        return deviceUID + "-" + String.valueOf(id);
    }

    public static char extractIDFromExecutableUID(String uid) {
        int i = uid.lastIndexOf('-');
        if (i == -1) throw new RuntimeException("Could not extract device port id from UID");
        return uid.charAt(i + 1);
    }

    public static String extractIDFromExecutableUID_s(String uid) {
        int i = uid.lastIndexOf('-');
        if (i == -1) throw new RuntimeException("Could not extract device port id from UID");
        return uid.substring(i + 1);
    }

    void fillExecutable(Executable executable, Credentials credentials, String uid, int value) {
        executable.ui_type = ExecutableType.TypeToggle;
        executable.deviceUID = credentials.getUid();
        executable.setUid(uid);
        executable.current_value = value;
        executable.setCredentials(credentials, dataService.connections);
    }

    Credentials createDefaultCredentials(String MacAddress) {
        Credentials di = new Credentials();
        di.pluginID = getPluginID();
        di.setPlugin(this);
        di.deviceUID = MacAddress;
        // Default values for user and password
        di.userName = "admin";
        di.password = "admin";
        return di;
    }

    /**
     * Switch a single outlet or io executable
     *
     * @param executable Execute on this executable
     * @param command    Execute this command
     * @param callback   This callback will be called when the execution finished
     */
    @Override
    public boolean execute(@NonNull Executable executable, int command, onExecutionFinished callback) {
        executable.setExecutionInProgress(true);

        boolean bValue = false;
        if (command == Executable.ON)
            bValue = true;
        else if (command == Executable.OFF)
            bValue = false;
        else if (command == Executable.TOGGLE)
            bValue = executable.current_value <= 0;

        DeviceIOConnections deviceIOConnections = dataService.connections.openDevice(executable.deviceUID);
        final IOConnection ioConnection = deviceIOConnections != null ? deviceIOConnections.findReachable() : null;
        final Credentials credentials = dataService.credentials.findByUID(executable.deviceUID);

        if (credentials == null) {
            Log.e(PLUGIN_ID, "execute. No credentials found!");
            if (callback != null) callback.addFail();
            return false;
        }

        if (ioConnection == null) {
            Log.e(PLUGIN_ID, "execute. No reachable DeviceConnection found!");
            if (callback != null) callback.addFail();
            return false;
        }

        char id = extractIDFromExecutableUID(executable.getUid());
        byte[] data = String.format(Locale.US, "%s%c%s%s", bValue ? "IO_on" : "IO_off",
                id, credentials.userName, credentials.password).getBytes();
        new UDPSend(ioConnection, data, requestMessage, UDPErrors.INQUERY_REQUEST);

        return true;
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public void onStart(Context context) {

    }

    @Override
    public boolean isStarted() {
        return false;
    }

    @Override
    public void requestData() {
        UDPSend.createBroadcast(dataService.connections.getAllUDPSendPorts(this), requestMessage, UDPErrors.INQUERY_BROADCAST_REQUEST);
    }

    @Override
    public void requestData(@NonNull IOConnection ioConnection) {
        new UDPSend(ioConnection, requestMessage, UDPErrors.INQUERY_REQUEST);
    }

    @Override
    public Credentials createNewDefaultCredentials() {
        return createDefaultCredentials(UUID.randomUUID().toString());
    }

    /**
     * Renaming is done via http and the dd.htm page on the ANEL devices.
     *
     * @param executable The device executable to rename.
     * @param new_name   The new name
     * @param callback   A callback for the done/failed message.
     */
    @Override
    public void setTitle(@NonNull final Executable executable, @NonNull final String new_name, @Nullable final onNameChangeResult callback) {

    }

    @Override
    public String getPluginID() {
        return PLUGIN_ID;
    }

    @Override
    public String getLocalizedName() {
        return App.getAppString(R.string.plugin_simpleudp);
    }
}
