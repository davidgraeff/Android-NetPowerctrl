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
import oly.netpowerctrl.listen_service.ListenService;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.network.UDPReceiving;

class AnelUDPDeviceDiscoveryThread extends UDPReceiving {
    public static AnelPlugin anelPlugin;

    public AnelUDPDeviceDiscoveryThread(AnelPlugin anelPlugin, int port) {
        super(port, "AnelDeviceDiscoveryThread");
        AnelUDPDeviceDiscoveryThread.anelPlugin = anelPlugin;
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
        try {
            msg = new String(message, 0, length, "iso8859-1").split(":");
        } catch (UnsupportedEncodingException e) {
            return;
        }

        if (msg.length < 3) {
            return;
        }

        if ((msg.length >= 4) && (msg[3].trim().equals("Err"))) {
            App.getMainThreadHandler().post(new Runnable() {
                public void run() {
                    ListenService service = ListenService.getService();
                    if (service == null)
                        return;
                    String errMessage = msg[2].trim();
                    if (errMessage.trim().equals("NoPass"))
                        errMessage = ListenService.getService().getString(R.string.error_nopass);
                    AppData.getInstance().onDeviceErrorByName(
                            service,
                            msg[1].trim(),
                            errMessage);
                }
            });

            return;
        }

        final String HostName = msg[2];
        final String DeviceName = msg[1].trim();
        final String MacAddress = msg[5].trim();

        boolean isNewDevice = false;
        Device device = AppData.getInstance().findDevice(MacAddress);
        if (device == null) {
            device = createReceivedAnelDevice(MacAddress);
            isNewDevice = true;
        } else
            device.lockDevice();

        int disabledOutlets = 0;
        int numOutlets = 8; // normally, the device sends info for 8 outlets no matter how many are actually equipped

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

                DevicePort devicePort = new DevicePort(device, ExecutableType.TypeToggle);

                if (io_port[1].equals("1")) // input
                    devicePort.id = io_id + 10;
                else
                    devicePort.id = io_id;

                devicePort.setTitle(io_port[0]);
                devicePort.current_value = io_port[2].equals("1") ? DevicePort.ON : DevicePort.OFF;
                device.updatePort(devicePort);
            }
            device.addFeatures(new DeviceFeatureTemperature(msg[24]));
            device.setVersion(msg[25].trim());
        }

        for (int i = 0; i < numOutlets; i++) {
            String outlet[] = msg[6 + i].split(",");
            if (outlet.length < 1)
                continue;
            DevicePort devicePort = new DevicePort(device, ExecutableType.TypeToggle);
            devicePort.id = i + 1; // 1-based
            devicePort.setTitle(outlet[0]);
            if (outlet.length > 1)
                devicePort.current_value = outlet[1].equals("1") ? DevicePort.ON : DevicePort.OFF;
            devicePort.Disabled = (disabledOutlets & (1 << i)) != 0;

            device.updatePort(devicePort);
        }
        device.releaseDevicePorts();

        device.replaceAutomaticAssignedConnections(deviceConnectionList);
        device.releaseDevice();

        if (isNewDevice)
            AppData.getInstance().updateDeviceFromOtherThread(device);
        else
            AppData.getInstance().updateExistingDeviceFromOtherThread(device);
    }
}
