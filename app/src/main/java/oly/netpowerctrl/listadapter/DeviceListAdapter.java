package oly.netpowerctrl.listadapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.utils.MenuConfigureEvent;

public class DeviceListAdapter extends BaseAdapter implements OnClickListener {

    private MenuConfigureEvent menuConfigureEvent = null;

    private List<DeviceInfo> all_devices;
    private LayoutInflater inflater;

    public DeviceListAdapter(Context context, List<DeviceInfo> devices) {
        inflater = LayoutInflater.from(context);
        all_devices = devices;
    }

    public int getCount() {
        return all_devices.size();
    }

    public Object getItem(int position) {
        return all_devices.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null)
            convertView = inflater.inflate(R.layout.device_list_item, null);
        assert convertView != null;

        DeviceInfo di = all_devices.get(position);
        TextView tvName = (TextView) convertView.findViewById(R.id.device_name);
        tvName.setText(di.DeviceName);

        TextView tvIP = (TextView) convertView.findViewById(R.id.device_ip);
        tvIP.setText(di.HostName);

        ImageButton btn = (ImageButton) convertView.findViewById(R.id.btnEditDevice);
        if (di.isConfigured())
            btn.setImageResource(android.R.drawable.ic_menu_edit);
        else
            btn.setImageResource(android.R.drawable.ic_menu_add);
        btn.setTag(position);
        btn.setFocusable(false); // or else onItemClick doesn't work in the ListView
        btn.setFocusableInTouchMode(false);
        btn.setOnClickListener(this);

        convertView.setTag(position);
        return convertView;
    }

    public void setDevices(List<DeviceInfo> new_devices) {
        all_devices = new_devices;
        update();
    }

    public void setMenuConfigureEvent(MenuConfigureEvent dce) {
        menuConfigureEvent = dce;
    }

    @Override
    public void onClick(View v) {
        if (menuConfigureEvent != null) {
            int position = (Integer) v.getTag();
            menuConfigureEvent.onConfigure(v, position);
        }
    }

    public void update() {
        notifyDataSetChanged();
    }

}
