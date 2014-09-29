package oly.netpowerctrl.anel;

import java.io.UnsupportedEncodingException;
import java.net.NetworkInterface;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.device_ports.DevicePort;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.devices.DeviceConnection;
import oly.netpowerctrl.devices.DeviceConnectionHTTP;
import oly.netpowerctrl.devices.DeviceConnectionUDP;
import oly.netpowerctrl.devices.DeviceFeatureTemperature;
import oly.netpowerctrl.listen_service.ListenService;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.network.UDPReceiving;

class AnelUDPDeviceDiscoveryThread extends UDPReceiving {
    public static AnelPlugin anelPlugin;

    public AnelUDPDeviceDiscoveryThread(AnelPlugin anelPlugin, int port) {
        super(port, "AnelDeviceDiscoveryThread");
        AnelUDPDeviceDiscoveryThread.anelPlugin = anelPlugin;
    }

    private static Device createReceivedAnelDevice(String DeviceName, String MacAddress) {
        Device di = new Device(anelPlugin.getPluginID());
        di.setPluginInterface(anelPlugin);
        di.DeviceName = DeviceName;
        di.UniqueDeviceID = MacAddress;
        // Default values for user and password
        di.UserName = "admin";
        di.Password = "anel";
        di.setUpdatedNow();
        return di;
    }

    @Override
    public void parsePacket(final byte[] message, int length, int receive_port, NetworkInterface localInterface) {
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
        final Device di = createReceivedAnelDevice(msg[1].trim(), msg[5]);
        DeviceConnection deviceConnection = new DeviceConnectionUDP(di, HostName, receive_port, SharedPrefs.getInstance().getDefaultSendPort());
        deviceConnection.setIsAssignedByDevice(true);
        di.addConnection(deviceConnection);

        int disabledOutlets = 0;
        int numOutlets = 8; // normally, the device sends info for 8 outlets no matter how many are actually equipped

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
                httpPort = Integer.parseInt(msg[15]);
            } catch (NumberFormatException ignored) {
                httpPort = -1;
            }

            deviceConnection = new DeviceConnectionHTTP(di, HostName, httpPort);
            deviceConnection.setIsAssignedByDevice(true);
            di.addConnection(deviceConnection);
        }
        // IO ports
        if (msg.length > 25) {
            // 1-based id. IO output range: 11..19, IO input range is 21..29
            int io_id = 10;
            for (int i = 16; i <= 23; ++i) {
                io_id++;
                String io_port[] = msg[i].split(",");
                if (io_port.length != 3) continue;

                DevicePort oi = new DevicePort(di, DevicePort.DevicePortType.TypeToggle);

                if (io_port[1].equals("1")) // input
                    oi.id = io_id + 10;
                else
                    oi.id = io_id;

                oi.setDescription(io_port[0]);
                oi.current_value = io_port[2].equals("1") ? DevicePort.ON : DevicePort.OFF;
                di.putPort(oi);
            }
            di.Features.add(new DeviceFeatureTemperature(msg[24]));
            di.Version = msg[25].trim();
        }
        // For old firmwares
        else if (msg.length < 14) {
            numOutlets = msg.length - 6;
            di.addConnection(new DeviceConnectionHTTP(di, HostName, -1));
        }

        for (int i = 0; i < numOutlets; i++) {
            String outlet[] = msg[6 + i].split(",");
            if (outlet.length < 1)
                continue;
            DevicePort oi = new DevicePort(di, DevicePort.DevicePortType.TypeToggle);
            oi.id = i + 1; // 1-based
            oi.setDescription(outlet[0]);
            if (outlet.length > 1)
                oi.current_value = outlet[1].equals("1") ? DevicePort.ON : DevicePort.OFF;
            oi.Disabled = (disabledOutlets & (1 << i)) != 0;

            di.putPort(oi);
        }

        AppData.getInstance().onDeviceUpdatedOtherThread(di);
    }
}
