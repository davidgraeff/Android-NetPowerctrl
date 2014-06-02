package oly.netpowerctrl.alarms;

import android.content.Context;
import android.graphics.Paint;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import oly.netpowerctrl.R;

/**
 * List all alarms of the timer controller
 */
public class TimerAdapter extends BaseAdapter implements TimerController.IAlarmsUpdated {
    private LayoutInflater inflater;
    private final TimerController controller;
    private Context context;

    public TimerAdapter(Context context, TimerController timerController) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        this.controller = timerController;
    }

    public void start() {
        controller.registerObserver(this);
    }

    public void finish() {
        controller.unregisterObserver(this);
    }

    @Override
    public int getCount() {
        return controller.getCount();
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public Object getItem(int position) {
        return controller.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        return controller.getItem(position).id;
    }

    @Override
    public int getViewTypeCount() {
        return Alarm.TYPES;
    }

    @Override
    public int getItemViewType(int position) {
        Alarm data = controller.getItem(position);
        return data.type;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        Alarm alarm = controller.getItem(position);

        if (convertView == null) {
            if (alarm.type == Alarm.TYPE_RANGE_ON_WEEKDAYS)
                convertView = inflater.inflate(R.layout.alarm_range_weekdays, null);
            else if (alarm.type == Alarm.TYPE_RANGE_ON_RANDOM_WEEKDAYS)
                convertView = inflater.inflate(R.layout.alarm_range_weekdays_random, null);
            else if (alarm.type == Alarm.TYPE_ONCE)
                convertView = inflater.inflate(R.layout.alarm_once, null);
        }

        assert convertView != null;

        if (alarm.type == Alarm.TYPE_RANGE_ON_WEEKDAYS ||
                alarm.type == Alarm.TYPE_RANGE_ON_RANDOM_WEEKDAYS) {
            // Port name, device name
            TextView txt = (TextView) convertView.findViewById(R.id.alarm_target);
            txt.setText(alarm.getTargetName());
            if (alarm.enabled)
                txt.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.presence_online, 0, 0, 0);
            else
                txt.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.presence_busy, 0, 0, 0);

            // weekdays
            txt = (TextView) convertView.findViewById(R.id.alarm_weekdays);
            txt.setText(alarm.days());

            // start time
            txt = (TextView) convertView.findViewById(R.id.alarm_start);
            txt.setText(alarm.time(alarm.hour_minute_start));
            txt.setPaintFlags(txt.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

            // stop time
            txt = (TextView) convertView.findViewById(R.id.alarm_stop);
            txt.setText(alarm.time(alarm.hour_minute_stop));
            txt.setPaintFlags(txt.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

            if (alarm.type == Alarm.TYPE_RANGE_ON_RANDOM_WEEKDAYS) {
                // random time
                txt = (TextView) convertView.findViewById(R.id.alarm_random);
                txt.setText(alarm.time(alarm.hour_minute_random_interval));
                txt.setPaintFlags(txt.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            }

        } else if (alarm.type == Alarm.TYPE_ONCE) {
            // Port name, device name
            TextView txt = (TextView) convertView.findViewById(R.id.alarm_target);
            txt.setText(alarm.getTargetName());
            if (alarm.enabled)
                txt.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.presence_online, 0, 0, 0);
            else
                txt.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.presence_busy, 0, 0, 0);

            // date
            txt = (TextView) convertView.findViewById(R.id.alarm_date);
            txt.setText(DateFormat.getDateFormat(context).format(alarm.absolute_date));

            // start time
            txt = (TextView) convertView.findViewById(R.id.alarm_start);
            txt.setText(alarm.time(alarm.hour_minute_start));
            txt.setPaintFlags(txt.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

            // stop time
            txt = (TextView) convertView.findViewById(R.id.alarm_stop);
            txt.setText(alarm.time(alarm.hour_minute_stop));
            txt.setPaintFlags(txt.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        }

        return convertView;
    }

    @Override
    public boolean alarmsUpdated(boolean addedOrRemoved, boolean inProgress) {
        notifyDataSetChanged();
        return true;
    }

    public Alarm getAlarm(int position) {
        return controller.getItem(position);
    }
}
