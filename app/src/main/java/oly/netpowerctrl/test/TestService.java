package oly.netpowerctrl.test;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Ethersex ecmd service
 * Supported: pca9685, blind
 */
public class TestService extends Service {
    private final IBinder mBinder = new LocalBinder();
    private DatagramSocket s;

    @Override
    public void onCreate() {
        try {
            s = new DatagramSocket();
            s.setBroadcast(true);
        } catch (SocketException ignored) {
            s = null;
        }
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        if (s != null)
            s.close();
        super.onDestroy();
    }

    public void connect_to_host(String hostname, int port) throws UnknownHostException {
        InetAddress host = InetAddress.getByName(hostname);
        if (s != null)
            s.connect(host, port);
    }

    public void blind_close() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    byte[] message = "blindclose\n".getBytes();
                    s.send(new DatagramPacket(message, message.length));
                } catch (final Exception ignored) {
                }
            }
        }).start();
    }

    public void blind_open() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    byte[] message = "blindopen\n".getBytes();
                    s.send(new DatagramPacket(message, message.length));
                } catch (final Exception ignored) {
                }
            }
        }).start();
    }

    public void blind_stop() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    byte[] message = "blindstop\n".getBytes();
                    s.send(new DatagramPacket(message, message.length));
                } catch (final Exception ignored) {
                }
            }
        }).start();
    }

    public void changeLed(final int channel, final int value) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    String temp = "i2c_pca9685_set 64 " + Integer.valueOf(channel).toString() + " " +
                            Integer.valueOf(value).toString() + "\n";
                    byte[] message = temp.getBytes();
                    s.send(new DatagramPacket(message, message.length));
                } catch (final Exception ignored) {
                }
            }
        }).start();
    }


    ////////////// Service Related Stuff

    public class LocalBinder extends Binder {
        public TestService getService() {
            // Return this instance of LocalService so clients can call public methods
            return TestService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {

        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {

        stopSelf();
        return super.onUnbind(intent);
    }
}
