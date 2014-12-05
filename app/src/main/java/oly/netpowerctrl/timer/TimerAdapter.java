package oly.netpowerctrl.timer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.device_base.device.DevicePort;

/**
 * List all alarms of the timer controller
 */
public class TimerAdapter extends BaseAdapter implements onCollectionUpdated<TimerCollection, Timer> {
    private final TimerCollection controller;
    private final LayoutInflater inflater;
    private final Context context;

    public TimerAdapter(Context context, TimerCollection timerCollection) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        this.controller = timerCollection;
    }

    public void start() {
        controller.registerObserver(this);
    }

    public void finish() {
        controller.unregisterObserver(this);
    }

    @Override
    public int getCount() {
        return controller.size();
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public Object getItem(int position) {
        return controller.get(position);
    }

    @Override
    public long getItemId(int position) {
        return controller.get(position).viewID;
    }

    @Override
    public int getViewTypeCount() {
        return Timer.TYPES;
    }

    @Override
    public int getItemViewType(int position) {
        Timer data = controller.get(position);
        return data.type;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        Timer timer = controller.get(position);

        if (convertView == null) {
            if (timer.type == Timer.TYPE_RANGE_ON_WEEKDAYS || timer.type == Timer.TYPE_RANGE_ON_RANDOM_WEEKDAYS)
                convertView = inflater.inflate(R.layout.list_item_alarm_range_weekdays, parent, false);
//            else if (timer.type == Timer.TYPE_ONCE)
//                convertView = inflater.inflate(R.layout.alarm_once, null);
        }

        assert convertView != null;

        if (timer.type == Timer.TYPE_RANGE_ON_WEEKDAYS ||
                timer.type == Timer.TYPE_RANGE_ON_RANDOM_WEEKDAYS) {
            // Port name, device name
            TextView txt = (TextView) convertView.findViewById(R.id.alarm_target);
            txt.setText(timer.getTargetName());

            ImageView image = (ImageView) convertView.findViewById(R.id.alarm_image);
            if (timer.isFromCache())
                image.setImageResource(android.R.drawable.presence_offline);
            else if (!timer.enabled)
                image.setImageResource(android.R.drawable.presence_busy);
            else if (timer.command == DevicePort.ON)
                image.setImageResource(android.R.drawable.presence_online);
            else if (timer.command == DevicePort.OFF)
                image.setImageResource(android.R.drawable.presence_online);
            else if (timer.command == DevicePort.TOGGLE)
                image.setImageResource(android.R.drawable.presence_video_online);

            image = (ImageView) convertView.findViewById(R.id.alarm_image_isAndroid);
            image.setVisibility(timer.alarmOnDevice != null ? View.INVISIBLE : View.VISIBLE);

            // Weekdays or Absolute Date
            txt = (TextView) convertView.findViewById(R.id.alarm_weekdays);
            if (timer.type == Timer.TYPE_ONCE) {
                txt.setText(timer.absolute_date.toString());
            } else
                txt.setText(timer.days());

            // Time
            txt = (TextView) convertView.findViewById(R.id.alarm_time);
            String text;
            switch (timer.command) {
                case DevicePort.OFF:
                    text = context.getString(R.string.alarm_switch_off,
                            Timer.time(timer.hour_minute));
                    break;
                case DevicePort.ON:
                    text = context.getString(R.string.alarm_switch_on,
                            Timer.time(timer.hour_minute));
                    break;
                case DevicePort.TOGGLE:
                    text = context.getString(R.string.alarm_toggle_random,
                            Timer.time(timer.hour_minute));
                    break;
                default:
                    text = "";
            }
            txt.setText(Html.fromHtml(text));

//        } else if (timer.type == Timer.TYPE_ONCE) {
//            // Port name, device name
//            TextView txt = (TextView) convertView.findViewById(R.id.alarm_target);
//            txt.setText(timer.getTargetName());
//            if (timer.enabled)
//                txt.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.presence_online, 0, 0, 0);
//            else
//                txt.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.presence_busy, 0, 0, 0);
//
//            // date
//            txt = (TextView) convertView.findViewById(R.id.alarm_date);
//            txt.setText(DateFormat.getDateFormat(context).format(timer.absolute_date));
//
//            // start time
//            txt = (TextView) convertView.findViewById(R.id.alarm_start);
//            txt.setText(Timer.time(timer.hour_minute));
//            txt.setPaintFlags(txt.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
//
//            // stop time
//            txt = (TextView) convertView.findViewById(R.id.alarm_stop);
//            txt.setText(Timer.time(timer.hour_minute_stop));
//            txt.setPaintFlags(txt.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        }

        return convertView;
    }

    public Timer getAlarm(int position) {
        return controller.get(position);
    }


    @Override
    public boolean updated(@NonNull TimerCollection timerCollection, Timer timer, @NonNull ObserverUpdateActions action, int position) {
        notifyDataSetChanged();
        return true;
    }
}
