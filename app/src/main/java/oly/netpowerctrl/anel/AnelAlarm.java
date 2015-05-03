package oly.netpowerctrl.anel;

import android.util.Log;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.ioconnection.DeviceIOConnections;
import oly.netpowerctrl.ioconnection.IOConnectionHTTP;
import oly.netpowerctrl.network.HttpThreadPool;
import oly.netpowerctrl.network.onHttpRequestResult;
import oly.netpowerctrl.timer.Timer;
import oly.netpowerctrl.timer.TimerCollection;

;

/**
 * Anel Alarm functionality
 */
public class AnelAlarm {
    private Map<String, AnelTimer> availableTimers = new TreeMap<>();

    private List<AnelTimer> extractAlarms(final Executable executable, final String html) throws SAXException, IOException {
        final List<AnelTimer> l = new ArrayList<>();
        l.add(new AnelTimer());
        l.add(new AnelTimer());
        l.add(new AnelTimer());
        l.add(new AnelTimer());
        l.add(new AnelTimer());

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
                byte timerNumber = name.length() == 2 ? 4 : (byte) (name.charAt(2) - '0');

                if (dataIndex < 0 || dataIndex > 4 || timerNumber < 0 || timerNumber >= l.size())
                    return;

                AnelTimer timer = l.get(timerNumber);
                int id = AnelPlugin.extractIDFromExecutableUID(executable.getUid());


                switch (dataIndex) {
                    case 0: { // enabled / disabled
                        timer.Executable = executable;
                        timer.enabled = checked != null;
                        timer.alarmOnDevice.portId = (byte) id;
                        timer.alarmOnDevice.timerId = timerNumber;
                        timer.type = timerNumber < 4 ? Timer.TYPE_RANGE_ON_WEEKDAYS : Timer.TYPE_RANGE_ON_RANDOM_WEEKDAYS;
                        timer.computeUniqueTimerID();
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
                            Log.e(AnelPlugin.PLUGIN_ID, "alarm:parse:start_time failed " + value);
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
                            Log.e(AnelPlugin.PLUGIN_ID, "alarm:parse:start_time failed " + value);
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
                            Log.e(AnelPlugin.PLUGIN_ID, "alarm:parse:start_time failed " + value);
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
        return l;
    }

    Timer getNextFreeAlarm(Executable executable, int type, int command) {
        int id = AnelPlugin.extractIDFromExecutableUID(executable.getUid());

        // We only support those two alarm types
        if (type != Timer.TYPE_RANGE_ON_WEEKDAYS && type != Timer.TYPE_RANGE_ON_RANDOM_WEEKDAYS)
            return null;

        // First try to get a completely free alarm slot
        for (AnelTimer available : availableTimers.values()) {
            // Find alarm for the selected executable
            if (available.alarmOnDevice.portId == id && available.isFree()) {
                return translateAnelTimer(available, command);
            }
        }

        // If that is not success, than try to get an partially used slot.
        // Side effect on those slots: Enabling/disabling is only possible for both alarms of these slots.
        for (AnelTimer available : availableTimers.values()) {
            // Find alarm for the selected executable
            if (available.alarmOnDevice.portId == id && available.isUnused(command)) {
                return translateAnelTimer(available, command);
            }
        }
        return null;
    }

    private Timer translateAnelTimer(AnelTimer anelTimer, int command) {
        Timer timer = Timer.createNewTimer();
        timer.alarmOnDevice = anelTimer.alarmOnDevice;
        timer.enabled = anelTimer.enabled;
        timer.executable = anelTimer.Executable;
        timer.executable_uid = anelTimer.Executable.getUid();
        timer.uuid = anelTimer.getUniqueTimerID(command);
        timer.type = anelTimer.type;
        timer.command = command;
        System.arraycopy(anelTimer.weekdays, 0, timer.weekdays, 0, timer.weekdays.length);
        if (command == Executable.ON) {
            timer.hour_minute = anelTimer.hour_minute_start;
        } else if (command == Executable.OFF) {
            timer.hour_minute = anelTimer.hour_minute_stop;
        }
        return timer;
    }

    private AnelTimer findAnelTimerForTimer(int command, String uuid) {
        for (AnelTimer anelTimer : availableTimers.values()) {
            if (anelTimer.getUniqueTimerID(command).equals(uuid)) {
                return anelTimer;
            }
        }
        return null;
    }

    void saveAlarm(DataService dataService, Timer timer, final onHttpRequestResult callback) {
        Executable executable = timer.executable;
        if (callback != null)
            callback.httpRequestStart(executable);

        String recomputedUID = executable.getUid() + "-" + String.valueOf(timer.alarmOnDevice.timerId) + "-" + String.valueOf(timer.command);
        final AnelTimer temp = findAnelTimerForTimer(timer.command, recomputedUID);

        if (temp == null) {
            Log.e("Anel Alarm", "Timer not found " + executable.getTitle());
            if (callback != null)
                callback.httpRequestResult(executable, false, "Timer not found");
            return;
        }

        // First call the dd.htm page to get all current values (we only want to change one of those
        // and have to set all the others to the same values as before)
        int id = AnelPlugin.extractIDFromExecutableUID(executable.getUid());
        final String getData = "dd.htm?DD" + String.valueOf(id);
        final int timerNumber = temp.alarmOnDevice.timerId;
        // Get the timerController object. We will add received alarms to that instance.
        final TimerCollection timerCollection = dataService.timers;
        DeviceIOConnections deviceIOConnections = dataService.connections.openDevice(executable.deviceUID);
        final IOConnectionHTTP ci = deviceIOConnections != null ? (IOConnectionHTTP) deviceIOConnections.findReachable("HTTP") : null;
        if (ci == null) {
            Log.e("Anel Alarm", "No connection! " + executable.getTitle());
            if (callback != null)
                callback.httpRequestResult(executable, false, "No connection");
            return;
        }

        timer.uuid = recomputedUID;
        temp.updateBy(timer);

        HttpThreadPool.execute(new HttpThreadPool.HTTPRunner<>(ci, getData, null,
                executable, true, new HttpThreadPool.HTTPCallback<Executable>() {
            @Override
            public void httpResponse(Executable port, boolean callback_success, String response_message) {
                if (!callback_success) {
                    if (callback != null)
                        callback.httpRequestResult(port, false, response_message);
                    return;
                }

                AnelTimer[] timers = new AnelTimer[5];
                timers[timerNumber] = temp;

                String postData;
                // Parse received web page
                try {
                    postData = AnelReceiveSendHTTP.createHTTP_Post_byHTTP_response(response_message,
                            null, timers);
                } catch (UnsupportedEncodingException e) {
                    if (callback != null)
                        callback.httpRequestResult(port, false, "url_encode failed");
                    return;
                } catch (SAXException e) {
                    e.printStackTrace();
                    if (callback != null)
                        callback.httpRequestResult(port, false, "Html Parsing failed");
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    if (callback != null)
                        callback.httpRequestResult(port, false, "Html IO Parsing failed");
                    return;
                }

                requestAlarms(port, ci, timerCollection, postData, callback);
            }
        }
        ));
    }

    void removeAlarm(DataService dataService, Timer timer, final onHttpRequestResult callback) {
        if (callback != null)
            callback.httpRequestStart(timer.executable);

        // Reset all data to default values
        if (timer.command == Executable.OFF)
            timer.hour_minute = 23 * 60 + 59;
        else
            timer.hour_minute = 0;
        for (int i = 0; i < 7; ++i) timer.weekdays[i] = true;
        timer.enabled = false;
        saveAlarm(dataService, timer, callback);
    }

    void requestAlarms(Executable executable, IOConnectionHTTP ci, final TimerCollection timerCollection, final String postData, final onHttpRequestResult callback) {
        int id = AnelPlugin.extractIDFromExecutableUID(executable.getUid());
        final String getData = "dd.htm?DD" + String.valueOf(id);

        HttpThreadPool.execute(new HttpThreadPool.HTTPRunner<>(ci, getData, postData,
                executable, true, new HttpThreadPool.HTTPCallback<Executable>() {
            @Override
            public void httpResponse(Executable port, boolean callback_success, String response_message) {
                if (!callback_success) {
                    if (callback != null)
                        callback.httpRequestResult(port, false, response_message);
                    return;
                }
                try {
                    List<AnelTimer> anelTimers = extractAlarms(port, response_message);
                    List<Timer> timers = new ArrayList<>();
                    for (AnelTimer anelTimer : anelTimers) {
                        availableTimers.put(anelTimer.getUniqueTimerID(), anelTimer);

                        if (!anelTimer.isUnused(Executable.ON) || anelTimer.enabled)
                            timers.add(translateAnelTimer(anelTimer, Executable.ON));
                        if (!anelTimer.isUnused(Executable.OFF) || anelTimer.enabled)
                            timers.add(translateAnelTimer(anelTimer, Executable.OFF));
                    }
                    timerCollection.alarmsFromPluginOtherThread(timers);
                    if (callback != null)
                        callback.httpRequestResult(port, true, null);
                } catch (SAXException | IOException e) {
                    e.printStackTrace();
                }
            }
        }
        ));
    }
}
