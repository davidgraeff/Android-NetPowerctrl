package oly.netpowerctrl.anelservice;

import android.content.Context;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Locale;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.DeviceCommand;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.utils.ShowToast;

public class DeviceSend {
    /**
     * Bulk version of sendOutlet. Send changes for each device in only one packet per device.
     *
     * @param context         The context of the activity for showing toast messages and
     *                        getResources
     * @param device_commands Bulk command per device
     */
    static public void sendOutlet(final Context context, final Collection<DeviceCommand> device_commands, final boolean requestNewValuesAfterSend) {
        // udp sending in own thread
        new Thread(new Runnable() {
            public void run() {
                try {
                    DatagramSocket s = new DatagramSocket();

                    for (DeviceCommand c : device_commands) {
                        sendAllOutlets(c, s);
                    }
                    s.close();

                    if (requestNewValuesAfterSend) {
                        // wait 100ms
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ignored) {
                        }

                        // request new values from each device

                        for (DeviceCommand device_command : device_commands) {
                            DeviceQuery.sendQuery(context, device_command);
                        }
                    }

                } catch (final IOException e) {
                    ShowToast.FromOtherThread(context, context.getResources().getString(R.string.error_sending_inquiry) + ": "
                            + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * Bulk version of sendOutlet. Send changes for multiple outlets of a device in only one packet.
     *
     * @param context The context
     * @param device  The device and state
     */
    static public void sendAllOutlets(final Context context, final DeviceCommand device, final boolean requestNewValuesAfterSend) {
        // udp sending in own thread
        new Thread(new Runnable() {
            public void run() {
                try {
                    DatagramSocket s = new DatagramSocket();
                    sendAllOutlets(device, s);
                    s.close();

                    if (requestNewValuesAfterSend) {
                        // wait 100ms
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ignored) {
                        }

                        // request new values
                        DeviceQuery.sendQuery(context, device);
                    }
                } catch (final IOException e) {
                    ShowToast.FromOtherThread(context, context.getResources().getString(R.string.error_sending_inquiry) + ": "
                            + e.getMessage());
                }
            }
        }).start();
    }

    static private void sendAllOutlets(final DeviceCommand device, DatagramSocket s) throws IOException {
        if (device.dest != null) {
            byte[] data = new byte[3 + device.access.length()];
            data[0] = 'S';
            data[1] = 'w';
            data[2] = device.getSwitchByte();
            System.arraycopy(device.access.getBytes(), 0, data, 3, device.access.length());

            //Log.w("sendAllOutlets", String.valueOf( (char)(device.getSwitchByte()+'0') ));

            s.send(new DatagramPacket(data, data.length, device.dest, device.port));
            // wait for 20ms trying not to congest the line
            try {
                Thread.sleep(20);
            } catch (InterruptedException ignored) {
            }
        }
    }

    static public void sendOutlet(final Context context, final DeviceInfo device, final int OutletNumber,
                                  final boolean sw_state) {
        // udp sending in own socket
        new Thread(new Runnable() {
            public void run() {
                try {
                    String messageStr = String.format(Locale.US, "%s%d%s%s", sw_state ? "Sw_on" : "Sw_off",
                            OutletNumber, device.UserName, device.Password);
                    DatagramSocket s = new DatagramSocket();
                    InetAddress host = InetAddress.getByName(device.HostName);
                    int msg_length = messageStr.length();
                    byte[] message = messageStr.getBytes();
                    DatagramPacket p = new DatagramPacket(message, msg_length, host, device.SendPort);
                    s.send(p);
                    s.close();
                } catch (final IOException e) {
                    ShowToast.FromOtherThread(context, context.getResources().getString(R.string.error_sending_inquiry) + ": "
                            + e.getMessage());
                }
            }
        }).start();
    }

}
