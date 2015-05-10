package oly.netpowerctrl.plugin_anel;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AbstractBasePlugin;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.devices.Credentials;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.executables.ExecutableAndCommand;
import oly.netpowerctrl.executables.ExecutableType;
import oly.netpowerctrl.executables.onNameChangeResult;
import oly.netpowerctrl.ioconnection.DeviceIOConnections;
import oly.netpowerctrl.ioconnection.IOConnection;
import oly.netpowerctrl.ioconnection.IOConnectionHTTP;
import oly.netpowerctrl.ioconnection.IOConnectionUDP;
import oly.netpowerctrl.ioconnection.onNewIOConnection;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.network.HttpThreadPool;
import oly.netpowerctrl.network.UDPErrors;
import oly.netpowerctrl.network.UDPSend;
import oly.netpowerctrl.network.onExecutionFinished;
import oly.netpowerctrl.network.onHttpRequestResult;
import oly.netpowerctrl.preferences.SharedPrefs;

/**
 * For executing a name on a DevicePort or commands for multiple DevicePorts (bulk).
 * This is a specialized class for Anel devices.
 */
final public class AnelPlugin extends AbstractBasePlugin {
    public static final String PLUGIN_ID = "org.anel.outlets_and_io";
    private static final byte[] requestMessage = "wer da?\r\n".getBytes();

    private final List<AnelReceiveUDP> discoveryThreads = new ArrayList<>();
    //private AnelAlarm anelAlarm = new AnelAlarm();

    public AnelPlugin(DataService dataService) {
        super(dataService);
    }

    private static byte switchOn(byte data, int outletNumber) {
        data |= ((byte) (1 << outletNumber - 1));
        return data;
    }

    private static byte switchOff(byte data, int outletNumber) {
        data &= ~((byte) (1 << outletNumber - 1));
        return data;
    }

    private static boolean getIsOn(byte data, int outletNumber) {
        return ((data & ((byte) (1 << (outletNumber - 1)))) != 0);
    }

    private static byte toggle(byte data, int outletNumber) {
        if (getIsOn(data, outletNumber)) {
            return switchOff(data, outletNumber);
        } else {
            return switchOn(data, outletNumber);
        }
    }

    /**
     * @param deviceUID The device unique id
     * @param id        From 1..8 (using the UDP numbering)
     * @return Return a unique id for an executable where {@link #extractIDFromExecutableUID(String)} can extract the id back.
     */
    public static String makeExecutableUID(String deviceUID, int id) {
        return deviceUID + "-" + String.valueOf(id);
    }

    public static int extractIDFromExecutableUID(String uid) {
        int i = uid.lastIndexOf('-');
        if (i == -1) throw new RuntimeException("Could not extract device port id from UID");
        return Integer.valueOf(uid.substring(i + 1));
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
        di.password = "anel";
        return di;
    }

