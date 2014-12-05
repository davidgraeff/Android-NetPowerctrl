package oly.netpowerctrl.timer;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.device_base.executables.Executable;
import oly.netpowerctrl.executables.ExecutablesSourceDevicePorts;
import oly.netpowerctrl.network.onHttpRequestResult;
import oly.netpowerctrl.pluginservice.PluginInterface;
import oly.netpowerctrl.pluginservice.PluginService;
import oly.netpowerctrl.scenes.Scene;
import oly.netpowerctrl.ui.notifications.InAppNotifications;

public class TimerEditFragmentDialog extends DialogFragment implements onHttpRequestResult {
    int commandBefore;
    private Timer timer = Timer.createNewTimer();
    private View rootView;
    private View titleView;
    private Toast toast;
    private Spinner spinner;
    private boolean isNew = true;
    private boolean is_android_alarm;
    private List<Executable> executables = new ArrayList<>();
    private boolean willDelete = false;

    public TimerEditFragmentDialog() {
    }

    public void setParameter(Timer timer) {
        if (timer != null) {
            this.timer = timer;
            isNew = false;
        }
    }

    int indexOfExecutable(String executable_uid) {
        for (int i = 0; i < executables.size(); ++i)
            if (executables.get(i).getUid().equals(executable_uid))
                return i;
        return -1;
    }

    private void changeExecutableList(boolean deviceAlarm) {
        final ExecutablesSourceDevicePorts s = new ExecutablesSourceDevicePorts(null);
        s.fullUpdate(null);

        executables.clear();

        for (int i = 0; i < s.getDevicePortList().size(); ++i)
            executables.add(s.getDevicePortList().get(i));

        if (!deviceAlarm) {
            for (Scene scene : AppData.getInstance().sceneCollection.getItems()) {
                executables.add(scene);
            }
        }

        ArrayAdapter<String> array_adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1);
        for (Executable executable : executables) array_adapter.add(executable.getTitle());

