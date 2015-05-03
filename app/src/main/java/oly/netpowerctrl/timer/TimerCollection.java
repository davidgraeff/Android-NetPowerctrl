package oly.netpowerctrl.timer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.data.storage_container.CollectionMapItems;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.executables.ExecutableCollection;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.network.onExecutionFinished;
import oly.netpowerctrl.network.onHttpRequestResult;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.Logging;
import oly.netpowerctrl.utils.ObserverUpdateActions;
import oly.netpowerctrl.utils.WakeLocker;
import oly.netpowerctrl.utils.onCollectionUpdated;

/**
 * Control all configured alarms
 */
public class TimerCollection extends CollectionMapItems<TimerCollection, Timer> {
    private static final String TAG = "TimerCollection";
    private static final long maxDiffMS = 5000;
    public onCollectionUpdated<ExecutableCollection, Executable> deviceObserver = new onCollectionUpdated<ExecutableCollection, Executable>() {
        @Override
        public boolean updated(@NonNull ExecutableCollection c, @Nullable Executable executable, @NonNull ObserverUpdateActions action) {
            if (action != ObserverUpdateActions.RemoveAllAction && action != ObserverUpdateActions.RemoveAction)
                return true;

            if (executable == null) {
                removeAll();
                return true;
            }

            for (Map.Entry<String, Timer> entry : items.entrySet()) {
                if (entry.getValue().executable_uid.equals(executable.getUid())) {
                    removeAlarmFromDisk(entry.getValue());
                }
            }

            return true;
        }
    };
    private int receivedAlarmCount = 0;
    private int allAlarmCount = 0;
    private boolean requestActive = false;
    private final Runnable notifyRunnable = new Runnable() {
        @Override
        public void run() {
            for (Map.Entry<String, Timer> entry : items.entrySet()) {
                if (entry.getValue().isFromCache()) {
                    removeAlarmFromDisk(entry.getValue());
                }
            }

            if (requestActive) {
                requestActive = false;
                //notifyObservers(null, ObserverUpdateActions.ClearAndNewAction);
            }
        }
    };

    public TimerCollection(DataService dataService) {
        super(dataService, "alarms");
        dataService.executables.registerObserver(deviceObserver);
    }