    /**
     * Execute multiple port commands for one device (anel supports this as an extra command).
     *
     * @param ioConnection The device connection to use.
     * @param command_list     Command list where each entry is a port and command
     * @return Number of successful commands
     */
    private int executeDeviceBatch(@NonNull IOConnection ioConnection,
                                   @NonNull List<ExecutableAndCommand> command_list) {
        if (ioConnection instanceof IOConnectionHTTP) { // http
            int success = 0;
            // Use Http instead of UDP for sending. For each batch command we will send a single http request
            for (ExecutableAndCommand c : command_list) {
                boolean ok = executeViaHTTP(ioConnection, c.executable, c.command);
                if (ok) ++success;
            }
            return success;
        } else if (!(ioConnection instanceof IOConnectionUDP)) { // unknown protocol
            Log.e(PLUGIN_ID, "executeDeviceBatch: no known DeviceConnection " + ioConnection.getProtocol());
            return 0;
        }

        // build bulk change byte, see: www.anel-elektronik.de/forum_neu/viewtopic.php?f=16&t=207
        // “Sw” + Steckdosen + User + Passwort
        // Steckdosen = Zustand aller Steckdosen binär
        // LSB = Steckdose 1, MSB (Bit 8)= Steckdose 8 (PRO, POWER), Bit 2 = Steckdose 3 (HOME).
        // Soll nur 1 & 5 eingeschaltet werden=00010001 = 17 = 0x11 (HEX)
        byte data_outlet = 0;
        byte data_io = 0;
        boolean containsOutlets = false;
        boolean containsIO = false;

        // First step: Setup data byte (outlet, io) to reflect the current state of the device ports.
        Credentials credentials = ioConnection.credentials;
        for (int id = 1; id <= 8; ++id) {
            Executable oi = dataService.executables.findByUID(makeExecutableUID(credentials.deviceUID, id));
            if (oi == null) {
                // For Anel devices with only 3 outputs we will run into this case
                //Log.e(getPluginID(), "executeDeviceBatch. Did not find " + makeExecutableUID(credentials.deviceUID, id) + " " + credentials.getDeviceName());
                continue;
            }
            if (oi.current_value == 0) // Only take "ON" commands into account for the bulk change byte
                continue;
            if (id >= 10 && id < 20) {
                data_io = switchOn(data_io, id - 10);
            } else if (id >= 0) {
                data_outlet = switchOn(data_outlet, id);
            }
        }

        // Second step: Apply commands
        for (ExecutableAndCommand c : command_list) {
            c.executable.setExecutionInProgress(true);

            int id = extractIDFromExecutableUID(c.executable.getUid());
            if (id >= 10 && id < 20) {
                containsIO = true;
            } else if (id >= 0) {
                containsOutlets = true;
            }
            switch (c.command) {
                case Executable.OFF:
                    if (id >= 10 && id < 20) {
                        data_io = switchOff(data_io, id - 10);
                    } else if (id >= 0) {
                        data_outlet = switchOff(data_outlet, id);
                    }
                    break;
                case Executable.ON:
                    if (id >= 10 && id < 20) {
                        data_io = switchOn(data_io, id - 10);
                    } else if (id >= 0) {
                        data_outlet = switchOn(data_outlet, id);
                    }
                    break;
                case Executable.TOGGLE:
                    if (id >= 10 && id < 20) {
                        data_io = toggle(data_io, id - 10);
                    } else if (id >= 0) {
                        data_outlet = toggle(data_outlet, id);
                    }
                    break;
            }
        }

        // Third step: Send data
        String access = credentials.userName + credentials.password;
        byte[] data = new byte[3 + access.length()];
        System.arraycopy(access.getBytes(), 0, data, 3, access.length());

        if (containsOutlets) {
            data[0] = 'S';
            data[1] = 'w';
            data[2] = data_outlet;
            new UDPSend(ioConnection, data, requestMessage, UDPErrors.INQUERY_REQUEST);
        }
        if (containsIO) {
            data[0] = 'I';
            data[1] = 'O';
            data[2] = data_io;
            new UDPSend(ioConnection, data, requestMessage, UDPErrors.INQUERY_REQUEST);
        }

        return command_list.size();
    }

    private void startNetworkReceivers(boolean changed) {
        // Get all ports of configured devices and add the additional_port if != 0
        Set<Integer> ports = dataService.connections.getAllUDPReceivePorts(this);
        ports.add(SharedPrefs.getInstance().getDefaultReceivePort());

        if (!changed && discoveryThreads.size() == ports.size()) return;

        boolean new_threads_started = false;
        List<AnelReceiveUDP> unusedThreads = new ArrayList<>(discoveryThreads);

        StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
        String methodName = stacktrace[3].getClassName() + ":" + stacktrace[3].getMethodName() + "->" + stacktrace[4].getClassName() + ":" + stacktrace[4].getMethodName();
        Log.w(PLUGIN_ID, "startNetworkReceivers " + methodName);

        // Go through all ports and start a thread for it if none is running for it so far
        for (int port : ports) {
            boolean already_running = false;
            for (AnelReceiveUDP running_thread : discoveryThreads) {
                if (running_thread.getPort() == port) {
                    already_running = true;
                    unusedThreads.remove(running_thread);
                    break;
                }
            }

            if (already_running) {
                continue;
            }

            new_threads_started = true;
            AnelReceiveUDP thr = new AnelReceiveUDP(this, port);
            thr.start();
            discoveryThreads.add(thr);
        }

        if (unusedThreads.size() > 0) {
            for (AnelReceiveUDP thr : unusedThreads) {
                thr.interrupt();
                discoveryThreads.remove(thr);
            }
        }

        if (new_threads_started) {
            // give the threads a chance to start
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }

        HttpThreadPool.startHTTP();
    }

