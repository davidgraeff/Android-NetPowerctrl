package oly.netpowerctrl.listadapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListAdapter;
import android.widget.Switch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anelservice.DeviceSend;
import oly.netpowerctrl.anelservice.DevicesUpdate;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.OutletInfo;
import oly.netpowerctrl.main.NetpowerctrlApplication;
import oly.netpowerctrl.utils.AfterSentHandler;

public class OutletSwitchListAdapter extends BaseAdapter implements ListAdapter, OnCheckedChangeListener, DevicesUpdate {
    private List<DeviceInfo> all_devices;
    private List<OutletInfo> all_outlets;
    private LayoutInflater inflater;
    public final Context context;
    private boolean showHidden;
    private AfterSentHandler ash = new AfterSentHandler(this);

    public OutletSwitchListAdapter(Context context) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        all_outlets = new ArrayList<OutletInfo>();
        showHidden = false;
        NetpowerctrlApplication.instance.registerConfiguredObserver(this);
        all_devices = NetpowerctrlApplication.instance.configuredDevices;
        onDevicesUpdated();
    }

    public int getCount() {
        return all_outlets.size();
    }

    public Object getItem(int position) {
        return all_outlets.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.outlet_list_switch, null);
            assert convertView != null;
            Switch tv = (Switch) convertView.findViewById(R.id.outlet_list_switch);
            tv.setOnCheckedChangeListener(this);
            // We need this empty on long click handler, otherwise the listView's
            // on long click handler does not work
            tv.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    return false;
                }
            });
        }
        OutletInfo info = all_outlets.get(position);
        Switch tv = (Switch) convertView.findViewById(R.id.outlet_list_switch);
        tv.setAlpha(info.Hidden ? 0.6f : 1.0f);
        tv.setTag(-1);
        tv.setText(info.UserDescription.isEmpty() ? info.Description : info.UserDescription);
        tv.setChecked(info.State);
        tv.setEnabled(!info.Disabled);
        tv.setTag(position);
        return convertView;
    }

    public void swapPosition(int itemPosition, int targetPosition) {
        int t = all_outlets.get(itemPosition).positionRequest;
        all_outlets.get(itemPosition).positionRequest = all_outlets.get(targetPosition).positionRequest;
        all_outlets.get(targetPosition).positionRequest = t;

        onDevicesUpdated();
    }

    @Override
    public void onCheckedChanged(CompoundButton arg0, boolean new_state) {
        int position = (Integer) arg0.getTag();
        if (position == -1)
            return;
        arg0.setEnabled(false);
        OutletInfo oi = (OutletInfo) getItem(position);
        oi.Disabled = false;
        ash.setData(position, new_state);
        ash.removeMessages();
        ash.startDelayedCheck();
        DeviceSend.sendOutlet(context, oi.device, oi.OutletNumber, new_state);
    }

    public boolean getIsShowingHidden() {
        return showHidden;
    }

    public void setShowHidden(boolean b) {
        showHidden = b;
        onDevicesUpdated();
    }

    @Override
    public void onDevicesUpdated() {
        // Clear
        ash.removeMessages();
        all_outlets.clear();

        for (DeviceInfo device : all_devices) {
            for (OutletInfo oi : device.Outlets) {
                oi.device = device;
                if (!oi.Disabled && (!oi.Hidden || showHidden))
                    all_outlets.add(oi);
            }
        }

        // Sort for positionRequest number or alphabetically
        Collections.sort(all_outlets);

        // Assign positionRequest numbers
        for (int i = 0; i < all_outlets.size(); ++i) {
            all_outlets.get(i).positionRequest = i;
        }
        notifyDataSetChanged();
    }
}
