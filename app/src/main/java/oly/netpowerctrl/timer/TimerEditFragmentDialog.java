package oly.netpowerctrl.timer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.DateFormatSymbols;

import oly.netpowerctrl.R;
import oly.netpowerctrl.device_ports.DevicePort;
import oly.netpowerctrl.device_ports.DevicePortSourceConfigured;
import oly.netpowerctrl.device_ports.DevicePortsListAdapter;
import oly.netpowerctrl.listen_service.PluginInterface;
import oly.netpowerctrl.network.AsyncRunnerResult;
import oly.netpowerctrl.utils.controls.ActivityWithIconCache;

public class TimerEditFragmentDialog extends DialogFragment implements AsyncRunnerResult {
    private Timer timer = new Timer();
    private View rootView;
    private View titleView;

    public TimerEditFragmentDialog() {
    }

    public void setParameter(Timer timer) {
        if (timer != null) {
            this.timer = timer;
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        rootView = getActivity().getLayoutInflater().inflate(R.layout.fragment_alarm, null);

        // Port selection
        {
            DevicePortSourceConfigured s = new DevicePortSourceConfigured();
            final DevicePortsListAdapter adapter = new DevicePortsListAdapter(getActivity(), false, s,
                    ((ActivityWithIconCache) getActivity()).getIconCache(), false);

            Spinner spinner = ((Spinner) rootView.findViewById(R.id.alarm_port));
            // Only enable device port selection if this is a new alarm
            spinner.setEnabled(timer.id == -1);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    timer.port = adapter.getDevicePort(i);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                    timer.port = null;
                }
            });
            spinner.setAdapter(adapter);
            spinner.setSelection(adapter.indexOf(timer.port));
        }

        // Add weekdays
        String[] weekDays_Strings = DateFormatSymbols.getInstance().getShortWeekdays();
        RelativeLayout layout = (RelativeLayout) rootView.findViewById(R.id.layout);
        int lastID = R.id.alarm_weekdays;
        for (int i = 0; i < 7; ++i) {
            final int weekday = i;
            CheckBox p = new CheckBox(getActivity());
            p.setTag(weekday);
            p.setChecked(timer.weekdays[weekday]);
            // The first entry of weekDays_Strings is an empty string
            p.setText(weekDays_Strings[weekday + 1]);
            p.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    timer.weekdays[weekday] = b;
                }
            });
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            lp.addRule(RelativeLayout.BELOW, lastID);
            p.setId(lastID + 1);
            ++lastID;
            layout.addView(p, lp);
        }

        {
            final TimePicker sp = ((TimePicker) rootView.findViewById(R.id.alarm_start_time));
            final CheckBox cp = ((CheckBox) rootView.findViewById(R.id.alarm_start_enabled));
            cp.setChecked(timer.hour_minute_start != -1);
            sp.setVisibility(timer.hour_minute_start != -1 ? View.VISIBLE : View.GONE);
            cp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    sp.setVisibility(b ? View.VISIBLE : View.GONE);
                }
            });
            if (timer.hour_minute_start != -1) {
                sp.setCurrentHour(Timer.getHour(timer.hour_minute_start));
                sp.setCurrentMinute(Timer.getMinute(timer.hour_minute_start));
            }
        }

        {
            final TimePicker sp = ((TimePicker) rootView.findViewById(R.id.alarm_stop_time));
            final CheckBox cp = ((CheckBox) rootView.findViewById(R.id.alarm_stop_enabled));
            cp.setChecked(timer.hour_minute_stop != -1);
            sp.setVisibility(timer.hour_minute_stop != -1 ? View.VISIBLE : View.GONE);
            cp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    sp.setVisibility(b ? View.VISIBLE : View.GONE);
                }
            });
            if (timer.hour_minute_stop != -1) {
                sp.setCurrentHour(Timer.getHour(timer.hour_minute_stop));
                sp.setCurrentMinute(Timer.getMinute(timer.hour_minute_stop));
            }
        }

        // Helper
        rootView.findViewById(R.id.alarm_port_help_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(view.getContext(), R.string.alarm_device_name_summary, Toast.LENGTH_LONG).show();
            }
        });
        rootView.findViewById(R.id.alarm_start_help_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(view.getContext(), R.string.alarm_start_time_summary, Toast.LENGTH_LONG).show();
            }
        });
        rootView.findViewById(R.id.alarm_stop_help_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(view.getContext(), R.string.alarm_stop_time_summary, Toast.LENGTH_LONG).show();
            }
        });
        rootView.findViewById(R.id.alarm_weekdays_help_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(view.getContext(), R.string.alarm_weekdays_summary, Toast.LENGTH_LONG).show();
            }
        });

        titleView = getActivity().getLayoutInflater().inflate(R.layout.dialog_device_preferences_title, null);
        CheckBox checkBox = ((CheckBox) titleView.findViewById(android.R.id.title));
        checkBox.setText("Alarm aktiv");
        checkBox.setChecked(timer.enabled);

        ((TextView) titleView.findViewById(R.id.device_name)).setText(timer.id == -1 ? R.string.alarm_add : R.string.alarm_edit);


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCustomTitle(titleView)
                .setView(rootView)
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(R.string.remove, null)
                .setNegativeButton(android.R.string.cancel, null);
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();    //super.onStart() is where dialog.show() is actually called on the underlying dialog, so we have to do it after this point
        AlertDialog d = (AlertDialog) getDialog();
        d.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAlarm();
            }
        });
        d.getButton(Dialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        Button btn = d.getButton(Dialog.BUTTON_NEUTRAL);
        btn.setVisibility(timer.id == -1 ? View.GONE : View.VISIBLE);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeAlarm();
            }
        });
    }


    private void removeAlarm() {
        PluginInterface plugin = timer.port.device.getPluginInterface();
        plugin.removeAlarm(timer, this);
    }

    private void saveAlarm() {
        // Fill in data
        timer.type = Timer.TYPE_RANGE_ON_WEEKDAYS;
        if (!((CheckBox) rootView.findViewById(R.id.alarm_start_enabled)).isChecked())
            timer.hour_minute_start = -1;
        else {
            TimePicker c = ((TimePicker) rootView.findViewById(R.id.alarm_start_time));
            timer.hour_minute_start = c.getCurrentHour() * 60 + c.getCurrentMinute();
        }
        if (!((CheckBox) rootView.findViewById(R.id.alarm_stop_enabled)).isChecked())
            timer.hour_minute_stop = -1;
        else {
            TimePicker c = ((TimePicker) rootView.findViewById(R.id.alarm_stop_time));
            timer.hour_minute_stop = c.getCurrentHour() * 60 + c.getCurrentMinute();
        }
        timer.enabled = ((CheckBox) titleView.findViewById(android.R.id.title)).isChecked();

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
        dismiss();
    }

    @Override
    public void asyncRunnerStart(@SuppressWarnings("UnusedParameters") DevicePort oi) {
        Toast.makeText(getActivity(), "Bitte warten. Speichere Alarm...", Toast.LENGTH_SHORT).show();
    }
}
