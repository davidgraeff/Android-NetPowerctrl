package oly.netpowerctrl.anel;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.devices.Credentials;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.executables.ExecutableType;
import oly.netpowerctrl.ioconnection.IOConnection;
import oly.netpowerctrl.ioconnection.IOConnectionHTTP;
import oly.netpowerctrl.network.HttpThreadPool;
import oly.netpowerctrl.network.ReachabilityStates;
import oly.netpowerctrl.timer.Timer;

/**
 * Handles all http traffic between anel and the app. Including receiving alerts.
 */
public class AnelReceiveSendHTTP {
    static final HttpThreadPool.HTTPCallback<IOConnection> receiveCtrlHtml = new HttpThreadPool.HTTPCallback<IOConnection>() {
        @Override
        public void httpResponse(IOConnection ioConnection, boolean callback_success, String response_message) {
            if (response_message == null)
                response_message = "";

            DataService dataService = DataService.getService();

            //Log.w("AnelPluginHttp", "http receive" + response_message);
            if (!callback_success) {
                ioConnection.setStatusMessage(response_message);
                ioConnection.setReachability(ReachabilityStates.NotReachable);
                return;
            }

            String[] data = response_message.split(";");
            if (data.length < 10 || !data[0].startsWith("NET-")) {
                ioConnection.setStatusMessage(DataService.getService().getString(R.string.error_packet_received));
                ioConnection.setReachability(ReachabilityStates.NotReachable);
            } else {
                // First update the credentials
                Credentials credentials = ioConnection.getCredentials();
                AnelPlugin anelPlugin = (AnelPlugin) credentials.getPlugin();

                // The name is the second ";"-separated entry of the response_message.
                // Only update device name if it is empty, we do not want to overwrite the name given by the user.
                if (credentials.deviceName.isEmpty())
                    credentials.setDeviceName(data[1].trim());

                // The order is important: First save the credentials, then save the connections, then save the executables.
                // Because: connections need the credentials object. Executables need the reachability information of the connections.

                dataService.credentials.put(credentials);
                // data[10 + i].trim() credentials deviceUID

                // Update the connection
                ioConnection.incReceivedPackets();
                dataService.connections.put(ioConnection);

                // Update executables
                for (int i = 0; i < 8; ++i) {
                    String executable_uid = AnelPlugin.makeExecutableUID(ioConnection.deviceUID, i + 1);
                    Executable executable = dataService.executables.findByUID(executable_uid);
                    if (executable == null) {
                        executable = new Executable();
                        executable.ui_type = ExecutableType.TypeToggle;
                    }
                    anelPlugin.fillExecutable(executable, credentials, executable_uid, data[20 + i].equals("1") ? Executable.ON : Executable.OFF);
                    executable.title = data[10 + i];
                    boolean disabled = data[30 + i].equals("1");
                    if (!disabled)
                        dataService.executables.put(executable);
                }
            }
        }
    };

    /**
     * If we receive a response from a switch action (via http) we request updated data immediately.
     */
    static final HttpThreadPool.HTTPCallback<IOConnectionHTTP> receiveSwitchResponseHtml =
            new HttpThreadPool.HTTPCallback<IOConnectionHTTP>() {
                @Override
                public void httpResponse(IOConnectionHTTP ioConnection, boolean callback_success, String response_message) {
                    if (!callback_success) {
                        ioConnection.setStatusMessage(response_message);
                        ioConnection.setReachability(ReachabilityStates.NotReachable);
                        ioConnection.credentials.getPlugin().getDataService().connections.put(ioConnection);
                    } else {
                        HttpThreadPool.execute(new HttpThreadPool.HTTPRunner<>(ioConnection, "strg.cfg", "", ioConnection, false, receiveCtrlHtml));
                    }
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
                                                  final String newName, final AnelTimer[] newTimer)
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
            AnelTimer current = newTimer[i];
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
