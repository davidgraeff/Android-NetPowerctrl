package oly.netpowerctrl.timer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Paint;
import android.text.Html;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import oly.netpowerctrl.R;
import oly.netpowerctrl.utils_gui.AnimationController;

/**
 * List all alarms of the timer controller
 */
public class TimerAdapter extends BaseAdapter implements TimerController.IAlarmsUpdated {
    private final TimerController controller;
    private LayoutInflater inflater;
    private Context context;
    private WeakReference<AnimationController> removeAnimationWeakReference = new WeakReference<>(null);

    public TimerAdapter(Context context, TimerController timerController) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        this.controller = timerController;
    }

    public void setRemoveAnimation(AnimationController animationController) {
        removeAnimationWeakReference = new WeakReference<>(animationController);
    }

    public void remove(int position) {
        AnimationController a = removeAnimationWeakReference.get();
        if (a != null)
            a.beforeRemoval(position);
        controller.removeFromCache(position);
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        AnimationController a = removeAnimationWeakReference.get();
        if (a != null)
            a.animate();
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
        return Timer.TYPES;
    }

    @Override
    public int getItemViewType(int position) {
        Timer data = controller.getItem(position);
        return data.type;
    }

    @SuppressLint("InflateParams")
    public View getView(int position, View convertView, ViewGroup parent) {
        Timer timer = controller.getItem(position);

        if (convertView == null) {
            if (timer.type == Timer.TYPE_RANGE_ON_WEEKDAYS || timer.type == Timer.TYPE_RANGE_ON_RANDOM_WEEKDAYS)
                convertView = inflater.inflate(R.layout.alarm_range_weekdays, null);
            else if (timer.type == Timer.TYPE_ONCE)
                convertView = inflater.inflate(R.layout.alarm_once, null);
        }

        assert convertView != null;

        if (timer.type == Timer.TYPE_RANGE_ON_WEEKDAYS ||
                timer.type == Timer.TYPE_RANGE_ON_RANDOM_WEEKDAYS) {
            // Port name, device name
            TextView txt = (TextView) convertView.findViewById(R.id.alarm_target);
            txt.setText(timer.getTargetName());

            ImageView image = (ImageView) convertView.findViewById(R.id.alarm_image);
            if (timer.fromCache)
                image.setImageResource(android.R.drawable.presence_offline);
            else if (timer.enabled)
                image.setImageResource(android.R.drawable.presence_online);
            else
                image.setImageResource(android.R.drawable.presence_busy);

            // weekdays
            txt = (TextView) convertView.findViewById(R.id.alarm_weekdays);
            txt.setText(timer.days());

            // start+stop time
            txt = (TextView) convertView.findViewById(R.id.alarm_time);
            txt.setText(Html.fromHtml(context.getString(R.string.alarm_switch_on_off,
                    timer.time(timer.hour_minute_start),
                    timer.time(timer.hour_minute_stop))));

            txt = (TextView) convertView.findViewById(R.id.alarm_random);
            if (timer.type == Timer.TYPE_RANGE_ON_RANDOM_WEEKDAYS) {
                // random time
                txt.setText(Html.fromHtml(context.getString(R.string.alarm_toggle_random,
                        timer.time(timer.hour_minute_random_interval))));
                txt.setVisibility(View.VISIBLE);
            } else {
                txt.setVisibility(View.GONE);
            }

        } else if (timer.type == Timer.TYPE_ONCE) {
            // Port name, device name
            TextView txt = (TextView) convertView.findViewById(R.id.alarm_target);
            txt.setText(timer.getTargetName());
            if (timer.enabled)
                txt.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.presence_online, 0, 0, 0);
            else
                txt.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.presence_busy, 0, 0, 0);

            // date
            txt = (TextView) convertView.findViewById(R.id.alarm_date);
            txt.setText(DateFormat.getDateFormat(context).format(timer.absolute_date));

            // start time
            txt = (TextView) convertView.findViewById(R.id.alarm_start);
            txt.setText(timer.time(timer.hour_minute_start));
            txt.setPaintFlags(txt.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

            // stop time
            txt = (TextView) convertView.findViewById(R.id.alarm_stop);
            txt.setText(timer.time(timer.hour_minute_stop));
            txt.setPaintFlags(txt.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        }

        return convertView;
    }

    @Override
    public boolean alarmsUpdated(boolean addedOrRemoved, boolean inProgress) {
        notifyDataSetChanged();
        return true;
    }

    public Timer getAlarm(int position) {
        return controller.getItem(position);
    }


}
