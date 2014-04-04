package oly.netpowerctrl.anel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.DevicePort;
import oly.netpowerctrl.datastructure.ExecutionFinished;
import oly.netpowerctrl.datastructure.Executor;
import oly.netpowerctrl.network.DeviceSend;

/**
 * For executing a name on a DevicePort or commands for multiple DevicePorts (bulk).
 * This is a specialized class for Anel devices.
 */
final public class AnelExecutor {

    private static byte switchOn(byte data, int outletNumber) {
        data |= ((byte) (1 << outletNumber - 1));
        return data;
    }

    private static byte switchOff(byte data, int outletNumber) {
        data &= ~((byte) (1 << outletNumber - 1));
        return data;
    }

    private static boolean getIsOn(byte data, int outletNumber) {
        return ((data & ((byte) (1 << (outletNumber - 1)))) != 0);
    }

    private static byte toggle(byte data, int outletNumber) {
        if (getIsOn(data, outletNumber)) {
            return switchOff(data, outletNumber);
        } else {
            return switchOn(data, outletNumber);
        }
    }

    /**
     * @param command_list
     */
    public static void execute(List<Executor.PortAndCommand> command_list, ExecutionFinished callback) {
        TreeMap<DeviceInfo, List<Executor.PortAndCommand>> commands_grouped_by_devices =
                new TreeMap<DeviceInfo, List<Executor.PortAndCommand>>();

        // add to tree
        for (Executor.PortAndCommand portAndCommand : command_list) {
            if (portAndCommand.port.device.deviceType != DeviceInfo.DeviceType.AnelDevice)
                continue;

            if (!commands_grouped_by_devices.containsKey(portAndCommand.port.device)) {
                commands_grouped_by_devices.put(portAndCommand.port.device, new ArrayList<Executor.PortAndCommand>());
            }
            commands_grouped_by_devices.get(portAndCommand.port.device).add(portAndCommand);
        }

        // execute by device
        for (TreeMap.Entry<DeviceInfo, List<Executor.PortAndCommand>> entry : commands_grouped_by_devices.entrySet()) {
            execute(entry.getKey(), entry.getValue(), callback);
        }

    }

    public static void execute(DeviceInfo di, List<Executor.PortAndCommand> command_list, ExecutionFinished callback) {
        // build bulk change byte, see: www.anel-elektronik.de/forum_neu/viewtopic.php?f=16&t=207
        // “Sw” + Steckdosen + User + Passwort
        // Steckdosen = Zustand aller Steckdosen binär
        // LSB = Steckdose 1, MSB (Bit 8)= Steckdose 8 (PRO, POWER), Bit 2 = Steckdose 3 (HOME).
        // Soll nur 1 & 5 eingeschaltet werden=00010001 = 17 = 0x11 (HEX)
        byte data_outlet = 0;
        byte data_io = 0;
        boolean containsOutlets = false;
        boolean containsIO = false;

        // First step: Setup data byte (outlet, io) to reflect the current state of the device ports.
        for (int i = 0; i < di.DevicePorts.size(); ++i) {
            if (di.DevicePorts.get(i).Disabled || di.DevicePorts.get(i).current_value == 0)
                continue;

            int id = (int) di.DevicePorts.get(i).id;
            if (id >= 10) {
                data_io = switchOn(data_io, id - 10);
            } else {
                data_outlet = switchOn(data_outlet, id);
            }
        }

        // Second step: Apply commands
        for (Executor.PortAndCommand c : command_list) {
            c.port.last_command_timecode = System.currentTimeMillis();

            int id = (int) c.port.id;
            if (id >= 10) {
                containsIO = true;
            } else {
                containsOutlets = true;
            }
            switch (c.command) {
                case DevicePort.OFF:
                    if (id >= 10) {
                        data_io = switchOff(data_io, id - 10);
                    } else {
                        data_outlet = switchOff(data_outlet, id);
                    }
                    break;
                case DevicePort.ON:
                    if (id >= 10) {
                        data_io = switchOn(data_io, id - 10);
                    } else {
                        data_outlet = switchOn(data_outlet, id);
                    }
                    break;
                case DevicePort.TOGGLE:
                    if (id >= 10) {
                        data_io = toggle(data_io, id - 10);
                    } else {
                        data_outlet = toggle(data_outlet, id);
                    }
                    break;
            }
        }

        // Thirst step: Send data
        String access = di.UserName + di.Password;
        byte[] data = new byte[3 + access.length()];
        System.arraycopy(access.getBytes(), 0, data, 3, access.length());

        if (containsOutlets) {
            data[0] = 'S';
            data[1] = 'w';
            data[2] = data_outlet;
            DeviceSend.instance().addJob(new DeviceSend.SendJob(di, data, DeviceSend.INQUERY_REQUEST, true));
        }
        if (containsIO) {
            data[0] = 'I';
            data[1] = 'O';
            data[2] = data_io;
            DeviceSend.instance().addJob(new DeviceSend.SendJob(di, data, DeviceSend.INQUERY_REQUEST, true));
        }

        if (callback != null)
            callback.onExecutionFinished(command_list.size());
    }

    /**
     * Switch a single outlet or io port
     *
     * @param port
     * @param command
     */
    public static void execute(DevicePort port, int command, ExecutionFinished callback) {
        port.last_command_timecode = System.currentTimeMillis();
        boolean bValue = false;
        if (command == DevicePort.ON)
            bValue = true;
        else if (command == DevicePort.OFF)
            bValue = false;
        else if (command == DevicePort.TOGGLE)
            bValue = port.current_value <= 0;

        byte[] data;
        if (port.id >= 10) {
            // IOS
            data = String.format(Locale.US, "%s%d%s%s", bValue ? "IO_on" : "IO_off",
                    port.id - 10, port.device.UserName, port.device.Password).getBytes();
        } else {
            // Outlets
            data = String.format(Locale.US, "%s%d%s%s", bValue ? "Sw_on" : "Sw_off",
                    port.id, port.device.UserName, port.device.Password).getBytes();
        }

        DeviceSend.instance().addJob(new DeviceSend.SendJob(port.device, data, DeviceSend.INQUERY_REQUEST, true));

        if (callback != null)
            callback.onExecutionFinished(1);
    }

    public static void sendBroadcastQuery() {
        DeviceSend.instance().addJob(new AnelBroadcastSendJob());
    }

    public static void sendQuery(DeviceInfo di) {
        DeviceSend.instance().addJob(new DeviceSend.SendJob(di, "wer da?\r\n".getBytes(), DeviceSend.INQUERY_REQUEST, true));
    }
}
