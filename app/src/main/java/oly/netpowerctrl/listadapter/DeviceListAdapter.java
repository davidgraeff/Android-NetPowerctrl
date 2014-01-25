package oly.netpowerctrl.listadapter;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anelservice.DevicesUpdate;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.main.NetpowerctrlApplication;

public class DeviceListAdapter extends BaseAdapter implements DevicesUpdate {
    private List<DeviceInfo> all_devices;
    private LayoutInflater inflater;
    private boolean showNewDevices;

    public DeviceListAdapter(Context context, boolean showNewDevices) {
        this.showNewDevices = showNewDevices;
        inflater = LayoutInflater.from(context);
        if (showNewDevices) {
            all_devices = NetpowerctrlApplication.instance.newDevices;
            NetpowerctrlApplication.instance.registerNewDeviceObserver(this);
        } else {
            all_devices = NetpowerctrlApplication.instance.configuredDevices;
            NetpowerctrlApplication.instance.registerConfiguredObserver(this);
        }
    }

    /**
     * Call this "Destructor" while your activity is destroyed.
     * This will remove all remaining references to this object.
     */
    public void finish() {
        if (showNewDevices) {
            NetpowerctrlApplication.instance.unregisterNewDeviceObserver(this);
        } else {
            NetpowerctrlApplication.instance.unregisterConfiguredObserver(this);
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
        if (!di.reachable && !showNewDevices)
            subtext += ", " + NetpowerctrlApplication.instance.getString(R.string.error_not_reachable);
        tvIP.setText(subtext);
        if (di.reachable || showNewDevices)
            tvIP.setPaintFlags(tvIP.getPaintFlags() & ~(Paint.STRIKE_THRU_TEXT_FLAG));
        else
            tvIP.setPaintFlags(tvIP.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        tvIP.setTag(position);

        convertView.setTag(position);
        return convertView;
    }

    public void setDevices(List<DeviceInfo> new_devices) {
        all_devices = new_devices;
        onDevicesUpdated(null);
    }

    @Override
    public void onDevicesUpdated(List<DeviceInfo> changed_devices) {
        notifyDataSetChanged();
    }
}
