package oly.netpowerctrl.anel;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.application_state.NetpowerctrlService;
import oly.netpowerctrl.application_state.PluginInterface;
import oly.netpowerctrl.application_state.RuntimeDataController;
import oly.netpowerctrl.device_ports.DevicePort;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.devices.DeviceConnection;
import oly.netpowerctrl.devices.DeviceConnectionHTTP;
import oly.netpowerctrl.devices.DeviceConnectionUDP;
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.network.AsyncRunnerResult;
import oly.netpowerctrl.network.ExecutionFinished;
import oly.netpowerctrl.network.HttpThreadPool;
import oly.netpowerctrl.network.UDPSending;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.scenes.Scene;
import oly.netpowerctrl.timer.Timer;
import oly.netpowerctrl.timer.TimerController;
import oly.netpowerctrl.utils.Logging;
import oly.netpowerctrl.utils_gui.ShowToast;

/**
 * For executing a name on a DevicePort or commands for multiple DevicePorts (bulk).
 * This is a specialized class for Anel devices.
 */
final public class AnelPlugin implements PluginInterface {
    public static final String PLUGIN_ID = "org.anel.outlets_and_io";
    private static final byte[] requestMessage = "wer da?\r\n".getBytes();
    private final List<AnelUDPDeviceDiscoveryThread> discoveryThreads = new ArrayList<>();
    private final List<Scene.PortAndCommand> command_list = new ArrayList<>();
    AnelCreateDevice anelCreateDevice;
    private UDPSending udpSending;

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

    private void executeDeviceBatch(Device device, List<Scene.PortAndCommand> command_list,
                                    ExecutionFinished callback) {
        // Get necessary objects
        NetpowerctrlService service = NetpowerctrlService.getService();
        if (service == null)
            return;

        DeviceConnection ci = device.getFirstReachableConnection();
        if (ci == null)
            return;

        if (ci instanceof DeviceConnectionHTTP) { // http
            // Use Http instead of UDP for sending. For each batch command we will send a single http request
            for (Scene.PortAndCommand c : command_list) {
                DevicePort port = c.port;
                if (c.command != DevicePort.TOGGLE && port.current_value == c.command)
                    continue;
                c.port.last_command_timecode = System.currentTimeMillis();
                // Important: For UDP the id is 1-based. For Http the id is 0 based!
                HttpThreadPool.execute(HttpThreadPool.createHTTPRunner((DeviceConnectionHTTP) ci, "ctrl.htm",
                        "F" + String.valueOf(port.id - 1) + "=s", (DeviceConnectionHTTP) ci, false, AnelPluginHttp.receiveSwitchResponseHtml));
            }
        } else if (!(ci instanceof DeviceConnectionUDP)) { // not udp: return: unknown protocol
            return;
        }

        if (warnUDPSending()) {
            return;
        }

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
        device.lockDevicePorts();
        Iterator<DevicePort> it = device.getDevicePortIterator();
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
        device.releaseDevicePorts();

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

        // Third step: Send data
        String access = device.UserName + device.Password;
        byte[] data = new byte[3 + access.length()];
        System.arraycopy(access.getBytes(), 0, data, 3, access.length());

        if (containsOutlets) {
            data[0] = 'S';
            data[1] = 'w';
            data[2] = data_outlet;
            udpSending.addJob(new UDPSending.SendAndObserveJob(ci, data,
                    requestMessage, UDPSending.INQUERY_REQUEST));
        }
        if (containsIO) {
            data[0] = 'I';
            data[1] = 'O';
            data[2] = data_io;
            udpSending.addJob(new UDPSending.SendAndObserveJob(ci, data,
                    requestMessage, UDPSending.INQUERY_REQUEST));
        }

        if (callback != null)
            callback.onExecutionFinished(command_list.size());
    }

