package oly.netpowerctrl.anel;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.Map;
import java.util.TreeMap;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.device_ports.DevicePort;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.devices.DeviceConnection;
import oly.netpowerctrl.devices.DeviceConnectionHTTP;
import oly.netpowerctrl.listen_service.ListenService;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.network.HttpThreadPool;
import oly.netpowerctrl.timer.Timer;

/**
 * Created by david on 04.07.14.
 */
public class AnelPluginHttp {
    static final HttpThreadPool.HTTPCallback<DeviceConnection> receiveCtrlHtml = new HttpThreadPool.HTTPCallback<DeviceConnection>() {
        @Override
        public void httpResponse(DeviceConnection ci, boolean callback_success, String response_message) {
            if (response_message == null)
                response_message = "";

            //Log.w("AnelPluginHttp", "http receive" + response_message);
            final Device device = ci.getDevice();
            if (!callback_success) {
                ci.setNotReachable(response_message);
                // Call onConfiguredDeviceUpdated to update device info.
                App.getMainThreadHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        AppData.getInstance().deviceCollection.updateNotReachable(App.instance, device);
                    }
                });
            } else {
                String[] data = response_message.split(";");
                if (data.length < 10 || !data[0].startsWith("NET-")) {
                    ci.setNotReachable(ListenService.getService().getString(R.string.error_packet_received));
                    // Call onConfiguredDeviceUpdated to update device info.
                    App.getMainThreadHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            AppData.getInstance().deviceCollection.updateNotReachable(App.instance, device);
                        }
                    });
                } else {
                    // The name is the second ";" separated entry of the response_message.
                    device.DeviceName = data[1].trim();
                    // DevicePorts data. Put that into a new map and use copyFreshDevicePorts method
                    // on the existing device.
                    Map<Integer, DevicePort> ports = new TreeMap<>();
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
                    }
                    AppData.getInstance().onDeviceUpdatedOtherThread(device);
                }

            }
        }
    };
    /**
     * If we receive a response from a switch action (via http) we request updated data immediately.
     */
    static final HttpThreadPool.HTTPCallback<DeviceConnectionHTTP> receiveSwitchResponseHtml =
            new HttpThreadPool.HTTPCallback<DeviceConnectionHTTP>() {
                @Override
                public void httpResponse(DeviceConnectionHTTP ci, boolean callback_success, String response_message) {
                    final Device device = ci.getDevice();
                    if (!callback_success) {
                        ci.setNotReachable(response_message);
                        AppData.getInstance().onDeviceUpdatedOtherThread(device);
                    } else
                        HttpThreadPool.execute(new HttpThreadPool.HTTPRunner<>(ci, "strg.cfg", "", ci, false, receiveCtrlHtml));
                }
            };

    /**
     * Parses a http response of dd.htm and construct the HTTP POST data to send to dd.htm, depending on the
     * methods arguments.
     *
     * @param response_message The http response message to parse the old values from
     * @param newName          New name or null for the old value
     * @param newTimer         List of new alarms (like: T10=1234567&T20=00:00&T30=23:59) or nulls for the old values
     * @return Return the new data for a HTTP POST.
     * @throws org.xml.sax.SAXException
     * @throws java.io.IOException
     */
    static String createHTTP_Post_byHTTP_response(String response_message,
                                                  final String newName, final Timer[] newTimer)
            throws SAXException, IOException {

        final String[] complete_post_data = {""};

        XMLReader parser = XMLReaderFactory.createXMLReader("org.ccil.cowan.tagsoup.Parser");
        org.xml.sax.ContentHandler handler = new DefaultHandler() {
            @SuppressWarnings("deprecation")
            @Override
            public void startElement(String uri,
                                     String localName,
                                     String qName,
                                     org.xml.sax.Attributes attributes) throws SAXException {
                if (!qName.equals("input"))
                    return;
                String name = attributes.getValue("name");
                String value = attributes.getValue("value");
                String checked = attributes.getValue("checked");

                if (name == null)
                    return;

                if (checked != null && (
                        (name.equals("T00") && newTimer[0] == null) ||
                                (name.equals("T01") && newTimer[1] == null) ||
                                (name.equals("T02") && newTimer[2] == null) ||
                                (name.equals("T03") && newTimer[3] == null) ||
                                (name.equals("T04") && newTimer[4] == null))) {
                    complete_post_data[0] += name + "on" + "&";
                    return;
                }

                if (value == null)
                    return;

                if (checked != null && name.equals("TF")) {
                    complete_post_data[0] += name + "=" + value + "&";
                    return;
                }

                if (name.equals("T4") ||
                        (name.equals("TN") && newName == null) ||
                        (name.equals("T10") || name.equals("T20") || name.equals("T30") && newTimer[0] == null) ||
                        (name.equals("T11") || name.equals("T21") || name.equals("T31") && newTimer[1] == null) ||
                        (name.equals("T12") || name.equals("T22") || name.equals("T32") && newTimer[2] == null) ||
                        (name.equals("T13") || name.equals("T23") || name.equals("T33") && newTimer[3] == null) ||
                        (name.equals("T14") || name.equals("T24") || name.equals("T34") && newTimer[4] == null)
                        )
                    complete_post_data[0] += name + "=" + URLEncoder.encode(value) + "&";
            }
        };
        parser.setContentHandler(handler);
        parser.parse(new InputSource(new StringReader(response_message)));

        if (newName != null)
            complete_post_data[0] += "TN=" + URLEncoder.encode(newName, "utf-8") + "&";

        for (int i = 0; i < newTimer.length; ++i) {
            Timer current = newTimer[i];
            if (current == null)
                continue;

            String timer = String.valueOf(i);

            //  T10=1234567 & T20=00:00 & T30=23:59

            if (current.enabled)
                complete_post_data[0] += "T0" + timer + "=on&";

            // Weekdays
            complete_post_data[0] += "T1" + timer + "=";
            for (int w = 0; w < current.weekdays.length; ++w)
                if (current.weekdays[w])
                    complete_post_data[0] += String.valueOf(w + 1);
            complete_post_data[0] += "&";

            if (current.hour_minute_random_interval == -1)
                current.hour_minute_random_interval = 99 * 60 + 99;

            // on-time
            complete_post_data[0] += "T2" + timer + "=";
            if (current.hour_minute_start == -1)
                complete_post_data[0] += "99:99";
            else
                complete_post_data[0] += URLEncoder.encode(Timer.time(current.hour_minute_start), "utf-8");
            complete_post_data[0] += "&";

            // off-time
            complete_post_data[0] += "T3" + timer + "=";
            if (current.hour_minute_stop == -1)
                complete_post_data[0] += "99:99";
            else
                complete_post_data[0] += URLEncoder.encode(Timer.time(current.hour_minute_stop), "utf-8");
            complete_post_data[0] += "&";

            if (i == 4) {
                // random-time
                complete_post_data[0] += "T4=";
                if (current.hour_minute_random_interval == -1)
                    complete_post_data[0] += "99:99";
                else
                    complete_post_data[0] += URLEncoder.encode(Timer.time(current.hour_minute_random_interval), "utf-8");
                complete_post_data[0] += "&";
            }
        }


        return complete_post_data[0] + "TS=Speichern";
    }
}
