package oly.netpowerctrl.alarms;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.utils.Icons;

/**
 * List all alarms of the timer controller
 */
public class TimerAdapter extends BaseAdapter implements TimerController.IAlarmsUpdated {
    private final LayoutInflater inflater;
    private final TimerController controller;

    public TimerAdapter(Context context, TimerController controller) {
        inflater = LayoutInflater.from(context);
        this.controller = controller;
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
            convertView = inflater.inflate(R.layout.grid_icon_item, parent);
        }

        Alarm data = controller.getItem(position);

        assert convertView != null;
        TextView tvName = (TextView) convertView.findViewById(R.id.text1);
        tvName.setText(data.name);

        ImageView image = (ImageView) convertView.findViewById(R.id.icon_bitmap);
        if (data.bitmap == null) {
            data.bitmap = Icons.loadIcon(NetpowerctrlApplication.instance, data.uuid,
                    Icons.IconType.GroupIcon, Icons.IconState.StateUnknown, R.drawable.stateon);
        }

        image.setImageBitmap(data.bitmap);
        return convertView;
    }

    @Override
    public void alarmsUpdated(boolean addedOrRemoved) {
        notifyDataSetChanged();
    }
}
