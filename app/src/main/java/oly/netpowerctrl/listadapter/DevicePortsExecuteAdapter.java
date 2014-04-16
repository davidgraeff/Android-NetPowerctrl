package oly.netpowerctrl.listadapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.application_state.RuntimeDataControllerStateChanged;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.DevicePort;
import oly.netpowerctrl.network.DeviceUpdate;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.Icons;
import oly.netpowerctrl.utils.ListItemMenu;

public class DevicePortsExecuteAdapter extends DevicePortsBaseAdapter implements
        DeviceUpdate, SeekBar.OnSeekBarChangeListener, RuntimeDataControllerStateChanged {

    // We block updates while moving the range slider
    private boolean blockUpdates = false;

    // Some observers
    private WeakReference<NotReachableUpdate> notReachableObserver;

    public DevicePortsExecuteAdapter(Context context, ListItemMenu mListContextMenu, UUID filterGroup) {
        super(context, mListContextMenu, filterGroup);
        showHidden = SharedPrefs.getShowHiddenOutlets();
        blockUpdates = !NetpowerctrlApplication.getDataController().isInitialDataQueryCompleted();
        onResume();
        if (!blockUpdates)
            onDeviceUpdated(null, true);
    }

    public void onPause() {
        NetpowerctrlApplication.getDataController().unregisterConfiguredDeviceChangeObserver(this);
        NetpowerctrlApplication.getDataController().unregisterRuntimeDataControllerStateChanged(this);
    }

    public void onResume() {
        NetpowerctrlApplication.getDataController().registerConfiguredDeviceChangeObserver(this);
        NetpowerctrlApplication.getDataController().registerRuntimeDataControllerStateChanged(this);
    }

    @Override
    public void onDataReloaded() {

    }

    /**
     * Update view with outlets after a complete device query finished
     */
    @Override
    public void onDataQueryFinished() {
        blockUpdates = false;
        onDeviceUpdated(null, true);
    }

    @Override
    public int getViewTypeCount() {
        return DevicePort.DevicePortType.values().length;
    }

    @Override
    public int getItemViewType(int position) {
        return all_outlets.get(position).port.getType().ordinal();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DevicePortListItem item = all_outlets.get(position);
        DevicePort port = item.port;

        convertView = super.getView(position, convertView, parent);

        // We do this only once, if the viewHolder is new
        if (current_viewHolder.isNew) {
            // For a grid view with a dedicated edit button (image) we use that for
            // setOnClickListener. In the other case we use the main icon for setOnClickListener.
            if (current_viewHolder.imageEdit != null) {
                current_viewHolder.imageEdit.setTag(position);
                current_viewHolder.imageEdit.setOnClickListener(current_viewHolder);
            } else {
                current_viewHolder.imageIcon.setTag(position);
                current_viewHolder.imageIcon.setOnClickListener(current_viewHolder);
            }
            //current_viewHolder.mainTextView.setTag(position);
            switch (port.getType()) {
                case TypeToggle: {
                    current_viewHolder.seekBar.setVisibility(View.GONE);
                    current_viewHolder.bitmapOff = Icons.loadIcon(context, port.uuid,
                            Icons.IconType.DevicePortIcon, Icons.IconState.StateOff,
                            Icons.getResIdForState(Icons.IconState.StateOff));
                    current_viewHolder.bitmapOn = Icons.loadIcon(context, port.uuid,
                            Icons.IconType.DevicePortIcon, Icons.IconState.StateOn,
                            Icons.getResIdForState(Icons.IconState.StateOn));
                    break;
                }
                case TypeButton: {
                    current_viewHolder.bitmapOff = Icons.loadIcon(context, port.uuid,
                            Icons.IconType.DevicePortIcon, Icons.IconState.StateOff,
                            R.drawable.netpowerctrl);
                    current_viewHolder.seekBar.setVisibility(View.GONE);
                    break;
                }
                case TypeRangedValue:
                    current_viewHolder.bitmapOff = Icons.loadIcon(context, port.uuid,
                            Icons.IconType.DevicePortIcon, Icons.IconState.StateOff,
                            Icons.getResIdForState(Icons.IconState.StateOff));
                    current_viewHolder.bitmapOn = Icons.loadIcon(context, port.uuid,
                            Icons.IconType.DevicePortIcon, Icons.IconState.StateOn,
                            Icons.getResIdForState(Icons.IconState.StateOn));
                    current_viewHolder.seekBar.setVisibility(View.VISIBLE);
                    current_viewHolder.seekBar.setOnSeekBarChangeListener(this);
                    current_viewHolder.seekBar.setTag(-1);
                    current_viewHolder.seekBar.setMax(port.max_value - port.min_value);
                    break;
            }

        }

        // This has to be done more often
        switch (port.getType()) {
            case TypeButton: {
                current_viewHolder.imageIcon.setImageBitmap(current_viewHolder.bitmapOff);
                break;
            }
            case TypeToggle: {
                if (port.current_value >= port.max_value)
                    current_viewHolder.imageIcon.setImageBitmap(current_viewHolder.bitmapOn);
                else
                    current_viewHolder.imageIcon.setImageBitmap(current_viewHolder.bitmapOff);

                break;
            }
            case TypeRangedValue:
                current_viewHolder.seekBar.setTag(-1);
                current_viewHolder.seekBar.setProgress(port.current_value - port.min_value);
                current_viewHolder.seekBar.setTag(position);
                if (port.current_value <= port.min_value)
                    current_viewHolder.imageIcon.setImageBitmap(current_viewHolder.bitmapOff);
                else
                    current_viewHolder.imageIcon.setImageBitmap(current_viewHolder.bitmapOn);
                break;
        }

        return convertView;
    }

    @Override
    public List<DeviceInfo> update(List<DeviceInfo> all_devices) {
        List<DeviceInfo> not_reachable = super.update(all_devices);

        if (notReachableObserver != null && notReachableObserver.get() != null)
            notReachableObserver.get().onNotReachableUpdate(not_reachable);

        return not_reachable;
    }

    @Override
    public void onDeviceUpdated(DeviceInfo di, boolean willBeRemoved) {
        if (blockUpdates)
            return;

        if (willBeRemoved)
            update(NetpowerctrlApplication.getDataController().configuredDevices);
        else
            notifyDataSetChanged();
    }

    @Override
    public void setGroupFilter(UUID groupFilter) {
        super.setGroupFilter(groupFilter);
        update(NetpowerctrlApplication.getDataController().configuredDevices);
    }

    public void sortAlphabetically() {
        temporary_ignore_positionRequest = true;
        update(NetpowerctrlApplication.getDataController().configuredDevices);
        temporary_ignore_positionRequest = false;
    }

    public boolean getIsShowingHidden() {
        return showHidden;
    }

    public void setShowHidden(boolean b) {
        showHidden = b;
        blockUpdates = false;
        update(NetpowerctrlApplication.getDataController().configuredDevices);
        SharedPrefs.setShowHiddenOutlets(showHidden);
    }

    /**
     * Inform the given object about not reachable devices
     *
     * @param notReachableObserver The object to notify
     */
    public void setNotReachableObserver(NotReachableUpdate notReachableObserver) {
        this.notReachableObserver = new WeakReference<NotReachableUpdate>(notReachableObserver);
        if (notReachableObserver == null)
            return;

        List<DeviceInfo> not_reachable = new ArrayList<DeviceInfo>();
        List<DeviceInfo> all_devices = NetpowerctrlApplication.getDataController().configuredDevices;
        for (DeviceInfo device : all_devices) {
            if (!device.isReachable())
                not_reachable.add(device);
        }

        notReachableObserver.onNotReachableUpdate(not_reachable);
    }

    public void handleClick(int position) {
        DevicePortListItem info = all_outlets.get(position);
        NetpowerctrlApplication.getDataController().execute(info.port, DevicePort.TOGGLE, null);
        notifyDataSetChanged();
    }

    @Override
    public void onProgressChanged(SeekBar view, int value, boolean b) {
        int position = (Integer) view.getTag();
        if (position == -1)
            return;
        DevicePortListItem info = all_outlets.get(position);
        info.port.current_value = value + info.port.min_value;
        info.command_value = info.port.current_value;
        NetpowerctrlApplication.getDataController().execute(info.port, info.command_value, null);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        blockUpdates = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        blockUpdates = false;
    }
}