    private void stopNetwork() {
        StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
        String methodName = stacktrace[3].getClassName() + ":" + stacktrace[3].getMethodName() + "->" + stacktrace[4].getClassName() + ":" + stacktrace[4].getMethodName();
        Log.w(PLUGIN_ID, "stopNetwork " + methodName);

        synchronized (this) {
            if (discoveryThreads.size() > 0) {
                for (AnelReceiveUDP thr : discoveryThreads)
                    thr.interrupt();
                discoveryThreads.clear();
            }

            HttpThreadPool.stopHTTP();
        }
    }

    public boolean executeViaHTTP(IOConnection ioConnection, Executable port, int command) {
        if (ioConnection.getDestinationPort() < 0) {
            Toast.makeText(App.instance, R.string.error_device_no_network_connections, Toast.LENGTH_SHORT).show();
            return false;
        }
        // The http interface can only toggle. If the current state is the same as the command state
        // then we request values instead of sending a command.
        if (command == Executable.TOGGLE && port.current_value == command)
            HttpThreadPool.execute(new HttpThreadPool.HTTPRunner<>((IOConnectionHTTP) ioConnection, "strg.cfg",
                    "", ioConnection, false, AnelReceiveSendHTTP.receiveCtrlHtml));
        else {
            // Important: For UDP the id is 1-based. For Http the id is 0 based!
            String f_id = String.valueOf(extractIDFromExecutableUID(port.getUid()) - 1);
            HttpThreadPool.execute(new HttpThreadPool.HTTPRunner<>((IOConnectionHTTP) ioConnection, "ctrl.htm",
                    "F" + f_id + "=s", (IOConnectionHTTP) ioConnection, false, AnelReceiveSendHTTP.receiveSwitchResponseHtml));
        }
        return true;
    }

