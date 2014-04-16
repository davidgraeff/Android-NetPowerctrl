package oly.netpowerctrl.anel;

import android.os.Handler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.DevicePort;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.ShowToast;

public class AnelDeviceDiscoveryThread extends Thread {
    private int receive_port;
    private boolean keep_running;
    private DatagramSocket socket;
    private AnelPlugin anelPlugin;

    public AnelDeviceDiscoveryThread(AnelPlugin anelPlugin, int port) {
        this.anelPlugin = anelPlugin;
        receive_port = port;
        socket = null;
    }

    public void run() {

        keep_running = true;
        while (keep_running) {
            try {
                byte[] message = new byte[1500];
                DatagramPacket p = new DatagramPacket(message, message.length);
                socket = new DatagramSocket(receive_port);
                socket.setReuseAddress(true);
                while (keep_running) {
                    socket.receive(p);
                    parsePacket(new String(message, 0, p.getLength(), "latin-1"), receive_port);
                }
                socket.close();
            } catch (final IOException e) {
                if (keep_running) { // no message if we were interrupt()ed
                    String msg = String.format(NetpowerctrlApplication.instance.getResources().getString(R.string.error_listen_thread_exception), receive_port);
                    msg += e.getLocalizedMessage();
                    if (receive_port < 1024)
                        msg += NetpowerctrlApplication.instance.getResources().getString(R.string.error_port_lt_1024);
                    ShowToast.FromOtherThread(NetpowerctrlApplication.instance, msg);
                }
                break;
            }
        }
    }

    @Override
    public void interrupt() {
        keep_running = false;
        if (socket != null) {
            socket.close();
        }
        super.interrupt();
    }

    private DeviceInfo createReceivedAnelDevice(String DeviceName, String HostName,
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

    void parsePacket(final String message, int receive_port) {
        final String msg[] = message.split(":");
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
                di.HttpPort = 80;
            }
            // IO ports
            if (msg.length > 23) {
                for (int i = 16; i <= 23; ++i) {
                    String io_port[] = msg[i].split(",");
                    if (io_port.length != 3) continue;
                    // Filter out inputs
                    if (io_port[1].equals("1"))
                        continue;

                    DevicePort oi = new DevicePort(di, DevicePort.DevicePortType.TypeToggle);
                    oi.id = (i - 16 + 1) + 10; // 1-based, and for IO we add 10. range: 11..19
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
            di.HttpPort = 80;
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

        new Handler(NetpowerctrlApplication.instance.getMainLooper()).post(new Runnable() {
            public void run() {
                NetpowerctrlApplication.getDataController().onDeviceUpdated(di);
            }
        });
    }

    /**
     * @return Return the receive port of this thread
     */
    public int getPort() {
        return receive_port;
    }
}
