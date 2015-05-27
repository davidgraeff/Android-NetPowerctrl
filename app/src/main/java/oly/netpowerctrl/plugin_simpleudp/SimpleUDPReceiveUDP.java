package oly.netpowerctrl.plugin_simpleudp;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;

import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.devices.Credentials;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.executables.ExecutableType;
import oly.netpowerctrl.ioconnection.DeviceIOConnections;
import oly.netpowerctrl.ioconnection.IOConnectionUDP;
import oly.netpowerctrl.network.UDPReceiving;
import oly.netpowerctrl.utils.Logging;

/**
 * SimpleUDP Packet Format (Receive type)
 * * All ascii based, two characters not allowed in names and ids: newline (\n) and tabulator (\t).
 * <p/>
 * General structure (lines are separated by newlines)
 * ---------------------------------------------------
 * Header (always SimpleUDP_info or SimpleUDP_info_ack or SimpleUDP_info_fail)
 * DeviceUniqueID (often Mac address of peer)
 * Name of device (english name of device, user may rename it within the app)
 * Version string (no special formatting)
 * List of actions (one action per line, action consists of multiple tokens)
 * <p/>
 * If you send SimpleUDP_detect, you will receive a full SimpleUDP_info with all known actions. If
 * you send a SimpleUDP_cmd, the peer will acknowledge with a SimpleUDP_info_ack where only the affected
 * actions are listed.
 * <p/>
 * Structure of an action
 * ----------------------
 * TYPE \t actionID string \t action name \t optional value
 * <p/>
 * TYPE is one of (STATELESS,TOGGLE,RANGE,NOTEXIST), the actionID may be any string but have to be unique among
 * all actions. The action name can be any string as long as it does not contain \n and \t. The
 * value is only necessary if the type is TOGGLE or RANGE. If the Header is equal to SimpleUDP_info_fail
 * the TYPE may be NOTEXIST if you requested a command on a non existing action.
 * <p/>
 * Example packet:
 * <p/>
 * SimpleUDP_info\n
 * 11:22:33:44:55:66\n
 * device_name\n
 * 1.0-2015.05.10\n
 * STATELESS\tACTION1\tAction Name
 * TOGGLE\tACTION2\tAction Name\t1
 */
class SimpleUDPReceiveUDP extends UDPReceiving {
    public static SimpleUDPPlugin plugin;

    public SimpleUDPReceiveUDP(SimpleUDPPlugin plugin, int port) {
        super(port, "SimpleUDPReceiveUDP");
        SimpleUDPReceiveUDP.plugin = plugin;
        Logging.getInstance().logDetect("SimpleUDP Listen " + String.valueOf(port));
    }

    @Override
    public void parsePacket(final byte[] message, int length, InetAddress local, InetAddress peer) {
        final String msg[];
        final String incoming;
        try {
            incoming = new String(message, 0, length, "utf-8");
            msg = incoming.split("\n");
        } catch (UnsupportedEncodingException e) { // Will not happen
            e.printStackTrace();
            return;
        }

        if (msg.length < 2 || !msg[0].startsWith("SimpleUDP_info")) {
            Logging.getInstance().logDetect("SimpleUDP Receive Error\n" + incoming);
            return;
        }

        final String DeviceUniqueID = msg[1];
        final String DeviceName = msg[2];
        final String Version = msg[3];
        final int action_start_index = 4;

        Logging.getInstance().logDetect("SimpleUDP Device detected\n" + DeviceName);

        // Create or get the credentials (device) object. It consists of a unique id, device name, username, password.
        DataService dataService = plugin.getDataService();
        Credentials credentials = dataService.credentials.findByUID(DeviceUniqueID);
        if (credentials == null) {
            credentials = plugin.createDefaultCredentials(DeviceUniqueID);
        }

        credentials.version = Version;

        // Only update device name if it is empty, we do not want to overwrite the name given by the user.
        if (credentials.deviceName.isEmpty())
            credentials.setDeviceName(DeviceName);

        // The order is important: First save the credentials, then save the executables, then save the connections.
        // Because: connections need the credentials object. Executables get the reachability information from the updated connections.

        dataService.credentials.put(credentials);

        for (int i = action_start_index; i < msg.length; ++i) {
            if (msg[i].trim().length() == 0) continue;
            String outlet[] = msg[i].trim().split("\t");
            if (outlet.length < 3) {
                Logging.getInstance().logDetect("SimpleUDP Receive Error outlet too short\n" + msg[i]);
                continue;
            }

            String uid = SimpleUDPPlugin.makeExecutableUID(DeviceUniqueID, outlet[1]);
            Executable executable = dataService.executables.findByUID(uid);
            if (executable == null) executable = new Executable();

            switch (outlet[0]) {
                case "STATELESS":
                    executable.ui_type = ExecutableType.TypeStateless;
                    break;
                case "VALUE":
                    if (outlet.length <= 5) {
                        Logging.getInstance().logDetect("SimpleUDP Error. Entry to short\n" + msg[i]);
                        continue;
                    }
                    executable.ui_type = ExecutableType.TypeRangedValue;
                    executable.current_value = Integer.valueOf(outlet[3]);
                    executable.min_value = Integer.valueOf(outlet[4]);
                    executable.max_value = Integer.valueOf(outlet[5]);
                    break;
                case "TOGGLE":
                    if (outlet.length <= 5) {
                        Logging.getInstance().logDetect("SimpleUDP Error. Entry to short\n" + msg[i]);
                        continue;
                    }
                    executable.ui_type = ExecutableType.TypeToggle;
                    executable.current_value = Integer.valueOf(outlet[3]);
                    executable.min_value = Integer.valueOf(outlet[4]);
                    executable.max_value = Integer.valueOf(outlet[5]);
                    break;
                case "FAIL_UNKNOWN_CMD":
                    Logging.getInstance().logDetect("SimpleUDP: Unknown command\n" + outlet[1]);
                    continue;
                case "FAIL_SYNTAX":
                    Logging.getInstance().logDetect("SimpleUDP: Wrong action syntax");
                    continue;
                case "FAIL_NOTEXIST":
                    Logging.getInstance().logDetect("SimpleUDP: Action does not exist\n" + outlet[1]);
                    continue;
                default:
                    Logging.getInstance().logDetect("SimpleUDP Receive Error outlet type\n" + outlet[1]);
                    continue;
            }

            executable.deviceUID = credentials.getUid();
            executable.setUid(uid);
            executable.setCredentials(credentials, dataService.connections);
            executable.title = outlet[2];
            dataService.executables.put(executable);
        }

        // IO Connections
        DeviceIOConnections deviceIOConnections = dataService.connections.openCreateDevice(DeviceUniqueID);

        {
            String ioConnection_uid = DeviceUniqueID + "UDP";
            IOConnectionUDP ioConnection = (IOConnectionUDP) deviceIOConnections.findByUID(ioConnection_uid);
            if (ioConnection == null)
                ioConnection = new IOConnectionUDP(credentials);
            ioConnection.setReceiveAddress(peer);
            ioConnection.connectionUID = ioConnection_uid;
            ioConnection.hostName = peer.getHostName();
            ioConnection.PortUDPSend = SimpleUDPPlugin.PORT_SEND;
            ioConnection.PortUDPReceive = receive_port;
            ioConnection.setStatusMessage(null);
            ioConnection.incReceivedPackets();
            dataService.connections.put(ioConnection);
        }
    }
}