        spinner.setAdapter(array_adapter);
        spinner.setSelection(indexOfExecutable(timer.executable_uid));
    }

    @SuppressLint({"InflateParams", "ShowToast"})
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        rootView = getActivity().getLayoutInflater().inflate(R.layout.fragment_alarm_edit, null);

        toast = Toast.makeText(getActivity(), "", Toast.LENGTH_SHORT);

        // Port selection
        {
            spinner = ((Spinner) rootView.findViewById(R.id.alarm_port));

            // Only enable device port selection if this is a new alarm
            spinner.setEnabled(isNew);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    timer.executable = executables.get(i);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                    timer.executable = null;
                }
            });
        }

        changeExecutableList(timer.alarmOnDevice != null);

        CheckBox androidAlarm = ((CheckBox) rootView.findViewById(R.id.alarm_on_android));
        androidAlarm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                changeExecutableList(!b);
            }
        });
        androidAlarm.setEnabled(isNew);
        androidAlarm.setChecked(timer.alarmOnDevice == null);

        // Add weekdays
        String[] weekDays_Strings = DateFormatSymbols.getInstance().getShortWeekdays();
        com.wefika.flowlayout.FlowLayout layout = (com.wefika.flowlayout.FlowLayout) rootView.findViewById(R.id.weekday_layout);
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
            com.wefika.flowlayout.FlowLayout.LayoutParams lp = new com.wefika.flowlayout.FlowLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(5, 2, 5, 2);
            p.setId(lastID + 1);
            ++lastID;
            layout.addView(p, lp);
        }

        {
            final CheckBox cp = ((CheckBox) rootView.findViewById(R.id.alarm_is_switch_on));
            cp.setChecked(timer.command == DevicePort.ON);
        }

        {
            final TimePicker sp = ((TimePicker) rootView.findViewById(R.id.alarm_start_time));
            sp.setIs24HourView(true);
            if (timer.hour_minute != -1) {
                sp.setCurrentHour(Timer.getHour(timer.hour_minute));
                sp.setCurrentMinute(Timer.getMinute(timer.hour_minute));
            }
        }

        // Helper
        rootView.findViewById(R.id.alarm_port_help_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toast.setText(R.string.alarm_device_name_summary);
                InAppNotifications.moveToastNextToView(toast, getResources(), view, false);
                toast.show();
            }
        });
        rootView.findViewById(R.id.alarm_on_android_help_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toast.setText(R.string.alarm_on_android_summary);
                InAppNotifications.moveToastNextToView(toast, getResources(), view, false);
                toast.show();
            }
        });
        rootView.findViewById(R.id.alarm_start_help_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toast.setText(R.string.alarm_start_time_summary);
                InAppNotifications.moveToastNextToView(toast, getResources(), view, false);
                toast.show();
            }
        });
        rootView.findViewById(R.id.alarm_weekdays_help_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toast.setText(R.string.alarm_weekdays_summary);
                InAppNotifications.moveToastNextToView(toast, getResources(), view, false);
                toast.show();
            }
        });

        titleView = getActivity().getLayoutInflater().inflate(R.layout.dialog_title_device_edit, null);
        CheckBox checkBox = ((CheckBox) titleView.findViewById(android.R.id.title));
        checkBox.setText(R.string.alarm_enabled);
        checkBox.setChecked(timer.enabled);

        ((TextView) titleView.findViewById(R.id.device_name)).setText(isNew ? R.string.alarm_add : R.string.alarm_edit);


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
        btn.setVisibility(isNew ? View.GONE : View.VISIBLE);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                willDelete = true;
                AppData.getInstance().timerCollection.removeAlarm(timer, TimerEditFragmentDialog.this);
            }
        });
    }

    private void saveAlarm() {
        // Fill in data
        timer.type = Timer.TYPE_RANGE_ON_WEEKDAYS;
        commandBefore = timer.command;
        timer.command = ((CheckBox) rootView.findViewById(R.id.alarm_is_switch_on)).isChecked() ? DevicePort.ON : DevicePort.OFF;

        TimePicker c = ((TimePicker) rootView.findViewById(R.id.alarm_start_time));
        timer.hour_minute = c.getCurrentHour() * 60 + c.getCurrentMinute();

        timer.enabled = ((CheckBox) titleView.findViewById(android.R.id.title)).isChecked();

        // Check input data
        if (timer.executable == null || timer.executable.getUid() == null) {
            Toast.makeText(getActivity(), R.string.alarm_no_target, Toast.LENGTH_SHORT).show();
            return;
        }

        timer.executable_uid = timer.executable.getUid();

        // Check input data
        if (timer.hour_minute == -1) {
            Toast.makeText(getActivity(), R.string.alarm_no_start_no_stop, Toast.LENGTH_SHORT).show();
            return;
        }

        is_android_alarm = ((CheckBox) rootView.findViewById(R.id.alarm_on_android)).isChecked();
        if (is_android_alarm) {
            AppData.getInstance().timerCollection.addAlarm(timer);
            dismiss();
        } else {
            DevicePort devicePort = (DevicePort) timer.executable;
            PluginService.getService().wakeupPlugin(devicePort.device);
            PluginInterface plugin = (PluginInterface) devicePort.device.getPluginInterface();

            // Find free device alarm, if not already assigned
            if (isNew || commandBefore != timer.command) {
                Timer found_timer;
                found_timer = plugin.getNextFreeAlarm(devicePort, timer.type, timer.command);
                if (found_timer == null) {
                    Toast.makeText(getActivity(), R.string.alarm_no_device_alarm, Toast.LENGTH_LONG).show();
                    return;
                }

                timer.alarmOnDevice = found_timer.alarmOnDevice;
            }
            // Save alarm
            plugin.saveAlarm(timer, this);
        }
    }

    @Override
    public void httpRequestResult(DevicePort oi, boolean success, String error_message) {
        if (!success) {
            Toast.makeText(getActivity(), error_message, Toast.LENGTH_SHORT).show();
        } else if (!willDelete) {
            // If command has changed, a new alarm will be used. We have to remove the old one now.
            if (!is_android_alarm && commandBefore != timer.command && !isNew) {
//                int temp = timer.command;
//                timer.command = commandBefore;
//                willDelete = true;
//                DevicePort devicePort = (DevicePort) timer.executable;
//                PluginInterface plugin = (PluginInterface) devicePort.device.getPluginInterface();
//                plugin.removeAlarm(timer, this);
//                timer.command = temp;
                dismiss();
            } else
                dismiss();
        } else { // willDelete = true

            dismiss();
        }
    }

    @Override
    public void httpRequestStart(@SuppressWarnings("UnusedParameters") DevicePort oi) {
        if (willDelete)
            Toast.makeText(getActivity(), R.string.alarm_wait_remove, Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(getActivity(), R.string.alarm_wait_save, Toast.LENGTH_SHORT).show();
    }
}
