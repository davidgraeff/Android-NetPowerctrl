package oly.netpowerctrl.anel;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.application_state.NetpowerctrlService;
import oly.netpowerctrl.application_state.PluginInterface;
import oly.netpowerctrl.application_state.RuntimeDataController;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.DevicePort;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.network.DevicePortRenamed;
import oly.netpowerctrl.network.ExecutionFinished;
import oly.netpowerctrl.network.UDPSending;

/**
 * For executing a name on a DevicePort or commands for multiple DevicePorts (bulk).
 * This is a specialized class for Anel devices.
 */
final public class AnelPlugin implements PluginInterface {
    private final List<AnelDeviceDiscoveryThread> discoveryThreads = new ArrayList<AnelDeviceDiscoveryThread>();

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

        AnelHTTP.startHTTP();
    }

    public void stopDiscoveryThreads(NetpowerctrlService service) {
        RuntimeDataController d = NetpowerctrlApplication.getDataController();
        for (DeviceInfo di : d.deviceCollection.devices) {
            if (this.equals(di.getPluginInterface(service))) {
                di.setNotReachable("Energiesparmodus");
                d.onDeviceUpdated(di);
            }
        }

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

        AnelHTTP.stopHTTP();
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
        // Get necessary objects
        NetpowerctrlService service = NetpowerctrlApplication.getService();
        if (service == null)
            return;

        if (di.PreferTCP) {
            // Use Http instead of UDP for sending. For each batch command we will send a single http request
            for (Scene.PortAndCommand c : command_list) {
                DevicePort port = c.port;
                if (c.command != DevicePort.TOGGLE && port.current_value == c.command)
                    continue;
                c.port.last_command_timecode = System.currentTimeMillis();
                // Important: For UDP the id is 1-based. For Http the id is 0 based!
                AnelHTTP.execute(AnelHTTP.createHTTPRunner(di, "ctrl.htm",
                        "F" + String.valueOf(port.id - 1) + "=s", di, false, receiveSwitchResponseHtml));
            }
            return;
        }
        UDPSending udpSending = service.getUDPSending();
        if (udpSending == null)
            return;

        // build bulk change byte, see: www.anel-elektronik.de/forum_neu/viewtopic.php?f=16&t=207
        // “Sw” + Steckdosen + User + Passwort
        // Steckdosen = Zustand aller Steckdosen binär
        // LSB = Steckdose 1, MSB (Bit 8)= Steckdose 8 (PRO, POWER), Bit 2 = Steckdose 3 (HOME).
        // Soll nur 1 & 5 eingeschaltet werden=00010001 = 17 = 0x11 (HEX)
        byte data_outlet = 0;
        byte data_io = 0;
        boolean containsOutlets = false;
        boolean containsIO = false;
        int valid_commands = 0;

        // First step: Setup data byte (outlet, io) to reflect the current state of the device ports.
        di.lockDevicePorts();
        Iterator<DevicePort> it = di.getDevicePortIterator();
        while (it.hasNext()) {
            DevicePort oi = it.next();
            if (oi.Disabled || oi.current_value == 0)
                continue;
            int id = oi.id;
            if (id >= 10 && id < 20) {
                data_io = switchOn(data_io, id - 10);
                ++valid_commands;
            } else if (id >= 0) {
                data_outlet = switchOn(data_outlet, id);
                ++valid_commands;
            }
        }
        di.releaseDevicePorts();

        if (valid_commands == 0) {
            if (callback != null)
                callback.onExecutionFinished(command_list.size());
            return;
        }

        // Second step: Apply commands
        for (Scene.PortAndCommand c : command_list) {
            c.port.last_command_timecode = System.currentTimeMillis();

            int id = c.port.id;
            if (id >= 10 && id < 20) {
                containsIO = true;
            } else if (id >= 0) {
                containsOutlets = true;
            }
            switch (c.command) {
                case DevicePort.OFF:
                    if (id >= 10 && id < 20) {
                        data_io = switchOff(data_io, id - 10);
                    } else if (id >= 0) {
                        data_outlet = switchOff(data_outlet, id);
                    }
                    break;
                case DevicePort.ON:
                    if (id >= 10 && id < 20) {
                        data_io = switchOn(data_io, id - 10);
                    } else if (id >= 0) {
                        data_outlet = switchOn(data_outlet, id);
                    }
                    break;
                case DevicePort.TOGGLE:
                    if (id >= 10 && id < 20) {
                        data_io = toggle(data_io, id - 10);
                    } else if (id >= 0) {
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
            udpSending.addJob(new UDPSending.SendAndObserveJob(di, data, requestMessage, UDPSending.INQUERY_REQUEST));
        }
        if (containsIO) {
            data[0] = 'I';
            data[1] = 'O';
            data[2] = data_io;
            udpSending.addJob(new UDPSending.SendAndObserveJob(di, data, requestMessage, UDPSending.INQUERY_REQUEST));
        }

        if (callback != null)
            callback.onExecutionFinished(command_list.size());
    }

    private static final byte[] requestMessage = "wer da?\r\n".getBytes();

    /**
     * Switch a single outlet or io port
     *
     * @param port     Execute on this port
     * @param command  Execute this command
     * @param callback This callback will be called when the execution finished
     */
    public void execute(DevicePort port, int command, ExecutionFinished callback) {
        // Get necessary objects
        NetpowerctrlService service = NetpowerctrlApplication.getService();
        if (service == null)
            return;

        port.last_command_timecode = System.currentTimeMillis();

        boolean bValue = false;
        if (command == DevicePort.ON)
            bValue = true;
        else if (command == DevicePort.OFF)
            bValue = false;
        else if (command == DevicePort.TOGGLE)
            bValue = port.current_value <= 0;

        // Use Http instead of UDP for sending. For each command we will send a single http request
        if (port.device.PreferTCP) {
            // The http interface can only toggle. If the current state is the same as the command state
            // then we request values instead of sending a command.
            if (command != DevicePort.TOGGLE && port.current_value == command)
                AnelHTTP.execute(AnelHTTP.createHTTPRunner(port.device, "strg.cfg",
                        "", port.device, false, receiveCtrlHtml));
            else
                // Important: For UDP the id is 1-based. For Http the id is 0 based!
                AnelHTTP.execute(AnelHTTP.createHTTPRunner(port.device, "ctrl.htm",
                        "F" + String.valueOf(port.id - 1) + "=s", port.device, false, receiveSwitchResponseHtml));
        } else {
            UDPSending udpSending = service.getUDPSending();
            if (udpSending == null)
                return;

            byte[] data;
            UDPSending.Job j = null;
            if (port.id >= 10 && port.id < 20) {
                // IOS
                data = String.format(Locale.US, "%s%d%s%s", bValue ? "IO_on" : "IO_off",
                        port.id - 10, port.device.UserName, port.device.Password).getBytes();
                j = new UDPSending.SendAndObserveJob(port.device, data, requestMessage, UDPSending.INQUERY_REQUEST);
            } else if (port.id >= 0) {
                // Outlets
                data = String.format(Locale.US, "%s%d%s%s", bValue ? "Sw_on" : "Sw_off",
                        port.id, port.device.UserName, port.device.Password).getBytes();
                j = new UDPSending.SendAndObserveJob(port.device, data, requestMessage, UDPSending.INQUERY_REQUEST);
            }

            if (j != null)
                udpSending.addJob(j);
        }

        if (callback != null)
            callback.onExecutionFinished(1);
    }

    @Override
    public void finish() {

    }

    @Override
    public void requestData() {
        // Get necessary objects
        NetpowerctrlService service = NetpowerctrlApplication.getService();
        if (service == null)
            return;
        UDPSending udpSending = service.getUDPSending();
        if (udpSending == null)
            return;

        udpSending.addJob(new AnelBroadcastSendJob());
    }

    @Override
    public void requestData(DeviceInfo di) {
        // Get necessary objects
        NetpowerctrlService service = NetpowerctrlApplication.getService();
        if (service == null)
            return;

        if (di.PreferTCP) {
            AnelHTTP.execute(AnelHTTP.createHTTPRunner(di, "strg.cfg", "", di, false, receiveCtrlHtml));
        } else {
            UDPSending udpSending = service.getUDPSending();
            if (udpSending == null)
                return;

            udpSending.addJob(new UDPSending.SendAndObserveJob(di, requestMessage, UDPSending.INQUERY_REQUEST));
        }
    }

    /**
     * If we receive a response from a switch action (via http) we request updated data immediately.
     */
    private static AnelHTTP.HTTPCallback<DeviceInfo> receiveSwitchResponseHtml = new AnelHTTP.HTTPCallback<DeviceInfo>() {
        @Override
        public void httpResponse(DeviceInfo device, boolean callback_success, String callback_message) {
            if (!callback_success) {
                device.setNotReachable(callback_message);
                NetpowerctrlApplication.getDataController().onDeviceUpdatedOtherThread(device);
            } else
                AnelHTTP.execute(AnelHTTP.createHTTPRunner(device, "strg.cfg", "", device, false, receiveCtrlHtml));
        }
    };

    private static AnelHTTP.HTTPCallback<DeviceInfo> receiveCtrlHtml = new AnelHTTP.HTTPCallback<DeviceInfo>() {
        @Override
        public void httpResponse(DeviceInfo device, boolean callback_success, String callback_message) {
            if (!callback_success) {
                device.setNotReachable(callback_message);
                NetpowerctrlApplication.getDataController().onDeviceUpdatedOtherThread(device);
            } else {
                String[] data = callback_message.split(";");
                if (data.length < 10 || !data[0].startsWith("NET-")) {
                    device.setNotReachable(NetpowerctrlApplication.instance.getString(R.string.error_packet_received));
                } else {
                    // We do not copy the network data but only the name.
                    device.DeviceName = data[1].trim();
                    // DevicePorts data. Put that into a new map and use copyFreshDevicePorts method
                    // on the existing device.
                    Map<Integer, DevicePort> ports = new TreeMap<Integer, DevicePort>();
                    for (int i = 0; i < 8; ++i) {
                        DevicePort port = new DevicePort(device, DevicePort.DevicePortType.TypeToggle);
                        port.id = i + 1; // 1-based
                        port.setDescription(data[10 + i].trim());
                        port.current_value = data[20 + i].equals("1") ? DevicePort.ON : DevicePort.OFF;
                        port.Disabled = data[30 + i].equals("1");
                        ports.put(port.id, port);
                    }

                    // If values have changed, update now
                    if (device.copyFreshDevicePorts(ports)) {
                        // To propagate this device although it is already the the configured list
                        // we have to set the changed flag.
                        device.setHasChanged();
                        NetpowerctrlApplication.getDataController().onDeviceUpdatedOtherThread(device);
                    }
                }

            }
        }
    };

    @Override
    public void rename(final DevicePort port, final String new_name, final DevicePortRenamed callback) {
        final String new_name_encoded;
        try {
            new_name_encoded = URLEncoder.encode(new_name, "utf-8");
        } catch (UnsupportedEncodingException e) {
            return;
        }
        String postData = "TN=" + new_name_encoded + "&TS=Speichern";
        String getData = "dd.htm?DD" + String.valueOf(port.id);

        AnelHTTP.execute(AnelHTTP.createHTTPRunner(port.device, getData, postData,
                port, true, new AnelHTTP.HTTPCallback<DevicePort>() {
            @Override
            public void httpResponse(DevicePort port, boolean callback_success, String callback_error_message) {
                if (callback_success) {
                    port.setDescription(new_name_encoded);
                }
                callback.devicePort_renamed(port, callback_success, callback_error_message);
            }
                }
        ));
    }

    private final List<Scene.PortAndCommand> command_list = new ArrayList<Scene.PortAndCommand>();

    @Override
    public void addToTransaction(DevicePort port, int command) {
        command_list.add(new Scene.PortAndCommand(port, command));
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

        command_list.clear();
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

    @Override
    public void openConfigurationPage(DeviceInfo device, Context context) {
        Intent browse = new Intent(Intent.ACTION_VIEW,
                Uri.parse("http://" + device.HostName + ":" + Integer.valueOf(device.HttpPort).toString()));
        context.startActivity(browse);
    }

    @Override
    public boolean isNetworkPlugin() {
        return true;
    }

//
//    public boolean onlyLinkLocalDevices() {
//        boolean linkLocals = true;
//        for (DeviceInfo di : deviceCollection) {
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
