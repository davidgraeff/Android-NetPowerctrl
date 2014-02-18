package oly.netpowerctrl.listadapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anelservice.DeviceSend;
import oly.netpowerctrl.anelservice.DevicesUpdate;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.OutletInfo;
import oly.netpowerctrl.preferences.SharedPrefs;

public class OutletsExecuteAdapter extends OutletsBaseAdapter implements
OnCheckedChangeListener, DevicesUpdate {

    private boolean temporary_ignore_positionRequest;
    private NotReachableUpdate notReachableObserver;

    private View.OnClickListener closeClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            mListItemMenu.onMenuItemClicked(view, (Integer) view.getTag());
        }
    };

    public OutletsExecuteAdapter(Context context) {
        super(context);
        showHidden = SharedPrefs.getShowHiddenOutlets(context);
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
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = super.getView(position, convertView, parent);
        OutletInfoAdditional info = all_outlets.get(position);

        Switch tv = (Switch) convertView.findViewById(R.id.outlet_list_switch);
        tv.setOnCheckedChangeListener(this);
        tv.setTag(-1);
        tv.setChecked(info.oi.State);
        tv.setEnabled(info.enabled);
        tv.setTag(position);

        return convertView;
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

    @Override
    public List<DeviceInfo> update(List<DeviceInfo> all_devices) {
        List<DeviceInfo> not_reachable = super.update(all_devices);

        if (notReachableObserver != null)
            notReachableObserver.onNotReachableUpdate(not_reachable);

        return not_reachable;
    }

    /**
     * Called if registerConfiguredObserver has been called before and a configured
     * device changes.
     *
     * @param ignored
     */
    @Override
    public void onDevicesUpdated(List<DeviceInfo> ignored) {
        update(NetpowerctrlApplication.getDataController().configuredDevices);
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
        SharedPrefs.setShowHiddenOutlets(showHidden);
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
