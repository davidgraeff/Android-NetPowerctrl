package oly.netpowerctrl.anel;

import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.application_state.PluginInterface;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.DevicePort;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.network.DevicePortRenamed;
import oly.netpowerctrl.network.DeviceSend;
import oly.netpowerctrl.network.ExecutionFinished;

/**
 * For executing a name on a DevicePort or commands for multiple DevicePorts (bulk).
 * This is a specialized class for Anel devices.
 */
final public class AnelPlugin implements PluginInterface {
    private List<AnelDeviceDiscoveryThread> discoveryThreads = new ArrayList<AnelDeviceDiscoveryThread>();

    public void startDiscoveryThreads(int additional_port) {
        // Get all ports of configured devices and add the additional_port if != 0
        Set<Integer> ports = NetpowerctrlApplication.getDataController().getAllReceivePorts();
        if (additional_port != 0)
            ports.add(additional_port);

        boolean new_threads_started = false;
        List<AnelDeviceDiscoveryThread> unusedThreads = new ArrayList<AnelDeviceDiscoveryThread>(discoveryThreads);

        // Go through all ports and start a thread for it if none is running for it so far
        for (int port : ports) {
            boolean already_running = false;
            for (AnelDeviceDiscoveryThread running_thread : discoveryThreads) {
                if (running_thread.getPort() == port) {
                    already_running = true;
                    unusedThreads.remove(running_thread);
                    break;
                }
            }

            if (already_running) {
                continue;
            }

            new_threads_started = true;
            AnelDeviceDiscoveryThread thr = new AnelDeviceDiscoveryThread(this, port);
            thr.start();
            discoveryThreads.add(thr);
        }

        if (unusedThreads.size() > 0) {
            for (AnelDeviceDiscoveryThread thr : unusedThreads) {
                thr.interrupt();
                discoveryThreads.remove(thr);
            }
        }

        if (new_threads_started) {
            // give the threads a chance to start
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public void stopDiscoveryThreads() {
        if (discoveryThreads.size() == 0)
            return;

        for (AnelDeviceDiscoveryThread thr : discoveryThreads)
            thr.interrupt();
        discoveryThreads.clear();
        // socket needs minimal time to really go away
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
    }

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

    private static void executeDeviceBatch(DeviceInfo di, List<Scene.PortAndCommand> command_list, ExecutionFinished callback) {
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
        for (Scene.PortAndCommand c : command_list) {
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
     * @param port     Execute on this port
     * @param command  Execute this command
     * @param callback This callback will be called when the execution finished
     */
    public void execute(DevicePort port, int command, ExecutionFinished callback) {
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

    static Thread renameThread;

    @Override
    public void finish() {

    }

    @Override
    public void requestData() {
        DeviceSend.instance().addJob(new AnelBroadcastSendJob());
    }

    @Override
    public void requestData(DeviceInfo di) {
        DeviceSend.instance().addJob(new DeviceSend.SendJob(di, "wer da?\r\n".getBytes(), DeviceSend.INQUERY_REQUEST, true));
    }

    @Override
    public void rename(final DevicePort port, final String new_name, final DevicePortRenamed callback) {
        if (renameThread != null)
            return;

        renameThread = new Thread(new Runnable() {
            @Override
            public void run() {
                URL url;
                boolean success = false;
                String error_message = null;
                try {
                    String new_name_encoded = URLEncoder.encode(new_name, "utf-8");
                    String cred = port.device.UserName + ":" + port.device.Password;
                    String command = "TN=" + new_name_encoded + "&TS=Speichern";
                    url = new URL("http://" + port.device.HostName + ":" + port.device.HttpPort + "/dd.htm?DD" + String.valueOf(port.id));
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setConnectTimeout(500);
                    con.setRequestMethod("POST");
                    con.setRequestProperty("Authorization", "Basic " +
                            Base64.encodeToString(cred.getBytes(), Base64.URL_SAFE | Base64.NO_WRAP));
                    con.getOutputStream().write(command.getBytes());
                    con.getOutputStream().flush();
                    switch (con.getResponseCode()) {
                        case 200:
                            success = true;
                            port.setDescription(new_name_encoded);
                            break;
                        case 401:
                            success = false;
                            error_message = NetpowerctrlApplication.instance.getString(R.string.error_device_no_access);
                            break;
                        default:
                            error_message = "code " + String.valueOf(con.getResponseCode());
                            success = false;
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    error_message = e.getMessage();
                } catch (ProtocolException e) {
                    e.printStackTrace();
                    error_message = e.getMessage();
                } catch (IOException e) {
                    e.printStackTrace();
                    error_message = e.getMessage();
                }
                renameThread = null;
                if (callback != null) {
                    final boolean callback_success = success;
                    final String callback_error_message = error_message;
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            callback.devicePort_renamed(port, callback_success, callback_error_message);
                        }
                    });
                }
            }
        });
        renameThread.start();
    }

    private List<Scene.PortAndCommand> command_list = new ArrayList<Scene.PortAndCommand>();

    @Override
    public void addToTransaction(DevicePort port, int command) {
        command_list.add(new Scene.PortAndCommand());
    }

    @Override
    public void executeTransaction(ExecutionFinished callback) {
        TreeMap<DeviceInfo, List<Scene.PortAndCommand>> commands_grouped_by_devices =
                new TreeMap<DeviceInfo, List<Scene.PortAndCommand>>();

        // add to tree
        for (Scene.PortAndCommand portAndCommand : command_list) {
            if (!commands_grouped_by_devices.containsKey(portAndCommand.port.device)) {
                commands_grouped_by_devices.put(portAndCommand.port.device, new ArrayList<Scene.PortAndCommand>());
            }
            commands_grouped_by_devices.get(portAndCommand.port.device).add(portAndCommand);
        }

        // execute by device
        for (TreeMap.Entry<DeviceInfo, List<Scene.PortAndCommand>> entry : commands_grouped_by_devices.entrySet()) {
            executeDeviceBatch(entry.getKey(), entry.getValue(), callback);
        }
    }

    public static final String PLUGIN_ID = "org.anel.outlets_and_io";

    @Override
    public String getPluginID() {
        return PLUGIN_ID;
    }

    @Override
    public void prepareForDevices(DeviceInfo device) {
        startDiscoveryThreads((device != null) ? device.ReceivePort : 0);
    }

//
//    public boolean onlyLinkLocalDevices() {
//        boolean linkLocals = true;
//        for (DeviceInfo di : configuredDevices) {
//            if (di.pluginID != DeviceInfo.DeviceType.AnelDevice)
//                continue;
//
//            try {
//                InetAddress address = InetAddress.getByName(di.HostName);
//                linkLocals &= (address.isLinkLocalAddress() || address.isSiteLocalAddress());
//            } catch (UnknownHostException e) {
//                // we couldn't resolve the device hostname to an IP address. One reason is, that
//                // the user entered a dns name instead of an IP (and the dns server is not reachable
//                // at the moment). Therefore we assume that there not only link local addresses.
//                return false;
//            }
//        }
//        return linkLocals;
//    }

}
