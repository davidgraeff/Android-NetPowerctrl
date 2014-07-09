package oly.netpowerctrl.devices;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.application_state.RuntimeDataController;
import oly.netpowerctrl.application_state.RuntimeStateChanged;
import oly.netpowerctrl.network.DeviceUpdate;

/**
 * An adapter for showing all configured (and newly discovered) devices. Configured and new devices
 * are separated by headers.
 */
public class DevicesAdapter extends BaseAdapter implements DeviceUpdate, RuntimeStateChanged {
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
        RuntimeDataController d = NetpowerctrlApplication.getDataController();
        d.deviceCollection.unregisterDeviceObserver(this);
        NetpowerctrlApplication.getDataController().unregisterStateChanged(this);
        NetpowerctrlApplication.getDataController().unregisterNewDeviceObserver(this);
    }

    public void onResume() {
        RuntimeDataController d = NetpowerctrlApplication.getDataController();
        d.registerStateChanged(this);
        onDataLoaded();
        if (showNewDevices) {
            d.registerNewDeviceObserver(this);
        }
    }

    @Override
    public void onDeviceUpdated(DeviceInfo di, boolean willBeRemoved) {
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return TYPE_HEADER;
        RuntimeDataController d = NetpowerctrlApplication.getDataController();
        int cs = d.deviceCollection.devices.size();
        if (cs > 0 && position - 1 == cs)
            return TYPE_HEADER;
        else
            return TYPE_ITEM;
    }

    @Override
    public Object getItem(int position) {
        if (position == 0)
            return null;
        RuntimeDataController d = NetpowerctrlApplication.getDataController();
        int cs = d.deviceCollection.devices.size();
        if (cs > 0 && position - 1 == cs)
            return null;
        else if (position - 1 < cs) // minus one header
            return d.deviceCollection.devices.get(position - 1);
        else // minus deviceCollection size and two headers
            return d.newDevices.get(position - (cs > 0 ? 2 : 1) - cs);
    }

    @Override
    public int getViewTypeCount() {
        RuntimeDataController d = NetpowerctrlApplication.getDataController();
        int c = 1; // always one view type!
        if ((showNewDevices && d.newDevices.size() > 0) ||
                d.deviceCollection.hasDevices())
            ++c; // header type + content type

        return c;
    }

    @Override
    public int getCount() {
        RuntimeDataController d = NetpowerctrlApplication.getDataController();
        int c = d.deviceCollection.devices.size();
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

            DeviceInfo di = (DeviceInfo) getItem(position);
            TextView tvName = (TextView) convertView.findViewById(R.id.device_name);
            tvName.setText(di.DeviceName);
            if (di.isReachable())
                tvName.setPaintFlags(tvName.getPaintFlags() & ~(Paint.STRIKE_THRU_TEXT_FLAG));
            else
                tvName.setPaintFlags(tvName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

            TextView tvIP = (TextView) convertView.findViewById(R.id.device_ip);
            String subtext = di.HostName;
            if (di.Temperature.length() > 0)
                subtext += ", " + di.Temperature;
            if (di.Version.length() > 0)
                subtext += ", " + di.Version;
            if (!di.isReachable())
                subtext += ", " + di.not_reachable_reason;
            if (di.PreferHTTP)
                subtext += ", HTTP";
            tvIP.setText(subtext);

            tvIP.setTag(position);

            convertView.setTag(position);
            return convertView;
        } else { // HEADER
            if (convertView == null)
                convertView = inflater.inflate(R.layout.device_group_header, viewGroup, false);
            assert convertView != null;

            RuntimeDataController d = NetpowerctrlApplication.getDataController();
            boolean isNewDeviceHeader = (!d.deviceCollection.hasDevices()
                    || position != 0);

            TextView tvName = (TextView) convertView.findViewById(R.id.lblListItem);
            tvName.setText(isNewDeviceHeader ? R.string.new_devices : R.string.configured_devices);

            return convertView;
        }
    }

    @Override
    public boolean onDataLoaded() {
        NetpowerctrlApplication.getDataController().deviceCollection.registerDeviceObserver(this);
        return true;
    }

    @Override
    public boolean onDataQueryFinished() {
        return true;
    }
}
