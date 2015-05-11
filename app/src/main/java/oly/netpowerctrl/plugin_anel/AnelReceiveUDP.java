package oly.netpowerctrl.plugin_anel;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.devices.Credentials;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.ioconnection.DeviceIOConnections;
import oly.netpowerctrl.ioconnection.IOConnectionHTTP;
import oly.netpowerctrl.ioconnection.IOConnectionUDP;
import oly.netpowerctrl.network.UDPReceiving;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.Logging;
import oly.netpowerctrl.utils.Streams;

class AnelReceiveUDP extends UDPReceiving {
    public static AnelPlugin anelPlugin;

    public AnelReceiveUDP(AnelPlugin anelPlugin, int port) {
        super(port, "AnelDeviceDiscoveryThread");
        AnelReceiveUDP.anelPlugin = anelPlugin;
        Logging.getInstance().logDetect("Anel UDP Listen " + String.valueOf(port));
    }

    @Override
    public void parsePacket(final byte[] message, int length, InetAddress local, InetAddress peer) {
        final String msg[];
        final String incoming;
        try {
            incoming = new String(message, 0, length, "iso8859-1");
            msg = incoming.split(":");
        } catch (UnsupportedEncodingException e) { // Will not happen
            e.printStackTrace();
            return;
        }

        if (msg.length < 3) {
            Logging.getInstance().logDetect("Anel UDP Receive Error\n" + incoming);
            return;
        }

        if ((msg.length >= 4) && (msg[3].trim().equals("Err"))) {
            String name = msg[1].trim();
            String errMessage = msg[2].trim();
            if (errMessage.trim().equals("NoPass"))
                errMessage = DataService.getService().getString(R.string.error_nopass);
            Logging.getInstance().logDetect("Anel UDP Device Error\n" + name + " " + errMessage);
            return;
        }

        final String HostName = msg[2];
        final String DeviceName = msg[1].trim();
        final String DeviceUniqueID = msg[5].trim(); // MacAddress

        Logging.getInstance().logDetect("Anel UDP Device detected\n" + DeviceName);

        // Create or get the credentials (device) object. It consists of a unique id, device name, username, password.
        DataService dataService = anelPlugin.getDataService();
        Credentials credentials = dataService.credentials.findByUID(DeviceUniqueID);
        if (credentials == null) {
            credentials = anelPlugin.createDefaultCredentials(DeviceUniqueID);
        }

        // Normally, the device sends info for 8 outlets no matter how many are actually equipped.
        // We will filter out any disabled outlets
        int disabledOutlets = 0;
        int numOutlets = 8;
        int httpPort = 80;

        // For old firmwares
        if (msg.length < 14) {
            numOutlets = msg.length - 6;

        } else
            // Current firmware v4
            if (msg.length > 14) {
                try {
                    disabledOutlets = Integer.parseInt(msg[14]);
                } catch (NumberFormatException ignored) {
                }
            }
        if (msg.length > 15) {
            try {
                httpPort = Integer.parseInt(msg[15].trim());
            } catch (NumberFormatException ignored) {
            }
        }

        if (msg.length > 25)
            credentials.version = msg[25].trim();

        // Only update device name if it is empty, we do not want to overwrite the name given by the user.
        if (credentials.deviceName.isEmpty())
            credentials.setDeviceName(DeviceName);

        // The order is important: First save the credentials, then save the executables, then save the connections.
        // Because: connections need the credentials object. Executables get the reachability information from the updated connections.

        dataService.credentials.put(credentials);

        // IO ports
        if (msg.length > 25) {
            // 1-based id. IO output range: 11..19, IO input range is 21..29
            int io_id = 10;
            for (int i = 16; i <= 23; ++i) {
                io_id++;
                List<String> io_port = new ArrayList<>();
                Streams.splitNonRegex(io_port, msg[i], ",");
                if (io_port.size() != 3) continue;

                String uid = AnelPlugin.makeExecutableUID(DeviceUniqueID, (io_port.get(1).equals("1") ? io_id + 10 : io_id));
                // input if io_port[1].equals("1") otherwise output
                Executable executable = dataService.executables.findByUID(uid);
                if (executable == null) executable = new Executable();
                anelPlugin.fillExecutable(executable, credentials, uid, io_port.get(2).equals("1") ? Executable.ON : Executable.OFF);
                executable.title = io_port.get(0);
                dataService.executables.put(executable);
            }
        }

        // Executables
        for (int i = 0; i < numOutlets; i++) {
            String outlet[] = msg[6 + i].split(",");
            if (outlet.length < 1)
                continue;
            boolean disabled = (disabledOutlets & (1 << i)) != 0;
            if (disabled) continue;

            String uid = AnelPlugin.makeExecutableUID(DeviceUniqueID, i + 1);  // 1-based id
            Executable executable = dataService.executables.findByUID(uid);
            if (executable == null) executable = new Executable();
            anelPlugin.fillExecutable(executable, credentials, uid, 0);
            executable.title = outlet[0];
            if (outlet.length > 1)
                executable.current_value = outlet[1].equals("1") ? Executable.ON : Executable.OFF;
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
            ioConnection.hostName = HostName;
            ioConnection.PortUDPSend = SharedPrefs.getInstance().getDefaultSendPort();
            ioConnection.PortUDPReceive = receive_port;
            ioConnection.setStatusMessage(null);
            ioConnection.incReceivedPackets();
            dataService.connections.put(ioConnection);
        }

        {
            String ioConnection_uid = DeviceUniqueID + "HTTP";
            IOConnectionHTTP ioConnection = (IOConnectionHTTP) deviceIOConnections.findByUID(ioConnection_uid);
            if (ioConnection == null)
                ioConnection = new IOConnectionHTTP(credentials);
            ioConnection.setReceiveAddress(peer);
            ioConnection.connectionUID = ioConnection_uid;
            ioConnection.hostName = HostName;
            ioConnection.PortHttp = httpPort;
            ioConnection.setStatusMessage(null);
            dataService.connections.put(ioConnection);
        }


    }
}
