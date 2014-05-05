package oly.netpowerctrl.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.utils.ShowToast;

abstract public class UDPReceiving extends Thread {
    protected final int receive_port;
    protected boolean keep_running;
    protected DatagramSocket socket;
    protected DatagramPacket receivedDatagram;

    public UDPReceiving(int port) {
        receive_port = port;
        socket = null;
    }

    public void run() {
        keep_running = true;
        while (keep_running) {
            try {
                byte[] message = new byte[1500];
                receivedDatagram = new DatagramPacket(message, message.length);
                socket = new DatagramSocket(receive_port);
                socket.setReuseAddress(true);
                while (keep_running) {
                    socket.receive(receivedDatagram);
                    parsePacket(message, receivedDatagram.getLength(), receive_port);
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

    public abstract void parsePacket(final byte[] message, int length, int receive_port);

    /**
     * @return Return the receive port of this thread
     */
    public int getPort() {
        return receive_port;
    }
}
