package oly.netpowerctrl.plugin_simpleudp;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import oly.netpowerctrl.App;
import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AbstractBasePlugin;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.devices.Credentials;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.executables.onNameChangeResult;
import oly.netpowerctrl.ioconnection.DeviceIOConnections;
import oly.netpowerctrl.ioconnection.IOConnection;
import oly.netpowerctrl.ioconnection.IOConnectionUDP;
import oly.netpowerctrl.network.UDPSend;
import oly.netpowerctrl.network.onExecutionFinished;
import oly.netpowerctrl.preferences.SharedPrefs;


/**
 * SimpleUDP Packet Format (command type)
 * * All ascii based, two characters not allowed in names and ids: newline (\n) and tabulator (\t).
 * <p/>
 * General structure (lines are separated by newlines)
 * ---------------------------------------------------
 * Header (always SimpleUDP_cmd)
 * UniqueID (we use the android device unique id)
 * CMD_TYPE \t actionID \t optional value
 * <p/>
 * CMD_TYPE is one of (SET,TOGGLE,RENAME), the actionID has been received before and is known.
 * <p/>
 * Example packet:
 * <p/>
 * SimpleUDP_cmd\n
 * NFKAJELAFBAGAHD\n
 * SET\tACTION2\t1
 */
final public class SimpleUDPPlugin extends AbstractBasePlugin {
    public static final String PLUGIN_ID = "org.custom.simpleudp";
    static final int PORT_SEND = 3338;
    static final int PORT_RECEIVE = 3339;
    private static final String OWN_ID = SharedPrefs.getAndroidID();
    private static final byte[] requestMessage = ("SimpleUDP_detect\n" + OWN_ID).getBytes();
    private SimpleUDPReceiveUDP simpleUDPReceiveUDP;

    public SimpleUDPPlugin(DataService dataService) {
        super(dataService);
    }

    /**
     * @param deviceUID The device unique id
     * @param actionID  The action id
     * @return Return a unique id for an executable where {@link #extractIDFromExecutableUID(String)} can extract the id back.
     */
    public static String makeExecutableUID(String deviceUID, String actionID) {
        return deviceUID + "-" + actionID;
    }

    public static String extractIDFromExecutableUID(String uid) {
        int i = uid.lastIndexOf('-');
        if (i == -1) throw new RuntimeException("Could not extract device port id from UID");
        return uid.substring(i + 1);
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

        DeviceIOConnections deviceIOConnections = dataService.connections.openDevice(executable.deviceUID);
        final IOConnection ioConnection = deviceIOConnections != null ? deviceIOConnections.findReachable() : null;

        if (ioConnection == null) {
            Log.e(PLUGIN_ID, "execute. No reachable DeviceConnection found!");
            if (callback != null) callback.addFail();
            return false;
        }

        final Credentials credentials = ioConnection.credentials;

        if (credentials == null) {
            Log.e(PLUGIN_ID, "execute. No credentials found!");
            if (callback != null) callback.addFail();
            return false;
        }

        String type = command == Executable.TOGGLE ? "TOGGLE" : "SET";
        String actionID = extractIDFromExecutableUID(executable.getUid());

        byte[] data = String.format(Locale.US, "SimpleUDP_cmd\n%s\n%s\t%s\t%s", OWN_ID, type, actionID, String.valueOf(executable.current_value)).getBytes();
        UDPSend.sendMessage((IOConnectionUDP) ioConnection, data);

        if (callback != null) callback.addSuccess();

        return true;
    }

    @Override
    public void onDestroy() {
        try {
            simpleUDPReceiveUDP.join(500);
            simpleUDPReceiveUDP.interrupt();
        } catch (InterruptedException ignore) {
        }
    }

    @Override
    public void onStart(Context context) {
        simpleUDPReceiveUDP = new SimpleUDPReceiveUDP(this, PORT_RECEIVE);
        simpleUDPReceiveUDP.start();
    }

    @Override
    public boolean isStarted() {
        return simpleUDPReceiveUDP.isAlive();
    }

    @Override
    public void requestData() {
        Set<Integer> ports = new TreeSet<>();
        ports.add(PORT_SEND);
        UDPSend.sendBroadcast(ports, requestMessage);
    }

    @Override
    public void requestData(@NonNull IOConnection ioConnection) {
        UDPSend.sendMessage((IOConnectionUDP) ioConnection, requestMessage);
    }

    @Override
    public Credentials createNewDefaultCredentials() {
        return createDefaultCredentials(UUID.randomUUID().toString());
    }

    @Override
    public boolean isNewIOConnectionAllowed(Credentials credentials) {
        return false;
    }

    @Override
    public boolean supportProperty(Properties property) {
        switch (property) {
            case RemoteRename:
                return true;
            case EditableUsername:
                return false;
            case EditablePassword:
                return false;
            case ManuallyAddDevice:
                return false;
            default:
                return false;
        }
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
        executable.setExecutionInProgress(true);

        DeviceIOConnections deviceIOConnections = dataService.connections.openDevice(executable.deviceUID);
        final IOConnection ioConnection = deviceIOConnections != null ? deviceIOConnections.findReachable() : null;

        if (ioConnection == null) {
            Log.e(PLUGIN_ID, "execute. No reachable DeviceConnection found!");
            if (callback != null) callback.onNameChangeResult(false, "No Connection");
            return;
        }

        final Credentials credentials = ioConnection.credentials;

        if (credentials == null) {
            Log.e(PLUGIN_ID, "execute. No credentials found!");
            if (callback != null) callback.onNameChangeResult(false, "No credentials");
            return;
        }

        String type = "RENAME";
        String actionID = extractIDFromExecutableUID(executable.getUid());

        byte[] data = String.format(Locale.US, "SimpleUDP_cmd\n%s\n%s\t%s\t%s", OWN_ID, type, actionID, executable.getTitle()).getBytes();
        UDPSend.sendMessage((IOConnectionUDP) ioConnection, data);

        if (callback != null) callback.onNameChangeResult(true, null);
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
