package oly.netpowerctrl.datastructure;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.TreeMap;

/**
 * This class extracts the destination ip and user access data from a DeviceInfo
 * and provides outlet manipulation methods (on/off/toggle).
 * The resulting data byte variable can be used to send a
 * bulk-change udp packet.
 */
final public class DeviceCommand {
    // build bulk change byte, see: www.anel-elektronik.de/forum_neu/viewtopic.php?f=16&t=207
    // “Sw” + Steckdosen + User + Passwort
    // Steckdosen = Zustand aller Steckdosen binär
    // LSB = Steckdose 1, MSB (Bit 8)= Steckdose 8 (PRO, POWER), Bit 2 = Steckdose 3 (HOME).
    // Soll nur 1 & 5 eingeschaltet werden=00010001 = 17 = 0x11 (HEX)
    private byte data;
    public InetAddress dest;
    public int port;
    public String access;
    public DeviceInfo device;

    public DeviceCommand(DeviceInfo di) {
        this.data = 0;
        this.device = di;
        this.port = di.SendPort;
        try {
            this.dest = InetAddress.getByName(di.HostName);
        } catch (UnknownHostException e) {
            this.dest = null;
        }
        this.access = di.UserName + di.Password;
        for (int i = 0; i < di.Outlets.size(); ++i) {
            //Log.w("DeviceSwitch", Integer.valueOf(i).toString() + " " + di.DeviceName + " " + Integer.valueOf(di.Outlets.get(i).OutletNumber).toString() + " " + Boolean.valueOf(di.Outlets.get(i).State).toString());

            if (!di.Outlets.get(i).Disabled && di.Outlets.get(i).State)
                switchOn(di.Outlets.get(i).OutletNumber);
        }

    }

    public boolean getIsOn(int outletNumber) {
        return ((data & ((byte) (1 << outletNumber - 1))) > 0);
    }

    private void switchOn(int outletNumber) {
        data |= ((byte) (1 << outletNumber - 1));
    }

    private void switchOff(int outletNumber) {
        data &= ~((byte) (1 << outletNumber - 1));
    }

    private void toggle(int outletNumber) {
        if ((data & ((byte) (1 << outletNumber - 1))) > 0) {
            switchOff(outletNumber);
        } else {
            switchOn(outletNumber);
        }
    }

    public byte getSwitchByte() {
        return data;
    }

    static public Collection<DeviceCommand> fromOutletCommandGroup(OutletCommandGroup og) {
        TreeMap<String, DeviceCommand> deviceCommands = new TreeMap<String, DeviceCommand>();
        for (OutletCommand c : og.commands) {
            if (!deviceCommands.containsKey(c.device_mac)) {
                deviceCommands.put(c.device_mac, new DeviceCommand(c.outletinfo.device));
            }

            //Log.w("sendOutlet",c.device_mac+" "+ Integer.valueOf(c.outletNumber).toString()+" "+Integer.valueOf(c.state).toString());

            switch (c.state) {
                case 0:
                    deviceCommands.get(c.device_mac).switchOff(c.outletNumber);
                    break;
                case 1:
                    deviceCommands.get(c.device_mac).switchOn(c.outletNumber);
                    break;
                case 2:
                    deviceCommands.get(c.device_mac).toggle(c.outletNumber);
                    break;
            }

        }
        return deviceCommands.values();
    }
}