    public void startUDPDiscoveryThreads(Set<Integer> additional_port) {
        // Get all ports of configured devices and add the additional_port if != 0
        Set<Integer> ports = NetpowerctrlApplication.getDataController().getAllReceivePorts();
        if (additional_port != null)
            ports.addAll(additional_port);

        boolean new_threads_started = false;
        List<AnelUDPDeviceDiscoveryThread> unusedThreads = new ArrayList<>(discoveryThreads);

        // Go through all ports and start a thread for it if none is running for it so far
        for (int port : ports) {
            boolean already_running = false;
            for (AnelUDPDeviceDiscoveryThread running_thread : discoveryThreads) {
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
            AnelUDPDeviceDiscoveryThread thr = new AnelUDPDeviceDiscoveryThread(this, port);
            thr.start();
            discoveryThreads.add(thr);
        }

        if (unusedThreads.size() > 0) {
            for (AnelUDPDeviceDiscoveryThread thr : unusedThreads) {
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

        HttpThreadPool.startHTTP();
    }

    public void stopUDPDiscoveryThreads() {
        Context context = NetpowerctrlApplication.instance;
        RuntimeDataController d = NetpowerctrlApplication.getDataController();
        for (Device di : d.deviceCollection.devices) {
            if (this.equals(di.getPluginInterface())) {
                di.setNotReachable("UDP", context.getString(R.string.device_energysave_mode));
                d.onDeviceUpdated(di);
            }
        }

        if (discoveryThreads.size() == 0)
            return;

        for (AnelUDPDeviceDiscoveryThread thr : discoveryThreads)
            thr.interrupt();
        discoveryThreads.clear();
        // socket needs minimal time to really go away
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }

        HttpThreadPool.stopHTTP();
    }

    /**
     * Switch a single outlet or io port
     *
     * @param port     Execute on this port
     * @param command  Execute this command
     * @param callback This callback will be called when the execution finished
     */
    public void execute(DevicePort port, int command, ExecutionFinished callback) {
        // Get necessary objects
        NetpowerctrlService service = NetpowerctrlService.getService();
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

        final Device device = port.device;
        final DeviceConnection ci = device.getFirstReachableConnection();

        // Use Http instead of UDP for sending. For each command we will send a single http request
        if (ci instanceof DeviceConnectionHTTP) {
            if (ci.getDestinationPort() < 0) {
                Toast.makeText(service, R.string.error_device_no_network_connections, Toast.LENGTH_SHORT).show();
                return;
            }
            // The http interface can only toggle. If the current state is the same as the command state
            // then we request values instead of sending a command.
            if (command == DevicePort.TOGGLE && port.current_value == command)
                HttpThreadPool.execute(HttpThreadPool.createHTTPRunner((DeviceConnectionHTTP) ci, "strg.cfg",
                        "", ci, false, AnelPluginHttp.receiveCtrlHtml));
            else
                // Important: For UDP the id is 1-based. For Http the id is 0 based!
                HttpThreadPool.execute(HttpThreadPool.createHTTPRunner((DeviceConnectionHTTP) ci, "ctrl.htm",
                        "F" + String.valueOf(port.id - 1) + "=s", (DeviceConnectionHTTP) ci, false, AnelPluginHttp.receiveSwitchResponseHtml));
        } else if (ci instanceof DeviceConnectionUDP) {
            if (warnUDPSending()) {
                return;
            }

            byte[] data;
            UDPSending.Job j = null;
            if (port.id >= 10 && port.id < 20) {
                // IOS
                data = String.format(Locale.US, "%s%d%s%s", bValue ? "IO_on" : "IO_off",
                        port.id - 10, device.UserName, device.Password).getBytes();
                j = new UDPSending.SendAndObserveJob(ci, data, requestMessage, UDPSending.INQUERY_REQUEST);
            } else if (port.id >= 0) {
                // Outlets
                data = String.format(Locale.US, "%s%d%s%s", bValue ? "Sw_on" : "Sw_off",
                        port.id, device.UserName, device.Password).getBytes();
                j = new UDPSending.SendAndObserveJob(ci, data, requestMessage, UDPSending.INQUERY_REQUEST);
            }

            if (j != null)
                udpSending.addJob(j);
        } else {
            Log.e("Anel", "execute. No reachable DeviceConnection found!");
        }

        if (callback != null)
            callback.onExecutionFinished(1);
    }

    /**
     * @return Return true if no udp sending thread is running
     */
    private boolean warnUDPSending() {
        if (udpSending == null) {
            ShowToast.FromOtherThread(NetpowerctrlApplication.instance, "udpSending null");
            return true;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        stopUDPDiscoveryThreads();
    }

    @Override
    public void onStart(NetpowerctrlService service) {

    }

    @Override
    public void requestData() {
        // Get necessary objects
        NetpowerctrlService service = NetpowerctrlService.getService();
        if (service == null)
            return;
        if (warnUDPSending()) {
            return;
        }

        udpSending.addJob(new AnelBroadcastSendJob());
    }

    @Override
    public void requestData(DeviceConnection ci) {
        // Get necessary objects
        NetpowerctrlService service = NetpowerctrlService.getService();
        if (service == null)
            return;

        final Device device = ci.getDevice();
        if (!device.isEnabled())
            return;

        if (ci instanceof DeviceConnectionHTTP) {
            HttpThreadPool.execute(HttpThreadPool.createHTTPRunner((DeviceConnectionHTTP) ci,
                    "strg.cfg", "", ci, false, AnelPluginHttp.receiveCtrlHtml));
        } else {
            if (warnUDPSending()) {
                return;
            }
            udpSending.addJob(new UDPSending.SendAndObserveJob(ci, requestMessage, UDPSending.INQUERY_REQUEST));
        }
    }

    @Override
    public void requestAlarms(final DevicePort port, final TimerController timerController) {
        final String getData = "dd.htm?DD" + String.valueOf(port.id);
        final DeviceConnectionHTTP ci = (DeviceConnectionHTTP) port.device.getFirstReachableConnection("HTTP");
        if (ci == null)
            return;

        HttpThreadPool.execute(HttpThreadPool.createHTTPRunner(ci, getData, null,
                port, false, new HttpThreadPool.HTTPCallback<DevicePort>() {
                    @Override
                    public void httpResponse(DevicePort port, boolean callback_success, String response_message) {
                        if (!callback_success) {
                            return;
                        }
                        try {
                            timerController.alarmsFromPlugin(extractAlarms(port, response_message));
                        } catch (SAXException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
        ));
    }

    public void configureDeviceScreenClose() {
        anelCreateDevice = null;
    }

    @Override
    public void showConfigureDeviceScreen(Device device) {
        anelCreateDevice = new AnelCreateDevice(device);
        MainActivity.getNavigationController().changeToDialog(MainActivity.instance, AnelDevicePreferences.class.getName());
    }

    private List<Timer> extractAlarms(final DevicePort port, final String html) throws SAXException, IOException {
        final List<Timer> l = new ArrayList<>();
        l.add(new Timer());
        l.add(new Timer());
        l.add(new Timer());
        l.add(new Timer());
        l.add(new Timer());

        XMLReader parser = XMLReaderFactory.createXMLReader("org.ccil.cowan.tagsoup.Parser");
        org.xml.sax.ContentHandler handler = new DefaultHandler() {
            @SuppressWarnings("deprecation")
            @Override
            public void startElement(java.lang.String uri,
                                     java.lang.String localName,
                                     java.lang.String qName,
                                     org.xml.sax.Attributes attributes) throws org.xml.sax.SAXException {
                if (!qName.equals("input"))
                    return;

                // We parse this: <input ... name="T24" value="00:00">
                // Where T24 mean: Timer 5 (0 based counting) and 2 is the start time. T34 the stop time.
                // T14 refers to the activated weekdays identified as numbers: "1234567" means all days.
                // 1 is Sunday. 2 is Monday...
                String name = attributes.getValue("name");
                String value = attributes.getValue("value");
                String checked = attributes.getValue("checked");

                if (name == null || name.length() < 2)
                    return;

                byte[] nameBytes = name.getBytes();

                if (nameBytes[0] != 'T')
                    return;

                int dataIndex = name.charAt(1) - '0';
                int timerNumber = name.length() == 2 ? 4 : name.charAt(2) - '0';

                if (dataIndex < 0 || dataIndex > 4 || timerNumber < 0 || timerNumber >= l.size())
                    return;

                Timer timer = l.get(timerNumber);


                switch (dataIndex) {
                    case 0: { // enabled / disabled
                        timer.deviceAlarm = true;
                        timer.port = port;
                        timer.port_id = port.uuid;
                        timer.enabled = checked != null;
                        timer.id = (port.id & 255) | timerNumber << 8;
                        timer.type = timerNumber < 4 ? Timer.TYPE_RANGE_ON_WEEKDAYS : Timer.TYPE_RANGE_ON_RANDOM_WEEKDAYS;
                        break;
                    }
                    case 1: { // weekdays
                        for (int i = 0; i < value.length(); ++i) {
                            int weekday_index = (value.charAt(i) - '1');
                            timer.weekdays[weekday_index % 7] = true;
                        }
                        break;
                    }
                    case 2: { // start time like 00:01
                        String[] e = value.split(":");
                        if (e.length != 2) {
                            Log.e(PLUGIN_ID, "alarm:parse:start_time failed " + value);
                            return;
                        }
                        timer.hour_minute_start = Integer.valueOf(e[0]) * 60 + Integer.valueOf(e[1]);
                        if (timer.hour_minute_start == 99 * 60 + 99) // disabled if time is 99:99
                            timer.hour_minute_start = -1;
                        break;
                    }
                    case 3: { // end time like 00:01
                        String[] e = value.split(":");
                        if (e.length != 2) {
                            Log.e(PLUGIN_ID, "alarm:parse:start_time failed " + value);
                            return;
                        }
                        timer.hour_minute_stop = Integer.valueOf(e[0]) * 60 + Integer.valueOf(e[1]);
                        if (timer.hour_minute_stop == 99 * 60 + 99) // disabled if time is 99:99
                            timer.hour_minute_stop = -1;
                        break;
                    }
                    case 4: { // random interval time like 00:01
                        String[] e = value.split(":");
                        if (e.length != 2) {
                            Log.e(PLUGIN_ID, "alarm:parse:start_time failed " + value);
                            return;
                        }
                        timer.hour_minute_random_interval = Integer.valueOf(e[0]) * 60 + Integer.valueOf(e[1]);
                        if (timer.hour_minute_random_interval == 99 * 60 + 99) // disabled if time is 99:99
                            timer.hour_minute_random_interval = -1;
                        break;
                    }
                }
            }
        };
        parser.setContentHandler(handler);
        parser.parse(new InputSource(new StringReader(html)));

        for (Timer timer : l) {
            if (!timer.enabled && timer.hour_minute_start == 0 && timer.hour_minute_stop == 23 * 60 + 59)
                timer.freeDeviceAlarm = true;
        }
        return l;
    }

    /**
     * Renaming is done via http and the dd.htm page on the ANEL devices.
     *
     * @param port     The device port to rename.
     * @param new_name The new name
     * @param callback A callback for the done/failed message.
     */
    @Override
    public void rename(final DevicePort port, final String new_name, final AsyncRunnerResult callback) {
        // First call the dd.htm page to get all current values (we only want to change one of those
        // and have to set all the others to the same values as before)
        final String getData = "dd.htm?DD" + String.valueOf(port.id);
        final DeviceConnectionHTTP ci = (DeviceConnectionHTTP) port.device.getFirstReachableConnection("HTTP");
        if (ci == null)
            return;

        HttpThreadPool.execute(HttpThreadPool.createHTTPRunner(ci, getData, null,
                port, true, new HttpThreadPool.HTTPCallback<DevicePort>() {
                    @Override
                    public void httpResponse(DevicePort port, boolean callback_success, String response_message) {
                        if (!callback_success) {
                            callback.asyncRunnerResult(port, false, response_message);
                            return;
                        }

                        String postData;
                        // Parse received web page
                        try {
                            postData = AnelPluginHttp.createHTTP_Post_byHTTP_response(response_message, new_name, new Timer[5]);
                        } catch (UnsupportedEncodingException e) {
                            callback.asyncRunnerResult(port, false, "url_encode failed");
                            return;
                        } catch (SAXException e) {
                            e.printStackTrace();
                            callback.asyncRunnerResult(port, false, "Html Parsing failed");
                            return;
                        } catch (IOException e) {
                            e.printStackTrace();
                            callback.asyncRunnerResult(port, false, "Html IO Parsing failed");
                            return;
                        }

                        HttpThreadPool.execute(HttpThreadPool.createHTTPRunner(ci, getData, postData,
                                port, true, new HttpThreadPool.HTTPCallback<DevicePort>() {
                                    @Override
                                    public void httpResponse(DevicePort port, boolean callback_success,
                                                             String response_message) {
                                        if (callback_success) {
                                            port.setDescription(new_name);
                                        }
                                        callback.asyncRunnerResult(port, callback_success, response_message);
                                    }
                                }
                        ));
                    }
                }
        ));
    }

    @Override
    public void addToTransaction(DevicePort port, int command) {
        command_list.add(new Scene.PortAndCommand(port, command));
    }

    @Override
    public void executeTransaction(ExecutionFinished callback) {
        TreeMap<Device, List<Scene.PortAndCommand>> commands_grouped_by_devices =
                new TreeMap<>();

        // add to tree
        for (Scene.PortAndCommand portAndCommand : command_list) {
            if (!commands_grouped_by_devices.containsKey(portAndCommand.port.device)) {
                commands_grouped_by_devices.put(portAndCommand.port.device, new ArrayList<Scene.PortAndCommand>());
            }
            commands_grouped_by_devices.get(portAndCommand.port.device).add(portAndCommand);
        }

        // execute by device
        for (TreeMap.Entry<Device, List<Scene.PortAndCommand>> entry : commands_grouped_by_devices.entrySet()) {
            executeDeviceBatch(entry.getKey(), entry.getValue(), callback);
        }

        command_list.clear();
    }

    @Override
    public String getPluginID() {
        return PLUGIN_ID;
    }

    @Override
    public void enterFullNetworkState(Device device) {
        if (SharedPrefs.logEnergySaveMode())
            Logging.appendLog("Anel: enterFullNetworkState");

        // Start send thread
        if (udpSending == null)
            udpSending = new UDPSending(false);
        boolean alreadyRunning = udpSending.isRunning();
        if (!alreadyRunning) {
            udpSending.start("AnelUDPSendThread");
        }

        if (device == null) {
            startUDPDiscoveryThreads(null);
        } else {
            // start socket listener on all udp listener ports for the given device
            Set<Integer> ports = new TreeSet<>();
            for (DeviceConnection ci : device.DeviceConnections) {
                if (ci instanceof DeviceConnectionUDP)
                    ports.add(ci.getListenPort());
            }
            startUDPDiscoveryThreads(ports);
        }
    }

    @Override
    public void enterNetworkReducedState() {
        if (SharedPrefs.logEnergySaveMode())
            Logging.appendLog("Anel: enterNetworkReducedState");

        stopUDPDiscoveryThreads();
        boolean running = udpSending != null && udpSending.isRunning();
        if (running) {
            udpSending.interrupt();
            udpSending = null;
        }
    }

    @Override
    public void openConfigurationPage(Device device, Context context) {
        final DeviceConnection ci = device.getFirstReachableConnection();
        if (ci == null) {
            Toast.makeText(NetpowerctrlApplication.instance, R.string.error_device_not_reachable, Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO http port in openConfigurationPage
        Intent browse = new Intent(Intent.ACTION_VIEW,
                Uri.parse("http://" + ci.getDestinationHost() + ":" + Integer.valueOf(80).toString()));
        context.startActivity(browse);
    }

    @Override
    public boolean isNetworkPlugin() {
        return true;
    }

    @Override
    public Timer getNextFreeAlarm(DevicePort port, int type) {
        // We only support those two alarm types
        if (type != Timer.TYPE_RANGE_ON_WEEKDAYS && type != Timer.TYPE_RANGE_ON_RANDOM_WEEKDAYS)
            return null;

        TimerController c = NetpowerctrlApplication.getDataController().timerController;
        List<Timer> available_timers = c.getAvailableDeviceAlarms();
        for (Timer available : available_timers) {
            // Find alarm for the selected port
            if ((available.id & 255) == port.id) {
                return available;
            }
        }
        return null;
    }

    @Override
    public void saveAlarm(final Timer timer, final AsyncRunnerResult callback) {
        if (callback != null)
            callback.asyncRunnerStart(timer.port);

        // First call the dd.htm page to get all current values (we only want to change one of those
        // and have to set all the others to the same values as before)
        final String getData = "dd.htm?DD" + String.valueOf(timer.port.id);
        final int timerNumber = (int) (timer.id >> 8) & 255;
        // Get the timerController object. We will add received alarms to that instance.
        final TimerController timerController = NetpowerctrlApplication.getDataController().timerController;
        final DeviceConnectionHTTP ci = (DeviceConnectionHTTP) timer.port.device.getFirstReachableConnection("HTTP");
        if (ci == null)
            return;

        HttpThreadPool.execute(HttpThreadPool.createHTTPRunner(ci, getData, null,
                timer.port, true, new HttpThreadPool.HTTPCallback<DevicePort>() {
                    @Override
                    public void httpResponse(DevicePort port, boolean callback_success, String response_message) {
                        if (!callback_success) {
                            if (callback != null)
                                callback.asyncRunnerResult(port, false, response_message);
                            return;
                        }

                        Timer[] timers = new Timer[5];
                        timers[timerNumber] = timer;

                        String postData;
                        // Parse received web page
                        try {
                            postData = AnelPluginHttp.createHTTP_Post_byHTTP_response(response_message,
                                    null, timers);
                        } catch (UnsupportedEncodingException e) {
                            if (callback != null)
                                callback.asyncRunnerResult(port, false, "url_encode failed");
                            return;
                        } catch (SAXException e) {
                            e.printStackTrace();
                            if (callback != null)
                                callback.asyncRunnerResult(port, false, "Html Parsing failed");
                            return;
                        } catch (IOException e) {
                            e.printStackTrace();
                            if (callback != null)
                                callback.asyncRunnerResult(port, false, "Html IO Parsing failed");
                            return;
                        }

                        HttpThreadPool.execute(HttpThreadPool.createHTTPRunner(ci, getData, postData,
                                port, true, new HttpThreadPool.HTTPCallback<DevicePort>() {
                                    @Override
                                    public void httpResponse(DevicePort port, boolean callback_success,
                                                             String response_message) {
                                        if (callback != null)
                                            callback.asyncRunnerResult(port, callback_success, response_message);

                                        try {
                                            timerController.alarmsFromPlugin(extractAlarms(port, response_message));
                                        } catch (SAXException | IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                        ));
                    }
                }
        ));
    }

    @Override
    public void removeAlarm(Timer timer, final AsyncRunnerResult callback) {
        if (callback != null)
            callback.asyncRunnerStart(timer.port);

        // Reset all data to default values
        timer.hour_minute_start = 0;
        timer.hour_minute_stop = 23 * 60 + 59;
        timer.hour_minute_random_interval = 0;
        for (int i = 0; i < 7; ++i) timer.weekdays[i] = true;
        timer.enabled = false;
        saveAlarm(timer, callback);
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