    public static void printAlarm(long time, String name) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Logging.getInstance().logAlarm("Next alarm: " + name + "\nOn: " + sdf.format(calendar.getTime()));
    }

    public static void armAndroidAlarm(Context context, long wakeupTimeInMs) {
        AlarmManager mgr = (AlarmManager) context.getSystemService(android.content.Context.ALARM_SERVICE);
        Intent intent = new Intent(context, DataService.class);
        intent.putExtra("isAlarm", true);
        PendingIntent pi = PendingIntent.getService(context, 1191, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_CANCEL_CURRENT);

        mgr.cancel(pi);

        if (wakeupTimeInMs == -1) {
            // Disable the manifest entry for the BootCompletedReceiver. This way the app won't unnecessary wake up after boot.
            ComponentName receiver = new ComponentName(context, BootCompletedReceiver.class);
            PackageManager pm = context.getPackageManager();
            pm.setComponentEnabledSetting(receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
            return;
        }

        if (wakeupTimeInMs < System.currentTimeMillis()) {
            printAlarm(wakeupTimeInMs, SharedPrefs.getNextAlarmName(context));
            return;
        }

        TimerCollection.printAlarm(wakeupTimeInMs, SharedPrefs.getNextAlarmName(context));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            mgr.setExact(AlarmManager.RTC_WAKEUP, wakeupTimeInMs, pi);
        else
            mgr.set(AlarmManager.RTC_WAKEUP, wakeupTimeInMs, pi);

        // Enable the manifest entry for the BootCompletedReceiver. This way alarms are setup again after a reboot.
        ComponentName receiver = new ComponentName(context, BootCompletedReceiver.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    /**
     * Check for alarms. If the current time matches (somehow) the next alarm time,
     * executeAlarm is called as soon as the DataService is ready. Otherwise the android
     * alarm is rescheduled.
     * The DataService as well as the DataService object may not be ready at this time.
     */
    public void checkAlarm(long checkTime) {
        if (checkTime == -1) return;
        final long nextAlarmTimestamp = SharedPrefs.getNextAlarmCheckTimestamp(App.instance);

        // Do not try to setup an alarm, if we have cached, that there is no valid alarm available.
        if (nextAlarmTimestamp == -1) return;

        // Check if current time matches somehow the next alarm time
        if (checkTime <= nextAlarmTimestamp + maxDiffMS) {
            if (checkTime >= nextAlarmTimestamp) {
                dataService.timers.executeAlarm(nextAlarmTimestamp);
            } else
                TimerCollection.armAndroidAlarm(App.instance, nextAlarmTimestamp);
        } else
            setupNextAndroidAlarmFromPointInTime(App.instance, checkTime);
    }

    public void setupNextAndroidAlarmFromPointInTime(Context context, long earliestPointInTimeInMs) {
        Timer nextTimer = null;

        for (int i = items.size() - 1; i >= 0; --i) {
            Timer timer = items.get(i);
            if (timer.alarmOnDevice != null)
                continue;

            timer.computeNextAlarmUnixTime(earliestPointInTimeInMs);

            if (timer.next_execution_unix_time >= earliestPointInTimeInMs &&
                    (nextTimer == null || timer.next_execution_unix_time < nextTimer.next_execution_unix_time)) {
                nextTimer = timer;
            }

            // Remove old one-time timer
            if (timer.type == Timer.TYPE_ONCE && timer.next_execution_unix_time < earliestPointInTimeInMs) {
                removeAlarmFromDisk(timer);
            }
        }

        if (nextTimer == null) {
            // Reset next alarm time cache
            SharedPrefs.setNextAlarmCheckTimestamp(context, 0, "");
            armAndroidAlarm(context, -1);
        } else {
            SharedPrefs.setNextAlarmCheckTimestamp(context, nextTimer.next_execution_unix_time, nextTimer.getTargetName());
            armAndroidAlarm(context, nextTimer.next_execution_unix_time);
        }
    }

    private void executeAlarm(final long nextAlarmTimestamp) {
        List<Executable> executables = new ArrayList<>();
        List<Integer> commands = new ArrayList<>();

        // Reset the stored nextAlarm time.
        SharedPrefs.setNextAlarmCheckTimestamp(App.instance, -1, "");

        // Determine executables for the given alarm time {@link nextAlarmTimestamp}
        for (Timer timer : getItems().values()) {
            if (timer.alarmOnDevice != null)
                continue;

            timer.computeNextAlarmUnixTime(nextAlarmTimestamp);

            if (timer.next_execution_unix_time >= nextAlarmTimestamp &&
                    timer.next_execution_unix_time < nextAlarmTimestamp + maxDiffMS) {
                Executable executable = dataService.executables.findByUID(timer.executable_uid);

                if (executable != null)
                    Logging.getInstance().logAlarm("Alarm: " + executable.getTitle());
                else {
                    Logging.getInstance().logAlarm("Alarm: Executable does not exist!");
                    continue;
                }

                executables.add(executable);
                commands.add(timer.command);
            }
        }

        if (executables.size() == 0) {
            Logging.getInstance().logAlarm("Armed alarm not found!");
            setupNextAndroidAlarmFromPointInTime(App.instance, nextAlarmTimestamp + maxDiffMS);
            return;
        }

        // Setup executionFinished callback and countDownLatch to release the wavelock, that we will
        // acquire for the duration of the alarm execution.
        final AtomicInteger countDownInteger = new AtomicInteger(executables.size());
        //final CountDownLatch countDownLatch = new CountDownLatch(executables.size());
        onExecutionFinished executionFinished = new onExecutionFinished(executables.size()) {
            @Override
            public void onExecutionProgress() {
                if (success + errors >= expected) {
                    Log.w(TAG, "alarm finished");
                    //Thread.dumpStack();
                    Logging.getInstance().logAlarm("Alarm items (OK, FAIL):\n" + String.valueOf(success) + ", " + String.valueOf(errors));
                    countDownInteger.decrementAndGet();
                }
            }
        };

        Iterator<Integer> command_it = commands.iterator();
        for (Executable executable : executables) {
            WakeLocker.acquire(App.instance);
            executable.execute(dataService, command_it.next(), executionFinished);
        }

        // Hint: Initially a countDownLatch has been used to finish this method after all
        // dataService.execute finished for every executable (or after 5s). But the main loop
        // was stopped and no network commands have been handed over to the network thread.
        App.getMainThreadHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                WakeLocker.release();
                long nextMinimumAlarmTime = Math.max(nextAlarmTimestamp + maxDiffMS, System.currentTimeMillis());
                // Setup new alarm if one got executed
                setupNextAndroidAlarmFromPointInTime(App.instance, nextMinimumAlarmTime);

                dataService.checkStopAfterAlarm();
            }
        }, 5000);
    }

    public boolean isRequestActive() {
        return requestActive;
    }

    public int countAllAlarms() {
        return allAlarmCount;
    }

    public int countReceivedAlarms() {
        return receivedAlarmCount;
    }

    /**
     * Called by plugins to propagate alarms.
     *
     * @param new_timers All alarms of the plugin.
     */
    public void alarmsFromPluginOtherThread(final List<Timer> new_timers) {
        Handler h = App.getMainThreadHandler();
        h.removeCallbacks(notifyRunnable);
        h.postDelayed(notifyRunnable, 1500);
        h.post(new Runnable() {
            @Override
            public void run() {
                ++receivedAlarmCount;
                notifyObservers(null, ObserverUpdateActions.UpdateAction);
                if (new_timers == null) return;
                for (Timer new_timer : new_timers) {
                    put(new_timer, false);
                }
            }
        });
    }

    public void put(Timer timer, boolean onlyForCache) {
        Timer existing = items.get(timer.getUid());
        items.put(timer.getUid(), timer);
        if (existing != null) {
            notifyObservers(timer, ObserverUpdateActions.UpdateAction);
        } else {
            notifyObservers(timer, ObserverUpdateActions.AddAction);
        }

        if (!onlyForCache)
            storage.save(timer);

        if (timer.alarmOnDevice == null)
            setupNextAndroidAlarmFromPointInTime(App.instance, System.currentTimeMillis());
    }

    void removeDeviceAlarm(final Timer timer, final onHttpRequestResult callback) {
//        if (timer.executable instanceof Executable && timer.alarmOnDevice != null) {
//            AbstractBasePlugin p = timer.executable.getPlugin();
        //TODO
//            p.removeAlarm(timer, new onHttpRequestResult() {
//                @Override
//                public void httpRequestResult(Executable oi, boolean success, String error_message) {
//                    if (success)
//                        removeAlarmFromDisk(timer);
//                    if (callback != null)
//                        callback.httpRequestResult(oi, success, error_message);
//                }
//
//                @Override
//                public void httpRequestStart(@SuppressWarnings("UnusedParameters") Executable oi) {
//                    if (callback != null)
//                        callback.httpRequestStart(oi);
//                }
//            });
//        } else {
//            removeAlarmFromDisk(timer);
//        }
    }

    void removeAlarmFromDisk(Timer timer) {
        storage.remove(timer);
        items.remove(timer.getUid());
        notifyObservers(timer, ObserverUpdateActions.RemoveAction);
    }

    public boolean refresh(Executable Executable) {
        if (requestActive)
            return true;

        App.getMainThreadHandler().postDelayed(notifyRunnable, 1500);

        // Flag all alarms as from-cache
        HashSet<String> alarm_uuids = new HashSet<>();
        for (Timer timer : items.values()) {
            timer.markFromCache();
            if (timer.alarmOnDevice != null)
                alarm_uuids.add(timer.executable_uid);
        }

        requestActive = true;
        notifyObservers(null, ObserverUpdateActions.UpdateAction);

        List<Executable> alarm_ports = new ArrayList<>();


        if (alarm_uuids.contains(Executable.getUid()))
            alarm_ports.add(0, Executable); // add in front of all alarm_uuids
        else
            alarm_ports.add(Executable);

        receivedAlarmCount = 0;
        allAlarmCount = 0;
//        for (Executable port : alarm_ports) {
//            AbstractBasePlugin i = port.getPlugin();
//            //TODO eigentlich sollen alle getPlugin() etwas zurückgeben. Jedoch laden Erweiterungen nicht schnell genug für diesen Aufruf hier
//            if (i != null) {
//                ++allAlarmCount;
//                //i.requestAlarms(port, this);
//                //TODO
//            }
//        }
        notifyObservers(null, ObserverUpdateActions.UpdateAction);

        return requestActive;
    }

    public void abortRequest() {
        Handler h = App.getMainThreadHandler();
        h.removeCallbacks(notifyRunnable);
        requestActive = false;
        notifyObservers(null, ObserverUpdateActions.UpdateAction);
    }

    public void removeAll() {
        int b = items.size();
        // Delete all alarms
        for (Timer timer : items.values())
            removeDeviceAlarm(timer, null);
        items.clear();
        notifyObservers(null, ObserverUpdateActions.RemoveAllAction);
    }

    public void fillItems(Executable executable, List<Timer> out_list) {
        for (Timer timer : items.values())
            if (timer.executable_uid.equals(executable.getUid()))
                out_list.add(timer);
    }

    public Timer getByUID(String timerUid) {
        return items.get(timerUid);
    }
}
