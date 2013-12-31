package oly.netpowerctrl.anelservice;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.OutletCommand;
import oly.netpowerctrl.datastructure.OutletCommandGroup;
import oly.netpowerctrl.utils.ShowToast;

public class DeviceSend {
    /**
     * This class extracts the destination ip, user access data
     * and provides outlet manipulation methods (on/off/toggle).
     * The resulting data byte variable can be used to send a
     * bulk-change udp packet.
     */
    static final public class DeviceSwitch {
        // build bulk change byte, see: www.anel-elektronik.de/forum_neu/viewtopic.php?f=16&t=207
        // “Sw” + Steckdosen + User + Passwort
        // Steckdosen = Zustand aller Steckdosen binär
        // LSB = Steckdose 1, MSB (Bit 8)= Steckdose 8 (PRO, POWER), Bit 2 = Steckdose 3 (HOME).
        // Soll nur 1 & 5 eingeschaltet werden=00010001 = 17 = 0x11 (HEX)
        private byte data;
        InetAddress dest;
        int port;
        String access;

        public DeviceSwitch(DeviceInfo di) {
            this.data = 0;
            this.port = di.SendPort;
            try {
                this.dest = InetAddress.getByName(di.HostName);
            } catch (UnknownHostException e) {
                this.dest = null;
            }
            this.access = di.UserName + di.Password;
            for (int i = 0; i < di.Outlets.size(); ++i) {
                Log.w("DeviceSwitch", Integer.valueOf(i).toString() + " " + di.DeviceName + " " + Integer.valueOf(di.Outlets.get(i).OutletNumber).toString() + " " + Boolean.valueOf(di.Outlets.get(i).State).toString());

                if (!di.Outlets.get(i).Disabled && di.Outlets.get(i).State)
                    switchOn(di.Outlets.get(i).OutletNumber);
            }

        }

        void switchOn(int outletNumber) {
            data |= ((byte) (1 << outletNumber - 1));
        }

        void switchOff(int outletNumber) {
            data &= ~((byte) (1 << outletNumber - 1));
        }

        void toggle(int outletNumber) {
            if ((data & ((byte) (1 << outletNumber - 1))) > 0) {
                switchOff(outletNumber);
            } else {
                switchOn(outletNumber);
            }
        }

        byte getSwitchByte() {
            return data;
        }
    }

    /**
     * Bulk version of sendOutlet. Send changes for each device in only one packet per device.
     *
     * @param context The context of the activity for showing toast messages and
     *                getResources
     * @param og      The OutletCommandGroup
     * @return Return true if all fields have been found
     */
    static public void sendOutlet(final Context context, final OutletCommandGroup og) {
        // udp sending in own thread
        new Thread(new Runnable() {
            public void run() {
                try {
                    DatagramSocket s = new DatagramSocket();

                    TreeMap<String, DeviceSwitch> devices = new TreeMap<String, DeviceSwitch>();
                    for (OutletCommand c : og.commands) {
                        if (!devices.containsKey(c.device_mac)) {
                            devices.put(c.device_mac, new DeviceSwitch(c.outletinfo.device));
                        }

                        //Log.w("sendOutlet",c.device_mac+" "+ Integer.valueOf(c.outletNumber).toString()+" "+Integer.valueOf(c.state).toString());

                        switch (c.state) {
                            case 0:
                                devices.get(c.device_mac).switchOn(c.outletNumber);
                                break;
                            case 1:
                                devices.get(c.device_mac).switchOff(c.outletNumber);
                                break;
                            case 2:
                                devices.get(c.device_mac).toggle(c.outletNumber);
                                break;
                        }

                    }

                    for (Map.Entry<String, DeviceSwitch> c : devices.entrySet()) {
                        sendAllOutlets(c.getValue(), s);
                    }
                    s.close();

                    // wait 100ms
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {
                    }

                    // request new values from each device
                    for (Map.Entry<String, DeviceSwitch> device : devices.entrySet()) {
                        DeviceQuery.sendQuery(context, device.getValue().dest.getHostAddress(), device.getValue().port);
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
    static public void sendAllOutlets(final Context context, final DeviceSwitch device) {
        // udp sending in own thread
        new Thread(new Runnable() {
            public void run() {
                try {
                    DatagramSocket s = new DatagramSocket();
                    sendAllOutlets(device, s);
                    s.close();
                } catch (final IOException e) {
                    ShowToast.FromOtherThread(context, context.getResources().getString(R.string.error_sending_inquiry) + ": "
                            + e.getMessage());
                }
            }
        }).start();
    }

    static private void sendAllOutlets(final DeviceSwitch device, DatagramSocket s) throws IOException {
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
