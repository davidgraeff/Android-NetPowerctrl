package oly.netpowerctrl.plugin_anel;

import android.support.annotation.Nullable;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;

import oly.netpowerctrl.R;
import oly.netpowerctrl.credentials.Credentials;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.executables.ExecutableAndCommand;
import oly.netpowerctrl.ioconnection.IOConnection;
import oly.netpowerctrl.ioconnection.IOConnectionHTTP;
import oly.netpowerctrl.network.HttpThreadPool;
import oly.netpowerctrl.network.ReachabilityStates;

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
                    boolean disabled = data[30 + i].equals("1");
                    if (disabled)
                        continue;

                    String executable_uid = AnelSendUDP.makeExecutableUID(ioConnection.deviceUID, i + 1);
                    Executable executable = dataService.executables.findByUID(executable_uid);
                    if (executable == null) {
                        executable = new Executable();
                    }
                    anelPlugin.fillExecutable(executable, credentials, executable_uid, data[20 + i].equals("1") ? ExecutableAndCommand.ON : ExecutableAndCommand.OFF);
                    executable.title = data[10 + i];
                    executable.updateCachedReachability(ReachabilityStates.Reachable);
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
                                                  final String newName, @Nullable AnelTimer[] newTimer)
            throws SAXException, IOException {

        XMLReader parser = XMLReaderFactory.createXMLReader("org.ccil.cowan.tagsoup.Parser");
        XMLParser handler = new XMLParser("", newName);
        parser.setContentHandler(handler);
        parser.parse(new InputSource(new StringReader(response_message)));
        String complete_post_data = handler.getComplete_post_data();

        if (newName != null)
            complete_post_data += "TN=" + URLEncoder.encode(newName, "utf-8") + "&";

        return complete_post_data + "TS=Speichern";
    }

    private static class XMLParser extends DefaultHandler {
        private String complete_post_data;
        private String newName;

        public XMLParser(String complete_post_data, String newName) {
            this.complete_post_data = complete_post_data;
            this.newName = newName;
        }

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
                    (name.equals("T00")) ||
                            (name.equals("T01")) ||
                            (name.equals("T02")) ||
                            (name.equals("T03")) ||
                            (name.equals("T04")))) {
                complete_post_data += name + "=on" + "&";
                return;
            }

            if (value == null)
                return;

            if (checked != null && name.equals("TF")) {
                complete_post_data += name + "=" + value + "&";
                return;
            }

            if (name.equals("T4") ||
                    (name.equals("TN") && newName == null) ||
                    (name.equals("T10") || name.equals("T20") || name.equals("T30")) ||
                    (name.equals("T11") || name.equals("T21") || name.equals("T31")) ||
                    (name.equals("T12") || name.equals("T22") || name.equals("T32")) ||
                    (name.equals("T13") || name.equals("T23") || name.equals("T33")) ||
                    (name.equals("T14") || name.equals("T24") || name.equals("T34"))
                    )
                complete_post_data += name + "=" + URLEncoder.encode(value) + "&";
        }

        public String getComplete_post_data() {
            return complete_post_data;
        }
    }
}
