package oly.netpowerctrl.plugin_anel;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import oly.netpowerctrl.credentials.Credentials;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.data.query.ExecuteQueryUDPDevice;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.executables.ExecutableAndCommand;
import oly.netpowerctrl.ioconnection.IOConnectionUDP;
import oly.netpowerctrl.network.UDPSend;
import oly.netpowerctrl.network.onExecutionFinished;

/**
 * UDP related functions for the anel plugin
 */
public class AnelSendUDP {
    public static final String PLUGIN_ID = "org.anel.outlets_and_io";
    static final byte[] requestMessage = "wer da?\r\n".getBytes();

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
     * @param deviceUID The device unique id
     * @param id        From 1..8 (using the UDP numbering)
     * @return Return a unique id for an executable where {@link #extractIDFromExecutableUID(String)} can extract the id back.
     */
    public static String makeExecutableUID(String deviceUID, int id) {
        return deviceUID + "-" + String.valueOf(id);
    }

    public static int extractIDFromExecutableUID(String uid) {
        int i = uid.lastIndexOf('-');
        if (i == -1) throw new RuntimeException("Could not extract device port id from UID");
        return Integer.valueOf(uid.substring(i + 1));
    }

    static int executeBatchViaUDP(DataService dataService, List<ExecutableAndCommand> command_list, IOConnectionUDP udpioConnection) {

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
        Credentials credentials = udpioConnection.credentials;
        for (int id = 1; id <= 8; ++id) {
            Executable oi = dataService.executables.findByUID(makeExecutableUID(credentials.deviceUID, id));
            if (oi == null) {
                // For Anel devices with only 3 outputs we will run into this case
                //Log.e(getPluginID(), "executeDeviceBatch. Did not find " + makeExecutableUID(credentials.deviceUID, id) + " " + credentials.getDeviceName());
                continue;
            }
            if (oi.current_value == 0) // Only take "ON" commands into account for the bulk change byte
                continue;
            if (id >= 10 && id < 20) {
                data_io = switchOn(data_io, id - 10);
            } else if (id >= 0) {
                data_outlet = switchOn(data_outlet, id);
            }
        }

        // Second step: Apply commands
        for (ExecutableAndCommand c : command_list) {
            c.executable.setExecutionInProgress(true);

            int id = extractIDFromExecutableUID(c.executable.getUid());
            if (id >= 10 && id < 20) {
                containsIO = true;
            } else if (id >= 0) {
                containsOutlets = true;
            }
            switch (c.command) {
                case ExecutableAndCommand.OFF:
                    if (id >= 10 && id < 20) {
                        data_io = switchOff(data_io, id - 10);
                    } else if (id >= 0) {
                        data_outlet = switchOff(data_outlet, id);
                    }
                    break;
                case ExecutableAndCommand.ON:
                    if (id >= 10 && id < 20) {
                        data_io = switchOn(data_io, id - 10);
                    } else if (id >= 0) {
                        data_outlet = switchOn(data_outlet, id);
                    }
                    break;
                case ExecutableAndCommand.TOGGLE:
                    if (id >= 10 && id < 20) {
                        data_io = toggle(data_io, id - 10);
                    } else if (id >= 0) {
                        data_outlet = toggle(data_outlet, id);
                    }
                    break;
            }
        }

        // Third step: Send data
        String access = credentials.userName + credentials.password;
        byte[] data = new byte[3 + access.length()];
        System.arraycopy(access.getBytes(), 0, data, 3, access.length());

        if (containsOutlets) {
            data[0] = 'S';
            data[1] = 'w';
            data[2] = data_outlet;
            UDPSend.sendMessage(udpioConnection, data);
            UDPSend.sendMessage(udpioConnection, requestMessage);
        }
        if (containsIO) {
            data[0] = 'I';
            data[1] = 'O';
            data[2] = data_io;
            UDPSend.sendMessage(udpioConnection, data);
            UDPSend.sendMessage(udpioConnection, requestMessage);
        }

        return command_list.size();
    }

    static boolean executeViaUDP(IOConnectionUDP connectionUDP, @NonNull Executable executable, int value, final onExecutionFinished callback) {
        // NOOP, No operation = Send current value
        if (value == ExecutableAndCommand.TOGGLE) value = executable.getCurrentValueToggled();
        else if (value == ExecutableAndCommand.NOOP) value = executable.getCurrentValue();

        final Credentials credentials = connectionUDP.credentials;
        if (credentials == null) {
            Log.e(PLUGIN_ID, "execute. No credentials found!");
            if (callback != null) callback.addFail();
            return false;
        }

        int id = extractIDFromExecutableUID(executable.getUid());
        String valueString;
        if (id >= 10 && id < 20) {
            // IOS
            valueString = value > 0 ? "IO_on" : "IO_off";
            id -= 10;
        } else if (id >= 0) {
            // Outlets
            valueString = value > 0 ? "Sw_on" : "Sw_off";
        } else {
            if (callback != null) callback.addFail();
            return false;
        }

        byte[] data = String.format(Locale.US, "%s%d%s%s", valueString,
                id, credentials.userName, credentials.password).getBytes();
        DataService.getService().sendAndObserve(new ExecuteQueryUDPDevice(credentials, connectionUDP, data) {
            @Override
            public void finish() {
                UDPSend.sendMessage(ioConnectionUDP, requestMessage);
                if (callback != null) {
                    if (mIsSuccess)
                        callback.addSuccess();
                    else
                        callback.addFail();
                }
            }
        });
        return true;
    }

    static public void requestData(Set<Integer> ports) {
        UDPSend.sendBroadcast(ports, requestMessage);
    }

    static public void requestData(@NonNull IOConnectionUDP ioConnection) {
        UDPSend.sendMessage(ioConnection, requestMessage);
    }
}
