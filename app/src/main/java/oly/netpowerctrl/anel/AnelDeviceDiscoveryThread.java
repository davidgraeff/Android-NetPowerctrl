package oly.netpowerctrl.anel;

import android.os.Handler;

import java.io.UnsupportedEncodingException;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.devices.DeviceInfo;
import oly.netpowerctrl.devices.DevicePort;
import oly.netpowerctrl.network.UDPReceiving;
import oly.netpowerctrl.preferences.SharedPrefs;

class AnelDeviceDiscoveryThread extends UDPReceiving {
    public static AnelPlugin anelPlugin;

    public AnelDeviceDiscoveryThread(AnelPlugin anelPlugin, int port) {
        super(port);
        AnelDeviceDiscoveryThread.anelPlugin = anelPlugin;
    }

    private static DeviceInfo createReceivedAnelDevice(String DeviceName, String HostName,
                                                       String MacAddress, int receive_port) {
        DeviceInfo di = DeviceInfo.createNewDevice(anelPlugin.getPluginID());
        di.DeviceName = DeviceName;
        di.HostName = HostName;
        di.UniqueDeviceID = MacAddress;
        di.ReceivePort = receive_port;
        // Default values for user and password
        di.UserName = "admin";
        di.Password = "anel";

        di.SendPort = SharedPrefs.getDefaultSendPort();
        di.setReachable();
        di.setUpdatedNow();
        return di;
    }

    @Override
    public void parsePacket(final byte[] message, int length, int receive_port) {
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
            new Handler(NetpowerctrlApplication.instance.getMainLooper()).post(new Runnable() {
                public void run() {
                    String errMessage = msg[2].trim();
                    if (errMessage.trim().equals("NoPass"))
                        errMessage = NetpowerctrlApplication.instance.getString(R.string.error_nopass);
                    NetpowerctrlApplication.getDataController().onDeviceErrorByName(msg[1].trim(), errMessage);
                }
            });

            return;
        }

        final DeviceInfo di = createReceivedAnelDevice(msg[1].trim(),
                msg[2], msg[5], receive_port);

        int disabledOutlets = 0;
        int numOutlets = 8; // normally, the device sends info for 8 outlets no matter how many are actually equipped

        // Current firmware v4
        if (msg.length > 14) {
            try {
                disabledOutlets = Integer.parseInt(msg[14]);
            } catch (NumberFormatException ignored) {
            }
            try {
                di.HttpPort = Integer.parseInt(msg[15]);
            } catch (NumberFormatException ignored) {
                di.HttpPort = -1;
            }
            // IO ports
            if (msg.length > 23) {
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
                    di.add(oi);
                }
                di.Temperature = msg[24];
                di.Version = msg[25].trim();
            }

        }
        // For old firmwares
        else if (msg.length < 14) {
            numOutlets = msg.length - 6;
            di.HttpPort = -1;
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

            di.add(oi);
        }

        NetpowerctrlApplication.getDataController().onDeviceUpdatedOtherThread(di);
    }
}
