package oly.netpowerctrl.alarms;

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
import oly.netpowerctrl.application_state.NetpowerctrlService;
import oly.netpowerctrl.application_state.PluginInterface;
import oly.netpowerctrl.device_ports.DevicePortSourceConfigured;
import oly.netpowerctrl.device_ports.DevicePortsListAdapter;
import oly.netpowerctrl.devices.DevicePort;
import oly.netpowerctrl.network.AsyncRunnerResult;
import oly.netpowerctrl.preferences.DatePreference;
import oly.netpowerctrl.preferences.TimePreference;
import oly.netpowerctrl.utils.ActivityWithIconCache;
import oly.netpowerctrl.utils.DoneCancelFragmentHelper;

public class AlarmEditPreferences extends PreferenceFragment implements AsyncRunnerResult {
    DoneCancelFragmentHelper doneCancelFragmentHelper = new DoneCancelFragmentHelper();
    Alarm alarm;

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

    public AlarmEditPreferences() {
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
        btnTest.setVisibility(alarm.id == -1 ? View.GONE : View.VISIBLE);
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
            p.setChecked(alarm.weekdays[weekday]);
            // The first entry of weekDays_Strings is an empty string
            p.setTitle(weekDays_Strings[weekday + 1]);
            p.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    alarm.weekdays[weekday] = (Boolean) o;
                    return true;
                }
            });
            lp.addPreference(p);
        }

        Preference p;

        //noinspection ConstantConditions
        p = findPreference("alarm_device_port");
        p.setEnabled(alarm.id == -1);
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
                        alarm.port = a.getItem(i);

                        findPreference("alarm_device_port").setSummary(alarm.getTargetName());
                        dialog.dismiss();
                    }
                });

                dialog.show();
                return false;
            }
        });

        if (alarm.port != null)
            p.setSummary(alarm.getTargetName());

        CheckBoxPreference cp = ((CheckBoxPreference) m.findPreference("alarm_start_time_enabled"));
        cp.setChecked(alarm.hour_minute_start != -1);
        cp.setOnPreferenceChangeListener(checkEnabledListener);

        cp = ((CheckBoxPreference) m.findPreference("alarm_stop_time_enabled"));
        cp.setChecked(alarm.hour_minute_stop != -1);
        cp.setOnPreferenceChangeListener(checkEnabledListener);

        cp = ((CheckBoxPreference) m.findPreference("alarm_enabled"));
        cp.setChecked(alarm.enabled);

        TimePreference tp = ((TimePreference) m.findPreference("alarm_start_time"));
        if (alarm.hour_minute_start != -1) {
            tp.setTime(alarm.time(alarm.hour_minute_start));
        }

        tp = ((TimePreference) m.findPreference("alarm_stop_time"));
        if (alarm.hour_minute_stop != -1) {
            tp.setTime(alarm.time(alarm.hour_minute_stop));
        }

        DatePreference dp = ((DatePreference) m.findPreference("alarm_date"));
        if (alarm.type == Alarm.TYPE_ONCE && alarm.absolute_date != null) {
            dp.setDate(alarm.absolute_date);
        } else
            dp.setDate(new Date());

        cp = (CheckBoxPreference) m.findPreference("alarm_type_absolute");
        cp.setOnPreferenceChangeListener(checkEnabledListener);
        cp.setChecked(alarm.type == Alarm.TYPE_ONCE);

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

    public void setParameter(Alarm alarm) {
        this.alarm = alarm;
        if (this.alarm == null) {
            this.alarm = new Alarm();
        }
    }

    private void removeAlarm() {
        PluginInterface plugin = alarm.port.device.getPluginInterface(NetpowerctrlService.getService());
        plugin.removeAlarm(alarm, this);
    }

    private void saveAlarm() {
        PreferenceManager m = getPreferenceManager();

        // Fill in data
        alarm.type = ((CheckBoxPreference) m.findPreference("alarm_type_absolute")).isChecked()
                ? Alarm.TYPE_ONCE : Alarm.TYPE_RANGE_ON_WEEKDAYS;
        if (!((CheckBoxPreference) m.findPreference("alarm_start_time_enabled")).isChecked())
            alarm.hour_minute_start = -1;
        else {
            Calendar c = ((TimePreference) m.findPreference("alarm_start_time")).getTime();
            alarm.hour_minute_start = c.get(Calendar.HOUR) * 60 + c.get(Calendar.MINUTE);
        }
        if (!((CheckBoxPreference) m.findPreference("alarm_stop_time_enabled")).isChecked())
            alarm.hour_minute_stop = -1;
        else {
            Calendar c = ((TimePreference) m.findPreference("alarm_stop_time")).getTime();
            alarm.hour_minute_stop = c.get(Calendar.HOUR) * 60 + c.get(Calendar.MINUTE);
        }
        alarm.enabled = ((CheckBoxPreference) m.findPreference("alarm_enabled")).isChecked();

        // Check input data
        if (alarm.port == null || alarm.port.uuid == null) {
            Toast.makeText(getActivity(), R.string.alarm_no_target, Toast.LENGTH_SHORT).show();
            return;
        }

        alarm.port_id = alarm.port.uuid;

        // Check input data
        if (alarm.hour_minute_start == -1 && alarm.hour_minute_stop == -1) {
            Toast.makeText(getActivity(), R.string.alarm_no_start_no_stop, Toast.LENGTH_SHORT).show();
            return;
        }

        PluginInterface plugin = alarm.port.device.getPluginInterface(NetpowerctrlService.getService());
        // Find free device alarm, if not already assigned
        if (alarm.id == -1) {
            Alarm found_alarm;
            found_alarm = plugin.getNextFreeAlarm(alarm.port, alarm.type);

            if (found_alarm == null) {
                Toast.makeText(getActivity(), R.string.alarm_no_device_alarm, Toast.LENGTH_LONG).show();
                return;
            }

            alarm.id = found_alarm.id;
            alarm.deviceAlarm = true;
        }
        plugin.saveAlarm(alarm, this);
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