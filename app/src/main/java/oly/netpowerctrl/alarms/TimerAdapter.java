package oly.netpowerctrl.alarms;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

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

    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = inflater.inflate(android.R.layout.simple_list_item_1, null);
        }

        Alarm data = controller.getItem(position);

        assert convertView != null;
        TextView tvName = (TextView) convertView.findViewById(android.R.id.text1);
        tvName.setText(data.toString(context));

        return convertView;
    }

    @Override
    public boolean alarmsUpdated(boolean addedOrRemoved, boolean inProgress) {
        notifyDataSetChanged();
        return true;
    }

}
