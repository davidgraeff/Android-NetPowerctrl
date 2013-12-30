package oly.netpowerctrl.network;

import android.content.Context;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Locale;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.OutletCommand;
import oly.netpowerctrl.datastructure.OutletCommandGroup;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.ShowToast;

public class UDPSendToDevice {

    /**
     * Convenience version of sendOutlet for the extra bundle of a shortcut
     * intent.
     *
     * @param context The context of the activity for showing toast messages and
     *                getResources
     * @param og      The OutletCommandGroup
     * @return Return true if all fields have been found
     */
    static public boolean sendOutlet(final Context context, final OutletCommandGroup og) {
        // Read configured devices, because we need the DeviceInfo objects that
        // matches
        // our stored mac addresses
        final ArrayList<DeviceInfo> alDevices = SharedPrefs.ReadConfiguredDevices(context);
        final boolean r = SharedPrefs.getNextToggleState(context);

        // udp sending in own thread
        new Thread(new Runnable() {
            public void run() {
                try {
                    DatagramSocket s = new DatagramSocket();
                    for (OutletCommand c : og.commands) {
                        // find DeviceInfo with matching mac address
                        DeviceInfo device = null;
                        for (DeviceInfo check : alDevices) {
                            if (check.MacAddress.equals(c.device_mac)) {
                                device = check;
                                break;
                            }
                        }
                        if (device != null) {
                            boolean sw_state = (c.state == 2) ? r : (c.state == 1);
                            String messageStr = String.format(Locale.US, "%s%d%s%s", sw_state ? "Sw_on" : "Sw_off",
                                    c.outletNumber, device.UserName, device.Password);
                            //Log.w("send",messageStr+" "+device.HostName+":"+Integer.valueOf(device.SendPort).toString());
                            InetAddress host = InetAddress.getByName(device.HostName);
                            s.send(new DatagramPacket(messageStr.getBytes(), messageStr.length(), host, device.SendPort));
                            // wait for 20ms trying not to congest the line
                            try {
                                Thread.sleep(20);
                            } catch (InterruptedException ignored) {
                            }
                        }
                    }
                    s.close();
                } catch (final IOException e) {
                    ShowToast.ShowToast(context, context.getResources().getString(R.string.error_sending_inquiry) + ": "
                            + e.getMessage());
                }
            }
        }).start();
        return true;
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
                    ShowToast.ShowToast(context, context.getResources().getString(R.string.error_sending_inquiry) + ": "
                            + e.getMessage());
                }
            }
        }).start();
    }

}
