package oly.netpowerctrl.timer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.device_base.device.DevicePort;

/**
 * List all alarms of the timer controller
 */
public class TimerAdapter extends RecyclerView.Adapter<TimerAdapter.ViewHolder> implements onCollectionUpdated<TimerCollection, Timer> {
    private final TimerCollection controller;
    private final Context context;
    private final int resImageFromCache;
    private final int resImageOn;
    private final int resImageOff;
    private final int resImageToggle;

    public TimerAdapter(Context context, TimerCollection timerCollection) {
        this.context = context;
        this.controller = timerCollection;

        TypedValue typedvalueattr = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.ic_action_download_themed, typedvalueattr, true);
        resImageFromCache = typedvalueattr.resourceId;
        context.getTheme().resolveAttribute(R.attr.ic_action_flash_on_themed, typedvalueattr, true);
        resImageOn = typedvalueattr.resourceId;
        context.getTheme().resolveAttribute(R.attr.ic_action_flash_off_themed, typedvalueattr, true);
        resImageOff = typedvalueattr.resourceId;
        context.getTheme().resolveAttribute(R.attr.ic_action_shuffle_themed, typedvalueattr, true);
        resImageToggle = typedvalueattr.resourceId;
    }

    public void start() {
        controller.registerObserver(this);
    }

    public void finish() {
        controller.unregisterObserver(this);
    }

    @Override
    public long getItemId(int position) {
        return controller.get(position).viewID;
    }

    @Override
    public int getItemCount() {
        return controller.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == Timer.TYPE_RANGE_ON_WEEKDAYS || viewType == Timer.TYPE_RANGE_ON_RANDOM_WEEKDAYS)
            return new ViewHolder(inflater.inflate(R.layout.list_item_alarm_range_weekdays, parent, false));
        else if (viewType == Timer.TYPE_ONCE)
            return new ViewHolder(inflater.inflate(R.layout.list_item_alarm_once, parent, false));
        throw new RuntimeException("onCreateViewHolder: type not known!");
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Timer timer = controller.get(position);
        holder.target.setText(timer.getTargetName());

        if (timer.isFromCache())
            holder.image.setImageResource(resImageFromCache);
        else if (!timer.enabled)
            holder.image.setImageResource(android.R.drawable.presence_busy);
        else if (timer.command == DevicePort.ON)
            holder.image.setImageResource(resImageOn);
        else if (timer.command == DevicePort.OFF)
            holder.image.setImageResource(resImageOff);
        else if (timer.command == DevicePort.TOGGLE)
            holder.image.setImageResource(resImageToggle);

        holder.image_android.setVisibility(timer.alarmOnDevice != null ? View.INVISIBLE : View.VISIBLE);

        if (timer.type == Timer.TYPE_RANGE_ON_WEEKDAYS ||
                timer.type == Timer.TYPE_RANGE_ON_RANDOM_WEEKDAYS) {

            holder.weekdays.setText(timer.days());

            // Time
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
            holder.alarmtime.setText(Html.fromHtml(text));

        } else if (timer.type == Timer.TYPE_ONCE) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            int res = timer.command == DevicePort.ON ? R.string.alarm_switch_on : R.string.alarm_switch_off;
            holder.alarmtime.setText(Html.fromHtml(context.getString(res, sdf.format(timer.absolute_date))));
        }
    }

    @Override
    public int getItemViewType(int position) {
        Timer data = controller.get(position);
        return data.type;
    }

    public Timer getAlarm(int position) {
        return controller.get(position);
    }

    @Override
    public boolean updated(@NonNull TimerCollection timerCollection, Timer timer, @NonNull ObserverUpdateActions action, int position) {
        notifyDataSetChanged();
        return true;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        ImageView image_android;
        TextView target;
        TextView weekdays;
        TextView alarmtime;

        public ViewHolder(View itemView) {
            super(itemView);
            image = (ImageView) itemView.findViewById(R.id.alarm_image);
            image_android = (ImageView) itemView.findViewById(R.id.alarm_image_isAndroid);
            target = (TextView) itemView.findViewById(R.id.alarm_target);
            weekdays = (TextView) itemView.findViewById(R.id.alarm_weekdays);
            alarmtime = (TextView) itemView.findViewById(R.id.alarm_time);
        }
    }
}
