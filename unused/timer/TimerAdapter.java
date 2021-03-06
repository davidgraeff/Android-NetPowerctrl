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
import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.utils.ObserverUpdateActions;
import oly.netpowerctrl.utils.onCollectionUpdated;

/**
 * List all alarms of the timer controller
 */
public class TimerAdapter extends RecyclerView.Adapter<TimerAdapter.ViewHolder> implements onCollectionUpdated<TimerCollection, Timer> {
    private final Context context;
    private final int resImageFromCache;
    private final int resImageOn;
    private final int resImageOff;
    private final int resImageToggle;
    private TimerCollection controller = null;
    private List<Timer> timers = new ArrayList<>();
    private Executable executable;

    /**
     * You need to call {#link start} to start this adapter.
     *
     * @param context A context
     */
    public TimerAdapter(Context context) {
        this.context = context;

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

    public void start(TimerCollection timerCollection, Executable executable) {
        this.controller = timerCollection;
        this.executable = executable;
        controller.registerObserver(this);
        controller.fillItems(executable, timers);
        controller.refresh(executable);
    }

    public void finish() {
        if (controller != null)
            controller.unregisterObserver(this);
    }

    @Override
    public long getItemId(int position) {
        return timers.get(position).viewID;
    }

    @Override
    public int getItemCount() {
        return timers.size();
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
        Timer timer = timers.get(position);
        holder.target.setText(timer.getTargetName());

        if (timer.isFromCache())
            holder.image.setImageResource(resImageFromCache);
        else if (!timer.enabled)
            holder.image.setImageResource(android.R.drawable.presence_busy);
        else if (timer.command == Executable.ON)
            holder.image.setImageResource(resImageOn);
        else if (timer.command == Executable.OFF)
            holder.image.setImageResource(resImageOff);
        else if (timer.command == Executable.TOGGLE)
            holder.image.setImageResource(resImageToggle);

        holder.image_android.setVisibility(timer.alarmOnDevice != null ? View.INVISIBLE : View.VISIBLE);

        if (timer.type == Timer.TYPE_RANGE_ON_WEEKDAYS ||
                timer.type == Timer.TYPE_RANGE_ON_RANDOM_WEEKDAYS) {

            holder.weekdays.setText(timer.days());

            // Time
            String text;
            switch (timer.command) {
                case Executable.OFF:
                    text = context.getString(R.string.alarm_switch_off,
                            Timer.time(timer.hour_minute));
                    break;
                case Executable.ON:
                    text = context.getString(R.string.alarm_switch_on,
                            Timer.time(timer.hour_minute));
                    break;
                case Executable.TOGGLE:
                    text = context.getString(R.string.alarm_toggle_random,
                            Timer.time(timer.hour_minute));
                    break;
                default:
                    text = "";
            }
            holder.alarmtime.setText(Html.fromHtml(text));

        } else if (timer.type == Timer.TYPE_ONCE) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            int res = timer.command == Executable.ON ? R.string.alarm_switch_on : R.string.alarm_switch_off;
            holder.alarmtime.setText(Html.fromHtml(context.getString(res, sdf.format(timer.absolute_date))));
        }
    }

    @Override
    public int getItemViewType(int position) {
        Timer data = timers.get(position);
        return data.type;
    }

    public Timer getAlarm(int position) {
        return timers.get(position);
    }

    @Override
    public boolean updated(@NonNull TimerCollection timerCollection, Timer timer, @NonNull ObserverUpdateActions action) {
        timers.clear();
        controller.fillItems(executable, timers);
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