    /**
     * Switch a single outlet or io executable
     *
     * @param executable     Execute on this executable
     * @param command  Execute this command
     * @param callback This callback will be called when the execution finished
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

        // Use Http instead of UDP for sending. For each command we will send a single http request
        if (ioConnection instanceof IOConnectionHTTP) {
            boolean success = executeViaHTTP(ioConnection, executable, command);
            if (callback != null) {
                if (success) callback.addSuccess();
                else callback.addFail();
            }
            return success;
        } else if (ioConnection instanceof IOConnectionUDP) {
            final Credentials credentials = dataService.credentials.findByUID(executable.deviceUID);
            if (credentials == null) {
                Log.e(PLUGIN_ID, "execute. No credentials found!");
                if (callback != null) callback.addFail();
                return false;
            }
            int id = extractIDFromExecutableUID(executable.getUid());
            byte[] data;
            if (id >= 10 && id < 20) {
                // IOS
                data = String.format(Locale.US, "%s%d%s%s", bValue ? "IO_on" : "IO_off",
                        id - 10, credentials.userName, credentials.password).getBytes();
                new UDPSend(ioConnection, data, requestMessage, UDPErrors.INQUERY_REQUEST);
                if (callback != null) callback.addSuccess();
                return true;
            } else if (id >= 0) {
                // Outlets
                data = String.format(Locale.US, "%s%d%s%s", bValue ? "Sw_on" : "Sw_off",
                        id, credentials.userName, credentials.password).getBytes();
                new UDPSend(ioConnection, data, requestMessage, UDPErrors.INQUERY_REQUEST);
                if (callback != null) callback.addSuccess();
                return true;
            } else {
                if (callback != null) callback.addFail();
                return false;
            }
        }

        Log.e(PLUGIN_ID, "execute. No reachable DeviceConnection found!");
        if (callback != null) callback.addFail();
        return false;
    }

    @Override
    public void onDestroy() {
        stopNetwork();
    }

    @Override
    public void onStart(Context context) {
        startNetworkReceivers(true);
    }

    @Override
    public boolean isStarted() {
        return discoveryThreads.size() > 0;
    }

    @Override
    public void requestData() {
        Set<Integer> ports = dataService.connections.getAllUDPSendPorts(this);
        ports.add(SharedPrefs.getInstance().getDefaultSendPort());
        UDPSend.createBroadcast(ports, requestMessage, UDPErrors.INQUERY_BROADCAST_REQUEST);
    }

    @Override
    public void requestData(@NonNull IOConnection ioConnection) {
        Log.w("Anel", "Query " + ioConnection.credentials.getDeviceName() + " " + ioConnection.getProtocol());

        if (ioConnection instanceof IOConnectionHTTP) {
            HttpThreadPool.execute(new HttpThreadPool.HTTPRunner<>((IOConnectionHTTP) ioConnection,
                    "strg.cfg", "", ioConnection, false, AnelReceiveSendHTTP.receiveCtrlHtml));
        } else {
            new UDPSend(ioConnection, requestMessage, UDPErrors.INQUERY_REQUEST);
        }
    }

    @Override
    public Credentials createNewDefaultCredentials() {
        return createDefaultCredentials(UUID.randomUUID().toString());
    }

    /**
     * Renaming is done via http and the dd.htm page on the ANEL devices.
     *  @param executable     The device executable to rename.
     * @param new_name The new name
     * @param callback A callback for the done/failed message.
     */
    @Override
    public void setTitle(@NonNull final Executable executable, @NonNull final String new_name, @Nullable final onNameChangeResult callback) {
        // First call the dd.htm page to get all current values (we only want to change one of those
        // and have to set all the others to the same values as before)
        final String getData = "dd.htm?DD" + extractIDFromExecutableUID_s(executable.getUid());
        DeviceIOConnections deviceIOConnections = dataService.connections.openDevice(executable.deviceUID);
        final IOConnectionHTTP ci = deviceIOConnections != null ? (IOConnectionHTTP) deviceIOConnections.findReachable("HTTP") : null;
        if (ci == null) {
            Toast.makeText(App.instance, R.string.error_rename_only_with_http_connection, Toast.LENGTH_SHORT).show();
            return;
        }

        final onHttpRequestResult httpRequestResult = new onHttpRequestResult() {
            @Override
            public void httpRequestResult(Executable oi, boolean success, String error_message) {
                if (callback != null) callback.onNameChangeResult(success, error_message);
            }

            @Override
            public void httpRequestStart(@SuppressWarnings("UnusedParameters") Executable oi) {

            }
        };

        HttpThreadPool.execute(new HttpThreadPool.HTTPRunner<>(ci, getData, null,
                executable, true, new HttpThreadPool.HTTPCallback<Executable>() {
            @Override
            public void httpResponse(Executable port, boolean callback_success, String response_message) {
                if (!callback_success) {
                    httpRequestResult.httpRequestResult(port, false, response_message);
                    return;
                }

                String postData;
                // Parse received web page
                try {
                    postData = AnelReceiveSendHTTP.createHTTP_Post_byHTTP_response(response_message, new_name, new AnelTimer[5]);
                } catch (UnsupportedEncodingException e) {
                    httpRequestResult.httpRequestResult(port, false, "url_encode failed");
                    return;
                } catch (SAXException e) {
                    e.printStackTrace();
                    httpRequestResult.httpRequestResult(port, false, "Html Parsing failed");
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    httpRequestResult.httpRequestResult(port, false, "Html IO Parsing failed");
                    return;
                }

                HttpThreadPool.execute(new HttpThreadPool.HTTPRunner<>(ci, getData, postData,
                        port, true, new HttpThreadPool.HTTPCallback<Executable>() {
                    @Override
                    public void httpResponse(Executable port, boolean callback_success,
                                             String response_message) {
                        if (callback_success) {
                            port.title = new_name;
                        }
                        httpRequestResult.httpRequestResult(port, callback_success, response_message);
                    }
                }
                ));
            }
        }
        ));
    }

