package oly.netpowerctrl.devices;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import java.util.Iterator;

import oly.netpowerctrl.R;

public class DevicePortsAvailableAdapter extends DevicePortsBaseAdapter {
    public DevicePortsAvailableAdapter(Context context) {
        super(context, null);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        outlet_res_id = R.layout.available_outlet_list_item;
        return super.getView(position, convertView, parent);
    }

    public void removeAll(DevicePortsBaseAdapter adapter) {
        for (DevicePortListItem outlet_info : adapter.all_outlets) {
            Iterator<DevicePortListItem> i = all_outlets.iterator();
            while (i.hasNext()) {
                if (outlet_info.port.equals(i.next().port)) {
                    i.remove();
                }
            }

        }
    }

    public DevicePort getItem(int position) {
        return all_outlets.get(position).port;
    }
}
