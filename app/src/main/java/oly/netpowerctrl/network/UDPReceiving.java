package oly.netpowerctrl.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import oly.netpowerctrl.App;
import oly.netpowerctrl.R;
import oly.netpowerctrl.ui.notifications.InAppNotifications;

abstract public class UDPReceiving extends Thread {
    protected final int receive_port;
    protected DatagramPacket receivedDatagram;
    private boolean keep_running;
    private DatagramSocket socket;

    protected UDPReceiving(int port, String threadName) {
        super(threadName);
        receive_port = port;
        socket = null;
    }

    public void run() {
        keep_running = true;
        while (keep_running) {
            try {
                byte[] message = new byte[1500];
                receivedDatagram = new DatagramPacket(message, message.length);
                socket = new DatagramSocket(null);
                socket.setReuseAddress(true);
                socket.bind(new InetSocketAddress(receive_port));
                while (keep_running) {
                    socket.receive(receivedDatagram);
                    parsePacket(message, receivedDatagram.getLength(), socket.getLocalAddress(), receivedDatagram.getAddress());
                    //NetworkInterface.getByInetAddress(socket.getLocalAddress()));
                }
                socket.close();
            } catch (final IOException e) {
                if (keep_running) { // no message if we were interrupt()ed
                    String msg = App.getAppString(R.string.error_listen_thread_exception, receive_port);
                    msg += e.getLocalizedMessage();
                    if (receive_port < 1024)
                        msg += App.getAppString(R.string.error_port_lt_1024);
                    InAppNotifications.FromOtherThread(App.instance, msg);
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
                                        InetAddress local, InetAddress peer);

    /**
     * @return Return the receive port of this thread
     */
    public int getPort() {
        return receive_port;
    }
}
