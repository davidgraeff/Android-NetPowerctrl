package oly.netpowerctrl.listadapter;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;

/**
 * Allows a set of device ports to be selected
 */
public class DevicePortsSelectAdapter extends DevicePortsBaseAdapter {
    public DevicePortsSelectAdapter(Context context) {
        super(context, null);
    }

    private final SparseBooleanArray checked = new SparseBooleanArray();

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        outlet_res_id = R.layout.selectable_outlet_list_item;
        View v = super.getView(position, convertView, parent);
        CheckedTextView t = (CheckedTextView) current_viewHolder.title;
        t.setChecked(checked.get(position));
        return v;
    }

    public void toggleItemChecked(int position) {
        checked.put(position, !checked.get(position));
        notifyDataSetChanged();
    }


    public List<UUID> getCheckedUUids() {
        List<UUID> slaves = new ArrayList<UUID>();
        for (int i = 0; i < getCount(); i++) {
            if (checked.get(i)) {
                slaves.add(getItem(i).uuid);
            }
        }
        return slaves;
    }

    public void setChecked(List<UUID> slaves) {
        for (UUID slave : slaves) {
            int position = getItemPositionByUUid(slave);
            if (position == -1)
                continue;
            checked.put(position, true);
        }
    }
}
