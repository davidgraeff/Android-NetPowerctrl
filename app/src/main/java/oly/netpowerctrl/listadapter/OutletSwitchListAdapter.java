package oly.netpowerctrl.listadapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anelservice.DeviceSend;
import oly.netpowerctrl.anelservice.DevicesUpdate;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.OutletInfo;
import oly.netpowerctrl.dynamicgid.AbstractDynamicGridAdapter;
import oly.netpowerctrl.preferences.SharedPrefs;

public class OutletSwitchListAdapter extends AbstractDynamicGridAdapter implements
        OnCheckedChangeListener, DevicesUpdate {

    private static class OutletInfoAdditional {
        public OutletInfo oi;
        String displayText;
        public boolean enabled;
        public long id;

        OutletInfoAdditional(OutletInfo oi, boolean showDevice, long id) {
            this.id = id;
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
    private boolean showHidden;
    private boolean showDeviceNames;
    private boolean temporary_ignore_positionRequest;
    private NotReachableUpdate notReachableObserver;

    public OutletSwitchListAdapter(Context context) {
        inflater = LayoutInflater.from(context);
        all_outlets = new ArrayList<OutletInfoAdditional>();
        showHidden = SharedPrefs.getShowHiddenOutlets(context);
        showDeviceNames = SharedPrefs.getShowDeviceNames(context);
        NetpowerctrlApplication.getDataController().registerConfiguredObserver(this);
        onDevicesUpdated(null);
    }

    /**
     * Call this "Destructor" while your activity is destroyed.
     * This will remove all remaining references to this object.
     */
    public void finish() {
        setNotReachableObserver(null);
        NetpowerctrlApplication.getDataController().unregisterConfiguredObserver(this);
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
        return all_outlets.get(position).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.outlet_list_switch, null);
            assert convertView != null;
            Switch tv = (Switch) convertView.findViewById(R.id.outlet_list_switch);
            tv.setOnCheckedChangeListener(this);
        }
        OutletInfoAdditional info = all_outlets.get(position);
        Switch tv = (Switch) convertView.findViewById(R.id.outlet_list_switch);
        tv.setTag(-1);
        tv.setChecked(info.oi.State);
        tv.setEnabled(info.enabled);
        tv.setTag(position);

        TextView textView = (TextView) convertView.findViewById(R.id.outlet_list_text);
        textView.setEnabled(info.enabled);
        textView.setTypeface(null, info.oi.Hidden ? Typeface.ITALIC : Typeface.NORMAL);
        textView.setAlpha(info.oi.Hidden ? 0.5f : 1.0f);
        textView.setText(info.displayText);

        return convertView;
    }


    @Override
    public void reorderItems(int originalPosition, int newPosition) {
        if (newPosition >= getCount()) {
            return;
        }
        OutletInfoAdditional temp = all_outlets.get(originalPosition);
        all_outlets.get(originalPosition).oi.positionRequest = all_outlets.get(newPosition).oi.positionRequest;
        all_outlets.get(newPosition).oi.positionRequest = temp.oi.positionRequest;
        all_outlets.remove(originalPosition);
        all_outlets.add(newPosition, temp);
        notifyDataSetChanged();
    }

    @Override
    public void finishedReordering() {
        NetpowerctrlApplication.getDataController().saveConfiguredDevices(false);
    }

    @Override
    public void onCheckedChanged(CompoundButton arg0, boolean new_state) {
        int position = (Integer) arg0.getTag();
        if (position == -1)
            return;
        arg0.setEnabled(false);
        OutletInfo oi = getItem(position);
        all_outlets.get(position).enabled = false;
        DeviceSend.instance().sendOutlet(oi, new_state, true);
    }

    private void addOutlet(DeviceInfo device, OutletInfo oi) {
        oi.device = device;
        if (oi.Disabled || (oi.Hidden && !showHidden))
            return;

        OutletInfoAdditional new_oi = new OutletInfoAdditional(oi, showDeviceNames, all_outlets.size());

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

    @Override
    public void onDevicesUpdated(List<DeviceInfo> ignored) {
        // Clear
        all_outlets.clear();

        List<DeviceInfo> not_reachable = new ArrayList<DeviceInfo>();
        List<DeviceInfo> all_devices = NetpowerctrlApplication.getDataController().configuredDevices;
        for (DeviceInfo device : all_devices) {
            if (!device.reachable) {
                not_reachable.add(device);
                continue;
            }

            for (OutletInfo oi : device.Outlets) {
                addOutlet(device, oi);
            }

            for (OutletInfo oi : device.IOs) {
                addOutlet(device, oi);
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
        onDevicesUpdated(null);
        temporary_ignore_positionRequest = false;
    }

    public boolean getIsShowingHidden() {
        return showHidden;
    }

    public void setShowHidden(boolean b) {
        showHidden = b;
        onDevicesUpdated(null);
        SharedPrefs.setShowHiddenOutlets(showDeviceNames);
    }

    /**
     * Inform the given object about not reachable devices
     *
     * @param notReachableObserver The object to notify
     */
    public void setNotReachableObserver(NotReachableUpdate notReachableObserver) {
        this.notReachableObserver = notReachableObserver;
        if (notReachableObserver == null)
            return;

        List<DeviceInfo> not_reachable = new ArrayList<DeviceInfo>();
        List<DeviceInfo> all_devices = NetpowerctrlApplication.getDataController().configuredDevices;
        for (DeviceInfo device : all_devices) {
            if (!device.reachable)
                not_reachable.add(device);

        }

        notReachableObserver.onNotReachableUpdate(not_reachable);
    }
}
