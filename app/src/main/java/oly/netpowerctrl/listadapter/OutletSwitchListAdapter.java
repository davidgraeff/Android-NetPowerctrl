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
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anelservice.DeviceSend;
import oly.netpowerctrl.anelservice.DevicesUpdate;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.OutletInfo;
import oly.netpowerctrl.main.NetpowerctrlApplication;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.AfterSentHandler;

public class OutletSwitchListAdapter extends BaseAdapter implements ListAdapter, OnCheckedChangeListener, DevicesUpdate {
    private static class OutletInfoAdditional {
        public OutletInfo oi;
        String displayText;
        public boolean enabled;

        OutletInfoAdditional(OutletInfo oi, boolean showDevice) {
            this.oi = oi;
            this.enabled = oi.device.reachable;
            if (showDevice)
                displayText = oi.device.DeviceName + ": " + oi.getDescription();
            else
                displayText = oi.getDescription();
        }
    }

    private List<OutletInfoAdditional> all_outlets;
    private LayoutInflater inflater;
    public final Context context;
    private boolean showHidden;
    private boolean showDeviceNames;
    private AfterSentHandler ash = new AfterSentHandler(this);
    private boolean temporary_ignore_positionRequest;
    private NotReachableUpdate notReachableObserver;

    public OutletSwitchListAdapter(Context context) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        all_outlets = new ArrayList<OutletInfoAdditional>();
        showHidden = SharedPrefs.getShowHiddenOutlets(context);
        showDeviceNames = SharedPrefs.getShowDeviceNames(context);
        NetpowerctrlApplication.instance.registerConfiguredObserver(this);
        onDevicesUpdated();
    }

    @Override
    public int getCount() {
        return all_outlets.size();
    }

    public OutletInfo getItem(int position) {
        return all_outlets.get(position).oi;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
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
        OutletInfoAdditional info = all_outlets.get(position);
        Switch tv = (Switch) convertView.findViewById(R.id.outlet_list_switch);
        tv.setAlpha(info.oi.Hidden ? 0.6f : 1.0f);
        tv.setTag(-1);
        tv.setText(info.displayText);
        tv.setChecked(info.oi.State);
        tv.setEnabled(info.enabled);
        tv.setTag(position);
        return convertView;
    }

    public void swapPosition(int itemPosition, int targetPosition) {
        int t = all_outlets.get(itemPosition).oi.positionRequest;
        all_outlets.get(itemPosition).oi.positionRequest = all_outlets.get(targetPosition).oi.positionRequest;
        all_outlets.get(targetPosition).oi.positionRequest = t;

        onDevicesUpdated();
    }

    @Override
    public void onCheckedChanged(CompoundButton arg0, boolean new_state) {
        int position = (Integer) arg0.getTag();
        if (position == -1)
            return;
        arg0.setEnabled(false);
        OutletInfo oi = getItem(position);
        all_outlets.get(position).enabled = false;
        ash.setData(position, new_state);
        ash.removeMessages();
        ash.startDelayedCheck();
        DeviceSend.sendOutlet(context, oi.device, oi.OutletNumber, new_state);
    }

    @Override
    public void onDevicesUpdated() {
        // Clear
        ash.removeMessages();
        all_outlets.clear();

        int not_reachable = 0;
        List<DeviceInfo> all_devices = NetpowerctrlApplication.instance.configuredDevices;
        for (DeviceInfo device : all_devices) {
            if (!device.reachable)
                ++not_reachable;

            for (OutletInfo oi : device.Outlets) {
                oi.device = device;
                if (oi.Disabled || (oi.Hidden && !showHidden))
                    continue;

                OutletInfoAdditional new_oi = new OutletInfoAdditional(oi, showDeviceNames);

                boolean found = false;
                for (int i = 0; i < all_outlets.size(); ++i) {
                    boolean behind_current = temporary_ignore_positionRequest ?
                            all_outlets.get(i).displayText.compareTo(new_oi.displayText) > 0 :
                            all_outlets.get(i).oi.positionRequest > new_oi.oi.positionRequest;

                    if (behind_current) {
                        all_outlets.add(i, new_oi);
                        found = true;
                        break;
                    }
                }
                if (!found)
                    all_outlets.add(new_oi);
            }
        }

        // Sort for positionRequest number or alphabetically
        //Collections.sort(all_outlets);

        // Assign positionRequest numbers
        for (int i = 0; i < all_outlets.size(); ++i) {
            all_outlets.get(i).oi.positionRequest = i;
        }
        notifyDataSetChanged();

        if (notReachableObserver != null)
            notReachableObserver.onNotReachableUpdate(not_reachable);
    }

    public void sortAlphabetically() {
        temporary_ignore_positionRequest = true;
        onDevicesUpdated();
        temporary_ignore_positionRequest = false;
    }


    public boolean getIsShowingHidden() {
        return showHidden;
    }

    public void setShowHidden(boolean b) {
        showHidden = b;
        onDevicesUpdated();
        SharedPrefs.setShowHiddenOutlets(showDeviceNames, context);
    }

    /**
     * Inform the given object about not reachable devices
     *
     * @param notReachableObserver
     */
    public void setNotReachableObserver(NotReachableUpdate notReachableObserver) {
        this.notReachableObserver = notReachableObserver;
        if (notReachableObserver == null)
            return;

        int not_reachable = 0;
        List<DeviceInfo> all_devices = NetpowerctrlApplication.instance.configuredDevices;
        for (DeviceInfo device : all_devices) {
            if (!device.reachable)
                ++not_reachable;

        }

        notReachableObserver.onNotReachableUpdate(not_reachable);
    }

    public boolean isShowDeviceNames() {
        return showDeviceNames;
    }

    public void setShowDeviceNames(boolean showDeviceNames) {
        this.showDeviceNames = showDeviceNames;
        onDevicesUpdated();
        SharedPrefs.setShowDeviceNames(showDeviceNames, context);
    }
}
