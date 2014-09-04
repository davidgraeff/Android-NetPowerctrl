package oly.netpowerctrl.devices;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.data.onDataLoaded;
import oly.netpowerctrl.network.onNewDevice;

/**
 * An adapter for showing all configured (and newly discovered) devices. Configured and new devices
 * are separated by headers.
 */
public class DevicesAdapter extends BaseAdapter implements onCollectionUpdated<DeviceCollection, Device>, onDataLoaded, onNewDevice {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private final LayoutInflater inflater;
    private final boolean showNewDevices;

    @SuppressWarnings("SameParameterValue")
    public DevicesAdapter(Context context, boolean showNewDevices) {
        this.showNewDevices = showNewDevices;
        inflater = LayoutInflater.from(context);
        onResume();
    }

    public void onPause() {
        AppData d = AppData.getInstance();
        d.deviceCollection.unregisterObserver(this);
        AppData.observersOnDataLoaded.unregister(this);
        AppData.observersNew.unregister(this);
    }

    public void onResume() {
        AppData.observersOnDataLoaded.register(this);
        onDataLoaded();
        if (showNewDevices) {
            AppData.observersNew.register(this);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return TYPE_HEADER;
        AppData d = AppData.getInstance();
        int cs = d.deviceCollection.size();
        if (cs > 0 && position - 1 == cs)
            return TYPE_HEADER;
        else
            return TYPE_ITEM;
    }

    @Override
    public Object getItem(int position) {
        if (position == 0)
            return null;
        AppData d = AppData.getInstance();
        int cs = d.deviceCollection.size();
        if (cs > 0 && position - 1 == cs)
            return null;
        else if (position - 1 < cs) // minus one header
            return d.deviceCollection.get(position - 1);
        else // minus deviceCollection size and two headers
            return d.newDevices.get(position - (cs > 0 ? 2 : 1) - cs);
    }

    @Override
    public int getViewTypeCount() {
        AppData d = AppData.getInstance();
        int c = 1; // always one view type!
        if ((showNewDevices && d.newDevices.size() > 0) ||
                d.deviceCollection.hasDevices())
            ++c; // header type + content type

        return c;
    }

    @Override
    public int getCount() {
        AppData d = AppData.getInstance();
        int c = d.deviceCollection.size();
        if (c > 0) ++c; // header
        if (showNewDevices) {
            c += d.newDevices.size();
            if (d.newDevices.size() > 0) ++c; // header
        }
        return c;
    }


    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        if (getItemViewType(position) == TYPE_ITEM) {
            if (convertView == null)
                convertView = inflater.inflate(R.layout.device_list_item, viewGroup, false);
            assert convertView != null;

            Device device = (Device) getItem(position);
            boolean reachable = device.getFirstReachableConnection() != null;
            TextView tvName = (TextView) convertView.findViewById(R.id.device_name);
            tvName.setText(device.DeviceName);
            if (reachable)
                tvName.setPaintFlags(tvName.getPaintFlags() & ~(Paint.STRIKE_THRU_TEXT_FLAG));
            else
                tvName.setPaintFlags(tvName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

            TextView tvIP = (TextView) convertView.findViewById(R.id.device_ip);
            String subtext = "";
            if (!reachable)
                subtext += device.getNotReachableReasons();
            else {
                DeviceConnection deviceConnection = device.getFirstReachableConnection();
                subtext += deviceConnection.getProtocol() + "/" + deviceConnection.getDestinationHost();
            }

            if (device.Version.length() > 0)
                subtext += ", " + device.Version;
            if (device.hasFeatures())
                subtext += ", " + device.getFeatureString();
            tvIP.setText(subtext);

            tvIP.setTag(position);

            convertView.setTag(position);
            return convertView;
        } else { // HEADER
            if (convertView == null)
                convertView = inflater.inflate(R.layout.device_group_header, viewGroup, false);
            assert convertView != null;

            AppData d = AppData.getInstance();
            boolean isNewDeviceHeader = (!d.deviceCollection.hasDevices()
                    || position != 0);

            TextView tvName = (TextView) convertView.findViewById(R.id.lblListItem);
            tvName.setText(isNewDeviceHeader ? R.string.new_devices : R.string.configured_devices);

            return convertView;
        }
    }

    @Override
    public boolean onDataLoaded() {
        AppData.getInstance().deviceCollection.registerObserver(this);
        return true;
    }

    @Override
    public void onNewDevice(Device device) {
        notifyDataSetChanged();
    }

    @Override
    public boolean updated(DeviceCollection deviceCollection, Device device, ObserverUpdateActions action) {
        notifyDataSetChanged();
        return true;
    }
}