    @Override
    public void executeTransaction(onExecutionFinished callback) {
        class Entry {
            public List<ExecutableAndCommand> list = new ArrayList<>();
            public IOConnection connection;
        }
        TreeMap<String, Entry> commands_grouped_by_devices = new TreeMap<>();

        if (callback != null)
            callback.addExpected(command_list.size());
        int success = 0;

        // add to tree
        for (ExecutableAndCommand executableAndCommand : command_list) {
            DeviceIOConnections deviceIOConnections = dataService.connections.openDevice(executableAndCommand.executable.deviceUID);
            if (deviceIOConnections == null) continue;
            IOConnection connection = deviceIOConnections.findReachable();
            if (connection == null) continue;

            Entry entry = commands_grouped_by_devices.get(connection.getUid());
            if (entry == null) entry = new Entry();
            entry.list.add(executableAndCommand);
            entry.connection = connection;
            commands_grouped_by_devices.put(connection.getUid(), entry);
        }

        // executeToggle by device
        for (Entry entry : commands_grouped_by_devices.values()) {
            success += executeDeviceBatch(entry.connection, entry.list);
        }

        command_list.clear();

        if (callback != null)
            callback.accumulateSuccessFail(success, command_list.size() - success);
    }

    @Override
    public String getPluginID() {
        return PLUGIN_ID;
    }

    @Override
    public String getLocalizedName() {
        return App.getAppString(R.string.plugin_anel);
    }

    @Override
    public void openConfigurationPage(Credentials credentials) {
        DeviceIOConnections deviceIOConnections = dataService.connections.openDevice(credentials.deviceUID);
        if (deviceIOConnections == null || deviceIOConnections.size() == 0) {
            Toast.makeText(dataService, R.string.device_test_not_reachable, Toast.LENGTH_SHORT).show();
            return;
        }
        IOConnection ci = deviceIOConnections.iterator().next();

        int http_port = 80;
        for (Iterator<IOConnection> iterator = deviceIOConnections.iterator(); iterator.hasNext(); ) {
            IOConnection ioConnection = iterator.next();
            if (ioConnection instanceof IOConnectionHTTP) {
                http_port = ioConnection.getDestinationPort();
                break;
            }
        }

        Intent browse = new Intent(Intent.ACTION_VIEW,
                Uri.parse("http://" + ci.getDestinationHost() + ":" + Integer.valueOf(http_port).toString()));
        dataService.startActivity(browse);
    }

    @Override
    public boolean hasEditableCredentials() {
        return true;
    }

    @Override
    public boolean isNewIOConnectionAllowed(Credentials credentials) {
        return true;
    }

    @Override
    public void addNewIOConnection(@NonNull Credentials credentials, @NonNull onNewIOConnection callback) {
        IOConnectionHTTP ioConnectionHTTP = new IOConnectionHTTP(credentials);
        ioConnectionHTTP.connectionUID = UUID.randomUUID().toString();
        callback.newIOConnection(ioConnectionHTTP);
    }

//
//    @Override
//    public Timer getNextFreeAlarm(Executable port, int type, int command) {
//        return anelAlarm.getNextFreeAlarm(port, type, command);
//    }
//
//    @Override
//    public void saveAlarm(Timer timer, onHttpRequestResult callback) {
//        anelAlarm.saveAlarm(dataService, timer, callback);
//    }
//
//    @Override
//    public void removeAlarm(Timer timer, onHttpRequestResult callback) {
//        anelAlarm.removeAlarm(dataService, timer, callback);
//    }
//
//    @Override
//    public void requestAlarms(Executable port, TimerCollection timers) {
//        final IOConnectionHTTP ci = (IOConnectionHTTP) port.executableIO.getFirstReachableConnection("HTTP");
//        if (ci == null)
//            return;
//
//        anelAlarm.requestAlarms(port, ci, timers, null, null);
//    }


}
