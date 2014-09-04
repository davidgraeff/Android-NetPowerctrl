package oly.netpowerctrl.network;

import android.content.Context;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.NetworkInterface;

import oly.netpowerctrl.R;
import oly.netpowerctrl.listen_service.ListenService;
import oly.netpowerctrl.utils.ShowToast;

abstract public class UDPReceiving extends Thread {
    private final int receive_port;
    protected DatagramPacket receivedDatagram;
    private boolean keep_running;
    private DatagramSocket socket;

    protected UDPReceiving(int port, String threadName) {
        super(threadName);
        receive_port = port;
        socket = null;
    }

    public void run() {
        Context context = ListenService.getService();
        if (context == null)
            return;

        keep_running = true;
        while (keep_running) {
            try {
                byte[] message = new byte[1500];
                receivedDatagram = new DatagramPacket(message, message.length);
                socket = new DatagramSocket(receive_port);
                socket.setReuseAddress(true);
                while (keep_running) {
                    socket.receive(receivedDatagram);
                    parsePacket(message, receivedDatagram.getLength(), receive_port,
                            NetworkInterface.getByInetAddress(socket.getLocalAddress()));
                }
                socket.close();
            } catch (final IOException e) {
                if (keep_running) { // no message if we were interrupt()ed
                    String msg = context.getString(R.string.error_listen_thread_exception, receive_port);
                    msg += e.getLocalizedMessage();
                    if (receive_port < 1024)
                        msg += context.getString(R.string.error_port_lt_1024);
                    ShowToast.FromOtherThread(context, msg);
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

    protected abstract void parsePacket(final byte[] message, int length,
                                        int receive_port, NetworkInterface localInterface);

    /**
     * @return Return the receive port of this thread
     */
    public int getPort() {
        return receive_port;
    }
}
