package oly.netpowerctrl.plugin_wol;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.App;
import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AbstractBasePlugin;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.devices.Credentials;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.executables.ExecutableType;
import oly.netpowerctrl.executables.onNameChangeResult;
import oly.netpowerctrl.ioconnection.DeviceIOConnections;
import oly.netpowerctrl.ioconnection.IOConnection;
import oly.netpowerctrl.ioconnection.IOConnectionIPDialog;
import oly.netpowerctrl.ioconnection.adapter.IOConnectionIP;
import oly.netpowerctrl.network.ReachabilityStates;
import oly.netpowerctrl.network.Utils;
import oly.netpowerctrl.network.onExecutionFinished;


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
final public class WOLPlugin extends AbstractBasePlugin {
    public static final String PLUGIN_ID = "org.custom.wol";

    public WOLPlugin(DataService dataService) {
        super(dataService);
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
        DeviceIOConnections deviceIOConnections = dataService.connections.openDevice(executable.deviceUID);
        final IOConnection ioConnection = deviceIOConnections != null ? deviceIOConnections.findReachable() : null;

        if (ioConnection == null || !(ioConnection instanceof IOConnectionIP)) {
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

        final String mac = ((IOConnectionIP) ioConnection).additional;

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    MagicPacket.send(mac, InetAddress.getByName(ioConnection.getDestinationHost()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        if (callback != null) callback.addSuccess();

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
        return true;
    }

    @Override
    public void requestData() {
        List<Credentials> credentialsList = dataService.credentials.findByPlugin(this);
        Credentials c[] = new Credentials[credentialsList.size()];
        credentialsList.toArray(c);
        new RequestData().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (c));
    }

    @Override
    public void requestData(@NonNull IOConnection connection) {
        new RequestData().execute(connection.credentials);
    }

    @Override
    public Credentials createNewDefaultCredentials() {
        Credentials di = new Credentials();
        di.pluginID = getPluginID();
        di.setPlugin(this);
        di.deviceName = App.getAppString(R.string.plugin_wol_new_device_name);
        di.deviceUID = UUID.randomUUID().toString();
        di.userName = "";
        di.password = "";
        return di;
    }

    @Override
    public boolean isNewIOConnectionAllowed(Credentials credentials) {
        return true;
    }

    @Override
    public void addNewIOConnection(@NonNull Credentials credentials, @NonNull Activity activity) {
        IOConnectionIP ioConnectionIP = new IOConnectionIP(credentials);
        ioConnectionIP.connectionUID = UUID.randomUUID().toString();
        IOConnectionIPDialog.show(activity, ioConnectionIP);
    }

    @Override
    public boolean supportProperty(Properties property) {
        switch (property) {
            case RemoteRename:
                return false;
            case EditableUsername:
                return false;
            case EditablePassword:
                return false;
            case ManuallyAddDevice:
                return true;
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
    }

    @Override
    public String getPluginID() {
        return PLUGIN_ID;
    }

    @Override
    public String getLocalizedName() {
        return App.getAppString(R.string.plugin_wol);
    }

    class RequestData extends AsyncTask<Credentials, Void, Void> {
        @Override
        protected Void doInBackground(Credentials... credentialsList) {
            for (Credentials credentials : credentialsList) {
                DeviceIOConnections deviceIOConnections = dataService.connections.openDevice(credentials.getUid());
                if (deviceIOConnections == null) continue;
                Iterator<IOConnection> it = deviceIOConnections.iterator();
                while (it.hasNext()) {
                    IOConnection ioConnection = it.next();
                    boolean reachable = false;
                    try {
                        reachable = Utils.addressIsInCurrentNetwork(InetAddress.getByName(ioConnection.getDestinationHost()));
                    } catch (IOException ignored) {
                    }

                    if (reachable)
                        ioConnection.incReceivedPackets();
                    else
                        ioConnection.setReachability(ReachabilityStates.NotReachable);
                    Executable executable = dataService.executables.findByUID(ioConnection.getUid());
                    if (executable == null) {
                        executable = new Executable();
                        executable.setUid(ioConnection.getUid());
                        executable.setCredentials(credentials, dataService.connections);
                        executable.ui_type = ExecutableType.TypeStateless;
                        executable.title = ioConnection.getDestinationHost();
                        dataService.executables.put(executable);
                    }
                    dataService.connections.put(ioConnection);
                }
            }
            return null;
        }
    }
}
