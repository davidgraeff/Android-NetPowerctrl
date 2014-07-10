package oly.netpowerctrl.device_ports;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.devices.DevicePort;
import oly.netpowerctrl.utils.IconDeferredLoadingThread;

public class DevicePortsListAdapter extends DevicePortsBaseAdapter {
    private final SparseBooleanArray checked = new SparseBooleanArray();
    private boolean checkable;

    public DevicePortsListAdapter(Context context, boolean checkable, DevicePortSource source,
                                  IconDeferredLoadingThread iconCache) {
        super(context, null, source, iconCache, true);
        this.checkable = checkable;
        if (checkable)
            setLayoutRes(R.layout.selectable_outlet_list_item);
        else
            setLayoutRes(R.layout.available_outlet_list_item);
        if (source != null)
            source.updateNow();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DevicePortListItem item = mItems.get(position);
        DevicePort port = item.port;

        convertView = super.getView(position, convertView, parent);

        // Not our business, if port is null
        if (port == null)
            return convertView;

        if (checkable) {
            CheckedTextView t = (CheckedTextView) mCurrent_devicePortViewHolder.title;
            t.setChecked(checked.get(position));
        }
        return convertView;
    }

    public void toggleItemChecked(int position) {
        checked.put(position, !checked.get(position));
        notifyDataSetChanged();
    }


    public List<UUID> getCheckedUUids() {
        List<UUID> slaves = new ArrayList<>();
        for (int i = 0; i < getCount(); i++) {
            if (checked.get(i)) {
                slaves.add(getItem(i).uuid);
            }
        }
        return slaves;
    }

    public void setChecked(List<UUID> slaves) {
        for (UUID slave : slaves) {
            int position = findIndexByUUid(slave);
            if (position == -1)
                continue;
            checked.put(position, true);
        }
    }
}
