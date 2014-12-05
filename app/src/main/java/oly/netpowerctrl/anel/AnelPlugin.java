package oly.netpowerctrl.anel;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DeviceConnection;
import oly.netpowerctrl.device_base.device.DeviceConnectionHTTP;
import oly.netpowerctrl.device_base.device.DeviceConnectionUDP;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.devices.DeviceEditDialog;
import oly.netpowerctrl.devices.EditDeviceInterface;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.network.HttpThreadPool;
import oly.netpowerctrl.network.UDPSending;
import oly.netpowerctrl.network.onExecutionFinished;
import oly.netpowerctrl.network.onHttpRequestResult;
import oly.netpowerctrl.pluginservice.PluginInterface;
import oly.netpowerctrl.pluginservice.PluginService;
import oly.netpowerctrl.scenes.Scene;
import oly.netpowerctrl.timer.Timer;
import oly.netpowerctrl.timer.TimerCollection;
import oly.netpowerctrl.ui.notifications.InAppNotifications;
import oly.netpowerctrl.utils.Logging;

/**
 * For executing a name on a DevicePort or commands for multiple DevicePorts (bulk).
 * This is a specialized class for Anel devices.
 */
final public class AnelPlugin implements PluginInterface {
    public static final String PLUGIN_ID = "org.anel.outlets_and_io";
    private static final byte[] requestMessage = "wer da?\r\n".getBytes();
    private final List<AnelUDPReceive> discoveryThreads = new ArrayList<>();
    private final List<Scene.PortAndCommand> command_list = new ArrayList<>();
    private UDPSending udpSending;
    private AnelAlarm anelAlarm = new AnelAlarm();

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
     * Execute multiple port commands for one device (anel supports this as an extra command).
     *
     * @param deviceConnection The device connection to use.
     * @param command_list     Command list where each entry is a port and command
     * @return Number of successful commands
     */
    private int executeDeviceBatch(@NonNull DeviceConnection deviceConnection,
                                   @NonNull List<Scene.PortAndCommand> command_list) {
        if (deviceConnection instanceof DeviceConnectionHTTP) { // http
            int success = 0;
            // Use Http instead of UDP for sending. For each batch command we will send a single http request
            for (Scene.PortAndCommand c : command_list) {
                boolean ok = executeViaHTTP(deviceConnection, c.port, c.command);
                if (ok) ++success;
            }
            return success;
        } else if (!(deviceConnection instanceof DeviceConnectionUDP)) { // unknown protocol
            return 0;
        }

        if (checkAndStartUDP()) {
            return 0;
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

        // First step: Setup data byte (outlet, io) to reflect the current state of the device ports.
        Device device = deviceConnection.device;
        device.lockDevicePorts();
        Iterator<DevicePort> it = device.getDevicePortIterator();
        while (it.hasNext()) {
            DevicePort oi = it.next();
            if (oi.current_value == 0) // Only take "ON" commands into account for the bulk change byte
                continue;
            int id = oi.id;
            if (id >= 10 && id < 20) {
                data_io = switchOn(data_io, id - 10);
            } else if (id >= 0) {
                data_outlet = switchOn(data_outlet, id);
            }
        }
        device.releaseDevicePorts();

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
        String access = device.getUserName() + device.getPassword();
        byte[] data = new byte[3 + access.length()];
        System.arraycopy(access.getBytes(), 0, data, 3, access.length());

        if (containsOutlets) {
            data[0] = 'S';
            data[1] = 'w';
            data[2] = data_outlet;
            udpSending.addJob(new UDPSending.SendAndObserveJob(udpSending, App.instance, deviceConnection, data,
                    requestMessage, UDPSending.INQUERY_REQUEST));
        }
        if (containsIO) {
            data[0] = 'I';
            data[1] = 'O';
            data[2] = data_io;
            udpSending.addJob(new UDPSending.SendAndObserveJob(udpSending, App.instance, deviceConnection, data,
                    requestMessage, UDPSending.INQUERY_REQUEST));
        }

        return command_list.size();
    }

    public void startUDPDiscoveryThreads(Set<Integer> additional_port) {
        // Get all ports of configured devices and add the additional_port if != 0
        Set<Integer> ports = AppData.getInstance().getAllReceivePorts();
        if (additional_port != null)
            ports.addAll(additional_port);

        boolean new_threads_started = false;
        List<AnelUDPReceive> unusedThreads = new ArrayList<>(discoveryThreads);

        // Go through all ports and start a thread for it if none is running for it so far
        for (int port : ports) {
            boolean already_running = false;
            for (AnelUDPReceive running_thread : discoveryThreads) {
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
            AnelUDPReceive thr = new AnelUDPReceive(this, port);
            thr.start();
            discoveryThreads.add(thr);
        }

        if (unusedThreads.size() > 0) {
            for (AnelUDPReceive thr : unusedThreads) {
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

    public void stopNetwork() {
        synchronized (this) {
            UDPSending copy = udpSending;
            udpSending = null;
            if (copy != null && copy.isRunning()) {
                copy.interrupt();
            }

            if (discoveryThreads.size() > 0) {
                for (AnelUDPReceive thr : discoveryThreads)
                    thr.interrupt();
                discoveryThreads.clear();
            }

            HttpThreadPool.stopHTTP();
        }
    }

    public boolean executeViaHTTP(DeviceConnection deviceConnection, DevicePort port, int command) {
        if (deviceConnection.getDestinationPort() < 0) {
            Toast.makeText(App.instance, R.string.error_device_no_network_connections, Toast.LENGTH_SHORT).show();
            return false;
        }
        // The http interface can only toggle. If the current state is the same as the command state
        // then we request values instead of sending a command.
        if (command == DevicePort.TOGGLE && port.current_value == command)
            HttpThreadPool.execute(new HttpThreadPool.HTTPRunner<>((DeviceConnectionHTTP) deviceConnection, "strg.cfg",
                    "", deviceConnection, false, AnelHttpReceiveSend.receiveCtrlHtml));
        else
            // Important: For UDP the id is 1-based. For Http the id is 0 based!
            HttpThreadPool.execute(new HttpThreadPool.HTTPRunner<>((DeviceConnectionHTTP) deviceConnection, "ctrl.htm",
                    "F" + String.valueOf(port.id - 1) + "=s", (DeviceConnectionHTTP) deviceConnection, false, AnelHttpReceiveSend.receiveSwitchResponseHtml));
        return true;
    }

    /**
     * Switch a single outlet or io port
     *
     * @param port     Execute on this port
     * @param command  Execute this command
     * @param callback This callback will be called when the execution finished
     */
    @Override
    public boolean execute(DevicePort port, int command, onExecutionFinished callback) {
        // Get necessary objects
        PluginService service = PluginService.getService();
        if (service == null) {
            if (callback != null) callback.onExecutionProgress(0, 1, 1);
            return false;
        }

        port.last_command_timecode = System.currentTimeMillis();

        boolean bValue = false;
        if (command == DevicePort.ON)
            bValue = true;
        else if (command == DevicePort.OFF)
            bValue = false;
        else if (command == DevicePort.TOGGLE)
            bValue = port.current_value <= 0;

        final Device device = port.device;
        final DeviceConnection deviceConnection = device.getFirstReachableConnection();

        // Use Http instead of UDP for sending. For each command we will send a single http request
        if (deviceConnection instanceof DeviceConnectionHTTP) {
            boolean success = executeViaHTTP(deviceConnection, port, command);
            if (callback != null) {
                if (success) callback.onExecutionProgress(1, 0, 1);
                else callback.onExecutionProgress(0, 1, 1);
            }
            return success;
        } else if (deviceConnection instanceof DeviceConnectionUDP) {
            if (checkAndStartUDP()) {
                if (callback != null) callback.onExecutionProgress(0, 1, 1);
                return false;
            }

            byte[] data;
            UDPSending.Job j;
            if (port.id >= 10 && port.id < 20) {
                // IOS
                data = String.format(Locale.US, "%s%d%s%s", bValue ? "IO_on" : "IO_off",
                        port.id - 10, device.getUserName(), device.getPassword()).getBytes();
                j = new UDPSending.SendAndObserveJob(udpSending, service, deviceConnection, data, requestMessage, UDPSending.INQUERY_REQUEST);
                udpSending.addJob(j);
                if (callback != null) callback.onExecutionProgress(1, 0, 1);
                return true;
            } else if (port.id >= 0) {
                // Outlets
                data = String.format(Locale.US, "%s%d%s%s", bValue ? "Sw_on" : "Sw_off",
                        port.id, device.getUserName(), device.getPassword()).getBytes();
                j = new UDPSending.SendAndObserveJob(udpSending, service, deviceConnection, data, requestMessage, UDPSending.INQUERY_REQUEST);
                udpSending.addJob(j);
                if (callback != null) callback.onExecutionProgress(1, 0, 1);
                return true;
            } else {
                if (callback != null) callback.onExecutionProgress(0, 1, 1);
                return false;
            }
        } else {
            Log.e("Anel", "execute. No reachable DeviceConnection found!");
            if (callback != null) callback.onExecutionProgress(0, 1, 1);
            return false;
        }
    }

    /**
     * @return Return true if no udp sending thread is running
     */
    private boolean checkAndStartUDP() {
        if (udpSending == null) {
            InAppNotifications.showException(PluginService.getService(), new Exception(), "udpSending null");
            return true;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        stopNetwork();
    }

    @Override
    public void onStart(PluginService service) {

    }

    @Override
    public void requestData() {
        // Get necessary objects
        PluginService service = PluginService.getService();
        if (service == null)
            return;
        if (checkAndStartUDP()) {
            return;
        }

        udpSending.addJob(new AnelBroadcastSendJob(udpSending));
    }

    @Override
    public void requestData(Device device, int device_connection_id) {
        // Get necessary objects
        PluginService service = PluginService.getService();
        if (service == null)
            return;

        if (!device.isEnabled())
            return;

        device.lockDevice();
        DeviceConnection ci = device.getConnectionByID(device_connection_id);
        device.releaseDevice();
        if (ci instanceof DeviceConnectionHTTP) {
            HttpThreadPool.execute(new HttpThreadPool.HTTPRunner<>((DeviceConnectionHTTP) ci,
                    "strg.cfg", "", ci, false, AnelHttpReceiveSend.receiveCtrlHtml));
        } else {
            if (checkAndStartUDP()) {
                return;
            }
            udpSending.addJob(new UDPSending.SendAndObserveJob(udpSending, service, ci, requestMessage, UDPSending.INQUERY_REQUEST));
        }
    }

    @Override
    public EditDeviceInterface openEditDevice(@Nullable Device device) {
        EditDeviceInterface editDeviceInterface = new AnelEditDevice(MainActivity.instance.getString(R.string.default_device_name), device);
        editDeviceInterface.getDevice().setPluginInterface(this);
        return editDeviceInterface;
    }

    // We assume the MainActivity exist!
    @Override
    public void showConfigureDeviceScreen(Device device) {
        device.setPluginInterface(this);
        DeviceEditDialog f = (DeviceEditDialog) Fragment.instantiate(MainActivity.instance, DeviceEditDialog.class.getName());
        f.setDevice(device);
        MainActivity.getNavigationController().changeToDialog(MainActivity.instance, f);
    }


    /**
     * Renaming is done via http and the dd.htm page on the ANEL devices.
     *
     * @param port     The device port to rename.
     * @param new_name The new name
     * @param callback A callback for the done/failed message.
     */
    @Override
    public void rename(final DevicePort port, final String new_name, final onHttpRequestResult callback) {
        // First call the dd.htm page to get all current values (we only want to change one of those
        // and have to set all the others to the same values as before)
        final String getData = "dd.htm?DD" + String.valueOf(port.id);
        final DeviceConnectionHTTP ci = (DeviceConnectionHTTP) port.device.getFirstReachableConnection("HTTP");
        if (ci == null) {
            Toast.makeText(App.instance, R.string.error_rename_only_with_http_connection, Toast.LENGTH_SHORT).show();
            return;
        }

        HttpThreadPool.execute(new HttpThreadPool.HTTPRunner<>(ci, getData, null,
                port, true, new HttpThreadPool.HTTPCallback<DevicePort>() {
            @Override
            public void httpResponse(DevicePort port, boolean callback_success, String response_message) {
                if (!callback_success) {
                    callback.httpRequestResult(port, false, response_message);
                    return;
                }

                String postData;
                // Parse received web page
                try {
                    postData = AnelHttpReceiveSend.createHTTP_Post_byHTTP_response(response_message, new_name, new AnelTimer[5]);
                } catch (UnsupportedEncodingException e) {
                    callback.httpRequestResult(port, false, "url_encode failed");
                    return;
                } catch (SAXException e) {
                    e.printStackTrace();
                    callback.httpRequestResult(port, false, "Html Parsing failed");
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    callback.httpRequestResult(port, false, "Html IO Parsing failed");
                    return;
                }

                HttpThreadPool.execute(new HttpThreadPool.HTTPRunner<>(ci, getData, postData,
                        port, true, new HttpThreadPool.HTTPCallback<DevicePort>() {
                    @Override
                    public void httpResponse(DevicePort port, boolean callback_success,
                                             String response_message) {
                        if (callback_success) {
                            port.setTitle(new_name);
                        }
                        callback.httpRequestResult(port, callback_success, response_message);
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
    public void executeTransaction(onExecutionFinished callback) {
        TreeMap<Device, List<Scene.PortAndCommand>> commands_grouped_by_devices =
                new TreeMap<>();

        int all = command_list.size();
        int success = 0;

        // add to tree
        for (Scene.PortAndCommand portAndCommand : command_list) {
            if (!commands_grouped_by_devices.containsKey(portAndCommand.port.device)) {
                commands_grouped_by_devices.put(portAndCommand.port.device, new ArrayList<Scene.PortAndCommand>());
            }
            commands_grouped_by_devices.get(portAndCommand.port.device).add(portAndCommand);
        }

        // executeToggle by device
        for (TreeMap.Entry<Device, List<Scene.PortAndCommand>> entry : commands_grouped_by_devices.entrySet()) {
            DeviceConnection deviceConnection = entry.getKey().getFirstReachableConnection();
            if (deviceConnection == null) {
                continue;
            }

            success += executeDeviceBatch(deviceConnection, entry.getValue());
        }

        command_list.clear();

        if (callback != null) callback.onExecutionProgress(success, all - success, all);
    }

    @Override
    public String getPluginID() {
        return PLUGIN_ID;
    }

    @Override
    public void enterFullNetworkState(Context context, Device device) {
        Logging.getInstance().logEnergy("Anel: enterFullNetworkState");

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
            device.lockDevice();
            for (DeviceConnection ci : device.getDeviceConnections()) {
                if (ci instanceof DeviceConnectionUDP)
                    ports.add(((DeviceConnectionUDP) ci).getListenPort());
            }
            device.releaseDevice();
            startUDPDiscoveryThreads(ports);
        }
    }

    @Override
    public void enterNetworkReducedState(Context context) {
        Logging.getInstance().logEnergy("Anel: enterNetworkReducedState");

        new AsyncTask<AnelPlugin, Void, Void>() {
            @Override
            protected Void doInBackground(AnelPlugin... plugin) {
                plugin[0].stopNetwork();
                return null;
            }
        }.execute(this);

        AppData d = AppData.getInstance();
        for (Device di : d.findDevices(this)) {
            // Mark all devices as changed: If network reduced mode ends all
            // devices propagate changes then.
            di.setStatusMessage("UDP", App.getAppString(R.string.device_energysave_mode), true);
            d.updateExistingDevice(di);
        }
    }

    @Override
    public boolean isNetworkReducedState() {
        return (udpSending == null);
    }

    @Override
    public void openConfigurationPage(Device device, Context context) {
        final DeviceConnection ci = device.getFirstReachableConnection();
        if (ci == null) {
            Toast.makeText(context, R.string.device_test_not_reachable, Toast.LENGTH_SHORT).show();
            return;
        }

        int http_port = 80;
        device.lockDevice();
        for (DeviceConnection deviceConnection : device.getDeviceConnections()) {
            if (deviceConnection instanceof DeviceConnectionHTTP) {
                http_port = deviceConnection.getDestinationPort();
                break;
            }
        }
        device.releaseDevice();

        Intent browse = new Intent(Intent.ACTION_VIEW,
                Uri.parse("http://" + ci.getDestinationHost() + ":" + Integer.valueOf(http_port).toString()));
        context.startActivity(browse);
    }

    @Override
    public boolean isNetworkPlugin() {
        return true;
    }

    @Override
    public Timer getNextFreeAlarm(DevicePort port, int type, int command) {
        return anelAlarm.getNextFreeAlarm(port, type, command);
    }

    @Override
    public void saveAlarm(Timer timer, onHttpRequestResult callback) {
        anelAlarm.saveAlarm(timer, callback);
    }

    @Override
    public void removeAlarm(Timer timer, onHttpRequestResult callback) {
        anelAlarm.removeAlarm(timer, callback);
    }

    @Override
    public void requestAlarms(DevicePort port, TimerCollection timerCollection) {
        final DeviceConnectionHTTP ci = (DeviceConnectionHTTP) port.device.getFirstReachableConnection("HTTP");
        if (ci == null)
            return;

        anelAlarm.requestAlarms(port, ci, timerCollection, null, null);
    }


}
