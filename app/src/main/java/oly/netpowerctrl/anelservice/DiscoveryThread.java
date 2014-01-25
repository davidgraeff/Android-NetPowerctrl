package oly.netpowerctrl.anelservice;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.OutletInfo;
import oly.netpowerctrl.utils.ShowToast;

class DiscoveryThread extends Thread {
    private int receive_port;
    private boolean keep_running;
    private DatagramSocket socket;
    private NetpowerctrlService service;

    public DiscoveryThread(int port, NetpowerctrlService service) {
        this.service = service;
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
                    String msg = String.format(service.getResources().getString(R.string.error_listen_thread_exception), receive_port);
                    msg += e.getLocalizedMessage();
                    if (receive_port < 1024)
                        msg += service.getResources().getString(R.string.error_port_lt_1024);
                    ShowToast.FromOtherThread(service, msg);
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

    void parsePacket(final String message, int receive_port) {
        String msg[] = message.split(":");
        if (msg.length < 3) {
            return;
        }

        if ((msg.length >= 4) && (msg[3].trim().equals("Err"))) {
            service.notifyErrorObservers(msg[1].trim(), msg[2].trim());
            return;
        }

        final DeviceInfo di = DeviceInfo.createReceivedDevice(msg[1].trim(),
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

                    OutletInfo oi = new OutletInfo(di);
                    oi.OutletNumber = i - 16 + 1; // 1-based
                    oi.setDescriptionByDevice(io_port[0]);
                    oi.State = io_port[1].equals("1");
                    di.IOs.add(oi);
                }
                di.Temperature = msg[24];
                di.FirmwareVersion = msg[25];
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
            OutletInfo oi = new OutletInfo(di);
            oi.OutletNumber = i + 1; // 1-based
            oi.setDescriptionByDevice(outlet[0]);
            if (outlet.length > 1)
                oi.State = outlet[1].equals("1");
            oi.Disabled = (disabledOutlets & (1 << i)) != 0;

            di.Outlets.add(oi);
        }

        service.notifyObservers(di);
    }
}
