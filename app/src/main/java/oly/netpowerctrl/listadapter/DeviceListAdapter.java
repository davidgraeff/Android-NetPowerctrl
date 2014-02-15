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
import oly.netpowerctrl.anelservice.DevicesUpdate;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.datastructure.DeviceInfo;

public class DeviceListAdapter extends BaseExpandableListAdapter implements DevicesUpdate {
    private LayoutInflater inflater;
    private boolean showNewDevices;

    public DeviceListAdapter(Context context, boolean showNewDevices) {
        this.showNewDevices = showNewDevices;
        inflater = LayoutInflater.from(context);
        NetpowerctrlApplication.getDataController().registerConfiguredObserver(this);
        if (showNewDevices) {
            NetpowerctrlApplication.getDataController().registerNewDeviceObserver(this);
        }
    }

    /**
     * Call this "Destructor" while your activity is destroyed.
     * This will remove all remaining references to this object.
     */
    public void finish() {
        NetpowerctrlApplication.getDataController().unregisterConfiguredObserver(this);
        if (showNewDevices) {
            NetpowerctrlApplication.getDataController().unregisterNewDeviceObserver(this);
        }
    }

    private View getView(int position, View convertView, List<DeviceInfo> devices) {

        if (convertView == null)
            convertView = inflater.inflate(R.layout.device_list_item, null);
        assert convertView != null;

        DeviceInfo di = devices.get(position);
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

    @Override
    public void onDevicesUpdated(List<DeviceInfo> changed_devices) {
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

        return (groupPosition == 0) ? NetpowerctrlApplication.getDataController().configuredDevices.size() :
                NetpowerctrlApplication.getDataController().newDevices.size();
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
    public View getChildView(int groupPosition, int position, boolean b, View view, ViewGroup viewGroup) {
        // The groupPosition of new_devices is always 1
        if (NetpowerctrlApplication.getDataController().configuredDevices.size() == 0 && groupPosition == 0)
            groupPosition = 1;

        return getView(position, view,
                (groupPosition == 0) ? NetpowerctrlApplication.getDataController().configuredDevices :
                        NetpowerctrlApplication.getDataController().newDevices);
    }

    @Override
    public boolean isChildSelectable(int i, int i2) {
        return true;
    }
}
