package oly.netpowerctrl.listadapter;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.network.DeviceUpdate;

public class DevicesAdapter extends BaseExpandableListAdapter implements DeviceUpdate {
    private LayoutInflater inflater;
    private boolean showNewDevices;

    public DevicesAdapter(Context context, boolean showNewDevices) {
        this.showNewDevices = showNewDevices;
        inflater = LayoutInflater.from(context);
        onResume();
    }

    public void onPause() {
        NetpowerctrlApplication.getDataController().unregisterConfiguredDeviceChangeObserver(this);
        NetpowerctrlApplication.getDataController().unregisterNewDeviceObserver(this);
    }

    public void onResume() {
        NetpowerctrlApplication.getDataController().registerConfiguredDeviceChangeObserver(this);
        if (showNewDevices) {
            NetpowerctrlApplication.getDataController().registerNewDeviceObserver(this);
        }
    }

    @Override
    public void onDeviceUpdated(DeviceInfo di, boolean willBeRemoved) {
        notifyDataSetChanged();
    }

    @Override
    public int getGroupCount() {
        int c = NetpowerctrlApplication.getDataController().configuredDevices.size() > 0 ? 1 : 0;
        if (showNewDevices && NetpowerctrlApplication.getDataController().newDevices.size() > 0)
            ++c;

        return c;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        // The groupPosition of new_devices is always 1
        if (NetpowerctrlApplication.getDataController().configuredDevices.size() == 0 && groupPosition == 0)
            groupPosition = 1;

        int r = (groupPosition == 0) ? NetpowerctrlApplication.getDataController().configuredDevices.size() :
                NetpowerctrlApplication.getDataController().newDevices.size();
        return r;
    }

    @Override
    public Object getGroup(int i) {
        return null;
    }

    @Override
    public Object getChild(int groupPosition, int device) {
        // The groupPosition of new_devices is always 1
        if (NetpowerctrlApplication.getDataController().configuredDevices.size() == 0 && groupPosition == 0)
            groupPosition = 1;
        return (groupPosition == 0) ? NetpowerctrlApplication.getDataController().configuredDevices.get(device) :
                NetpowerctrlApplication.getDataController().newDevices.get(device);
    }

    @Override
    public long getGroupId(int groupPosition) {
        // The groupID of new_devices is always 1
        if (NetpowerctrlApplication.getDataController().configuredDevices.size() == 0 && groupPosition == 0)
            groupPosition = 1;
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childID) {
        // The groupID of new_devices is always 1
        if (NetpowerctrlApplication.getDataController().configuredDevices.size() == 0 && groupPosition == 0)
            groupPosition = 1;
        return groupPosition * 0xffff + childID;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = inflater.inflate(R.layout.device_group_header, null);
        assert convertView != null;

        // The groupPosition of new_devices is always 1
        int groupP = groupPosition;
        if (NetpowerctrlApplication.getDataController().configuredDevices.size() == 0 && groupP == 0)
            groupP = 1;

        TextView tvName = (TextView) convertView.findViewById(R.id.lblListItem);
        tvName.setText((groupP == 0) ? R.string.configured_devices : R.string.new_devices);

        ExpandableListView mExpandableListView = (ExpandableListView) parent;
        mExpandableListView.expandGroup(groupPosition);

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int position, boolean b, View convertView, ViewGroup viewGroup) {
        // The groupPosition of new_devices is always 1
        if (NetpowerctrlApplication.getDataController().configuredDevices.size() == 0 && groupPosition == 0)
            groupPosition = 1;

        List<DeviceInfo> devices = (groupPosition == 0) ? NetpowerctrlApplication.getDataController().configuredDevices :
                NetpowerctrlApplication.getDataController().newDevices;

        if (convertView == null)
            convertView = inflater.inflate(R.layout.device_list_item, null);
        assert convertView != null;

        if (position == 1)
            position = 1;
        DeviceInfo di = devices.get(position);
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
        if (di.FirmwareVersion.length() > 0)
            subtext += ", " + di.FirmwareVersion;
        if (!di.isReachable())
            subtext += ", " + di.not_reachable_reason;
        tvIP.setText(subtext);

        tvIP.setTag(position);

        convertView.setTag(position);
        return convertView;

    }

    @Override
    public boolean isChildSelectable(int i, int i2) {
        return true;
    }

}
