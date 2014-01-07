package oly.netpowerctrl.listadapter;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anelservice.DevicesUpdate;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.main.NetpowerctrlApplication;
import oly.netpowerctrl.utils.ListItemMenu;

public class DeviceListAdapter extends BaseAdapter implements OnClickListener, DevicesUpdate {

    private ListItemMenu listItemMenu = null;

    private List<DeviceInfo> all_devices;
    private LayoutInflater inflater;
    private boolean showNewDevices;
    private Context context;

    public DeviceListAdapter(Context context, boolean showNewDevices) {
        this.showNewDevices = showNewDevices;
        this.context = context;
        inflater = LayoutInflater.from(context);
        if (showNewDevices) {
            all_devices = NetpowerctrlApplication.instance.newDevices;
            NetpowerctrlApplication.instance.registerNewDeviceObserver(this);
        } else {
            all_devices = NetpowerctrlApplication.instance.configuredDevices;
            NetpowerctrlApplication.instance.registerConfiguredObserver(this);

        }
    }

    public List<DeviceInfo> getDevices() {
        return all_devices;
    }

    public int getCount() {
        return all_devices.size();
    }

    public DeviceInfo getItem(int position) {
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
        String subtext = di.HostName;
        if (di.Temperature.length() > 0)
            subtext += ", " + di.Temperature;
        if (di.FirmwareVersion.length() > 0)
            subtext += ", " + di.FirmwareVersion;
        if (!di.reachable)
            subtext += ", " + context.getString(R.string.error_not_reachable);
        tvIP.setText(subtext);
        if (di.reachable)
            tvIP.setPaintFlags(tvIP.getPaintFlags() & ~(Paint.STRIKE_THRU_TEXT_FLAG));
        else
            tvIP.setPaintFlags(tvIP.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        ImageButton btn = (ImageButton) convertView.findViewById(R.id.btnEditDevice);
        if (showNewDevices)
            btn.setImageResource(android.R.drawable.ic_menu_add);
        else
            btn.setImageResource(android.R.drawable.ic_menu_edit);
        btn.setTag(position);
        btn.setFocusable(false); // or else onItemClick doesn't work in the ListView
        btn.setFocusableInTouchMode(false);
        btn.setOnClickListener(this);

        convertView.setTag(position);
        return convertView;
    }

    public void setDevices(List<DeviceInfo> new_devices) {
        all_devices = new_devices;
        onDevicesUpdated();
    }

    public void setListItemMenu(ListItemMenu dce) {
        listItemMenu = dce;
    }

    @Override
    public void onClick(View v) {
        if (listItemMenu != null) {
            int position = (Integer) v.getTag();
            listItemMenu.onMenuItemClicked(v, position);
        }
    }

    @Override
    public void onDevicesUpdated() {
        notifyDataSetChanged();
    }
}
