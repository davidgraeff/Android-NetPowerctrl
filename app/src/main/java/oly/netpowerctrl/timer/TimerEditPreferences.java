package oly.netpowerctrl.timer;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Date;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.application_state.PluginInterface;
import oly.netpowerctrl.device_ports.DevicePort;
import oly.netpowerctrl.device_ports.DevicePortSourceConfigured;
import oly.netpowerctrl.device_ports.DevicePortsListAdapter;
import oly.netpowerctrl.network.AsyncRunnerResult;
import oly.netpowerctrl.preferences.DatePreference;
import oly.netpowerctrl.preferences.TimePreference;
import oly.netpowerctrl.utils.ActivityWithIconCache;
import oly.netpowerctrl.utils_gui.DoneCancelFragmentHelper;

public class TimerEditPreferences extends PreferenceFragment implements AsyncRunnerResult {
    DoneCancelFragmentHelper doneCancelFragmentHelper = new DoneCancelFragmentHelper();
    Timer timer;

    private Preference.OnPreferenceChangeListener checkEnabledListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
            NetpowerctrlApplication.getMainThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    checkEnabled();
                }
            });
            return true;
        }
    };

    public TimerEditPreferences() {
    }

    @Override
    public void onStart() {
        super.onStart();
        doneCancelFragmentHelper.setTitle(getActivity(), R.string.alarm_edit);
        doneCancelFragmentHelper.addCancelDone(getActivity(), R.layout.device_done);

        Activity a = getActivity();
        View btnDone = a.findViewById(R.id.action_mode_save_button);
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveAlarm();
            }
        });
        Button btnTest = (Button) a.findViewById(R.id.action_mode_test_button);
        btnTest.setVisibility(timer.id == -1 ? View.GONE : View.VISIBLE);
        btnTest.setText(getString(R.string.remove));
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                removeAlarm();
            }
        });
        View btnCancel = a.findViewById(R.id.action_mode_close_button);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //noinspection ConstantConditions
                getFragmentManager().popBackStack();
            }
        });

    }

    @Override
    public void onStop() {
        doneCancelFragmentHelper.restoreTitle(getActivity());
        doneCancelFragmentHelper.restoreActionBar(getActivity());
        super.onStop();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.alarm_preferences);

        /** Getting the ListPreference from the Preference Resource */
        final PreferenceManager m = getPreferenceManager();

        // Add weekdays
        String[] weekDays_Strings = DateFormatSymbols.getInstance().getShortWeekdays();
        PreferenceCategory lp = (PreferenceCategory) findPreference("alarm_weekdays");
        for (int i = 0; i < 7; ++i) {
            final int weekday = i;
            CheckBoxPreference p = new CheckBoxPreference(getActivity());
            p.setPersistent(false);
            p.setKey("weekday" + String.valueOf(weekday));
            p.setChecked(timer.weekdays[weekday]);
            // The first entry of weekDays_Strings is an empty string
            p.setTitle(weekDays_Strings[weekday + 1]);
            p.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    timer.weekdays[weekday] = (Boolean) o;
                    return true;
                }
            });
            lp.addPreference(p);
        }

        Preference p;

        //noinspection ConstantConditions
        p = findPreference("alarm_device_port");
        p.setEnabled(timer.id == -1);
        p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.alarm_device_name);
                ListView v = new ListView(getActivity());
                builder.setView(v);
                final AlertDialog dialog = builder.create();

                DevicePortSourceConfigured s = new DevicePortSourceConfigured();
                final DevicePortsListAdapter a = new DevicePortsListAdapter(getActivity(), false, s,
                        ((ActivityWithIconCache) getActivity()).getIconCache());
                v.setAdapter(a);
                v.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        timer.port = a.getDevicePort(i);

                        findPreference("alarm_device_port").setSummary(timer.getTargetName());
                        dialog.dismiss();
                    }
                });

                dialog.show();
                return false;
            }
        });

        if (timer.port != null)
            p.setSummary(timer.getTargetName());

        CheckBoxPreference cp = ((CheckBoxPreference) m.findPreference("alarm_start_time_enabled"));
        cp.setChecked(timer.hour_minute_start != -1);
        cp.setOnPreferenceChangeListener(checkEnabledListener);

        cp = ((CheckBoxPreference) m.findPreference("alarm_stop_time_enabled"));
        cp.setChecked(timer.hour_minute_stop != -1);
        cp.setOnPreferenceChangeListener(checkEnabledListener);

        cp = ((CheckBoxPreference) m.findPreference("alarm_enabled"));
        cp.setChecked(timer.enabled);

        TimePreference tp = ((TimePreference) m.findPreference("alarm_start_time"));
        if (timer.hour_minute_start != -1) {
            tp.setTime(timer.time(timer.hour_minute_start));
        }

        tp = ((TimePreference) m.findPreference("alarm_stop_time"));
        if (timer.hour_minute_stop != -1) {
            tp.setTime(timer.time(timer.hour_minute_stop));
        }

        DatePreference dp = ((DatePreference) m.findPreference("alarm_date"));
        if (timer.type == Timer.TYPE_ONCE && timer.absolute_date != null) {
            dp.setDate(timer.absolute_date);
        } else
            dp.setDate(new Date());

        cp = (CheckBoxPreference) m.findPreference("alarm_type_absolute");
        cp.setOnPreferenceChangeListener(checkEnabledListener);
        cp.setChecked(timer.type == Timer.TYPE_ONCE);

        checkEnabled();
    }

    private void checkEnabled() {
        PreferenceManager m = getPreferenceManager();
        m.findPreference("alarm_start_time").setEnabled(
                ((CheckBoxPreference) m.findPreference("alarm_start_time_enabled")).isChecked());
        m.findPreference("alarm_stop_time").setEnabled(
                ((CheckBoxPreference) m.findPreference("alarm_stop_time_enabled")).isChecked());
        boolean absolute = ((CheckBoxPreference) m.findPreference("alarm_type_absolute")).isChecked();
        m.findPreference("alarm_date").setEnabled(absolute);
        m.findPreference("alarm_weekdays").setEnabled(!absolute);
    }

    public void setParameter(Timer timer) {
        this.timer = timer;
        if (this.timer == null) {
            this.timer = new Timer();
        }
    }

    private void removeAlarm() {
        PluginInterface plugin = timer.port.device.getPluginInterface();
        plugin.removeAlarm(timer, this);
    }

    private void saveAlarm() {
        PreferenceManager m = getPreferenceManager();

        // Fill in data
        timer.type = ((CheckBoxPreference) m.findPreference("alarm_type_absolute")).isChecked()
                ? Timer.TYPE_ONCE : Timer.TYPE_RANGE_ON_WEEKDAYS;
        if (!((CheckBoxPreference) m.findPreference("alarm_start_time_enabled")).isChecked())
            timer.hour_minute_start = -1;
        else {
            Calendar c = ((TimePreference) m.findPreference("alarm_start_time")).getTime();
            timer.hour_minute_start = c.get(Calendar.HOUR) * 60 + c.get(Calendar.MINUTE);
        }
        if (!((CheckBoxPreference) m.findPreference("alarm_stop_time_enabled")).isChecked())
            timer.hour_minute_stop = -1;
        else {
            Calendar c = ((TimePreference) m.findPreference("alarm_stop_time")).getTime();
            timer.hour_minute_stop = c.get(Calendar.HOUR) * 60 + c.get(Calendar.MINUTE);
        }
        timer.enabled = ((CheckBoxPreference) m.findPreference("alarm_enabled")).isChecked();

        // Check input data
        if (timer.port == null || timer.port.uuid == null) {
            Toast.makeText(getActivity(), R.string.alarm_no_target, Toast.LENGTH_SHORT).show();
            return;
        }

        timer.port_id = timer.port.uuid;

        // Check input data
        if (timer.hour_minute_start == -1 && timer.hour_minute_stop == -1) {
            Toast.makeText(getActivity(), R.string.alarm_no_start_no_stop, Toast.LENGTH_SHORT).show();
            return;
        }

        PluginInterface plugin = timer.port.device.getPluginInterface();
        // Find free device alarm, if not already assigned
        if (timer.id == -1) {
            Timer found_timer;
            found_timer = plugin.getNextFreeAlarm(timer.port, timer.type);

            if (found_timer == null) {
                Toast.makeText(getActivity(), R.string.alarm_no_device_alarm, Toast.LENGTH_LONG).show();
                return;
            }

            timer.id = found_timer.id;
            timer.deviceAlarm = true;
        }
        plugin.saveAlarm(timer, this);
    }

    @Override
    public void asyncRunnerResult(DevicePort oi, boolean success, String error_message) {
        if (!success) {
            Toast.makeText(getActivity(), error_message, Toast.LENGTH_SHORT).show();
            return;
        }
        //noinspection ConstantConditions
        getFragmentManager().popBackStack();
    }

    @Override
    public void asyncRunnerStart(@SuppressWarnings("UnusedParameters") DevicePort oi) {
        Toast.makeText(getActivity(), "Bitte warten. Speichere Alarm...", Toast.LENGTH_SHORT).show();
    }
}