package oly.netpowerctrl.anel;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DeviceConnection;
import oly.netpowerctrl.device_base.device.DeviceConnectionHTTP;
import oly.netpowerctrl.device_base.device.DeviceConnectionUDP;
import oly.netpowerctrl.device_base.device.DeviceFeatureTemperature;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.device_base.executables.ExecutableType;
import oly.netpowerctrl.network.UDPReceiving;
import oly.netpowerctrl.pluginservice.PluginService;
import oly.netpowerctrl.utils.Logging;

class AnelUDPReceive extends UDPReceiving {
    public static AnelPlugin anelPlugin;

    public AnelUDPReceive(AnelPlugin anelPlugin, int port) {
        super(port, "AnelDeviceDiscoveryThread");
        AnelUDPReceive.anelPlugin = anelPlugin;
        Logging.getInstance().logDetect("UDP Listen " + String.valueOf(port));
    }

    private static Device createReceivedAnelDevice(String MacAddress) {
        Device di = new Device(anelPlugin.getPluginID(), true);
        di.setPluginInterface(anelPlugin);
        di.setUniqueDeviceID(MacAddress);
        // Default values for user and password
        di.setUserName("admin");
        di.setPassword("anel");
        return di;
    }

    @Override
    public void parsePacket(final byte[] message, int length, int receive_port, InetAddress local, InetAddress peer) {
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
            Logging.getInstance().logDetect("UDP Receive Error\n" + incoming);
            return;
        }

        if ((msg.length >= 4) && (msg[3].trim().equals("Err"))) {
            String name = msg[1].trim();
            String errMessage = msg[2].trim();
            if (errMessage.trim().equals("NoPass"))
                errMessage = PluginService.getService().getString(R.string.error_nopass);
            Logging.getInstance().logDetect("UDP Device Error\n" + name + " " + errMessage);
            return;
        }

        final String HostName = msg[2];
        final String DeviceName = msg[1].trim();
        final String MacAddress = msg[5].trim();

        Logging.getInstance().logDetect("UDP Device detected\n" + DeviceName);

        AppData appData = anelPlugin.getPluginService().getAppData();
        boolean isNewDevice = false;
        Device device = appData.findDevice(MacAddress);
        if (device == null) {
            device = createReceivedAnelDevice(MacAddress);
            isNewDevice = true;
        } else
            device.lockDevice();

        // Normally, the device sends info for 8 outlets no matter how many are actually equipped.
        // We will filter out any disabled outlets
        int disabledOutlets = 0;
        int numOutlets = 8;

        device.setDeviceName(DeviceName);

        List<DeviceConnection> deviceConnectionList = new ArrayList<>();

        DeviceConnection deviceConnection = new DeviceConnectionUDP(device, HostName, receive_port, SharedPrefs.getInstance().getDefaultSendPort());
        deviceConnection.setReceiveAddress(peer);
        deviceConnection.makeAssignedByDevice();
        deviceConnectionList.add(deviceConnection);

        // For old firmwares
        if (msg.length < 14) {
            numOutlets = msg.length - 6;
            deviceConnection = new DeviceConnectionHTTP(device, HostName, 80);
            deviceConnection.makeAssignedByDevice();
            deviceConnection.setReceiveAddress(peer);
            deviceConnectionList.add(deviceConnection);
        } else
            // Current firmware v4
            if (msg.length > 14) {
                try {
                    disabledOutlets = Integer.parseInt(msg[14]);
                } catch (NumberFormatException ignored) {
                }
            }
        if (msg.length > 15) {
            int httpPort;
            try {
                httpPort = Integer.parseInt(msg[15].trim());
                deviceConnection = new DeviceConnectionHTTP(device, HostName, httpPort);
                deviceConnection.makeAssignedByDevice();
                deviceConnection.setReceiveAddress(peer);
                deviceConnectionList.add(deviceConnection);

            } catch (NumberFormatException ignored) {
            }
        }

        device.lockDevicePorts();
        // IO ports
        if (msg.length > 25) {
            // 1-based id. IO output range: 11..19, IO input range is 21..29
            int io_id = 10;
            for (int i = 16; i <= 23; ++i) {
                io_id++;
                String io_port[] = msg[i].split(",");
                if (io_port.length != 3) continue;

                // input if io_port[1].equals("1") otherwise output
                DevicePort devicePort = DevicePort.create(device, ExecutableType.TypeToggle, io_port[1].equals("1") ? io_id + 10 : io_id);

                devicePort.setTitle(io_port[0]);
                devicePort.current_value = io_port[2].equals("1") ? DevicePort.ON : DevicePort.OFF;
                device.updatePort(devicePort);
            }
            device.addFeatures(new DeviceFeatureTemperature(msg[24]));
            device.setVersion(msg[25].trim());
        }

        device.makeAllDevicePortsInvalid();
        for (int i = 0; i < numOutlets; i++) {
            String outlet[] = msg[6 + i].split(",");
            if (outlet.length < 1)
                continue;
            DevicePort devicePort = DevicePort.create(device, ExecutableType.TypeToggle, i + 1); // 1-based id
            devicePort.setTitle(outlet[0]);
            if (outlet.length > 1)
                devicePort.current_value = outlet[1].equals("1") ? DevicePort.ON : DevicePort.OFF;
            boolean disabled = (disabledOutlets & (1 << i)) != 0;
            if (!disabled)
                device.updatePort(devicePort);
        }
        device.removeInvalidDevicePorts();
        device.releaseDevicePorts();

        device.replaceAutomaticAssignedConnections(deviceConnectionList);
        device.releaseDevice();

        if (isNewDevice)
            appData.updateDeviceFromOtherThread(device);
        else
            appData.updateExistingDeviceFromOtherThread(device, true);
    }
}
