package oly.netpowerctrl.listadapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.DevicePort;
import oly.netpowerctrl.datastructure.Executor;
import oly.netpowerctrl.network.DevicesUpdate;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.Icons;
import oly.netpowerctrl.utils.ListItemMenu;

public class DevicePortsExecuteAdapter extends DevicePortsBaseAdapter implements
        DevicesUpdate, SeekBar.OnSeekBarChangeListener {

    // We block updates while moving the range slider
    private boolean blockUpdates = false;

    // Some observers
    private ListItemMenu mListContextMenu = null;
    private NotReachableUpdate notReachableObserver;

    private View.OnClickListener menuClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            mListContextMenu.onMenuItemClicked(view, (Integer) view.getTag());
        }
    };

    public DevicePortsExecuteAdapter(Context context, ListItemMenu mListContextMenu, UUID filterGroup) {
        super(context, filterGroup);
        this.mListContextMenu = mListContextMenu;
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
            current_viewHolder.imageView.setTag(position);
            current_viewHolder.imageView.setOnClickListener(menuClickListener);
            //current_viewHolder.mainTextView.setTag(position);
            switch (port.getType()) {
                case TypeToggle: {
                    current_viewHolder.seekBar.setVisibility(View.GONE);
                    current_viewHolder.bitmapDefault = Icons.loadStateIconBitmap(context, Icons.IconState.StateUnknown, port.uuid);
                    current_viewHolder.bitmapOff = Icons.loadStateIconBitmap(context, Icons.IconState.StateOff, port.uuid);
                    current_viewHolder.bitmapOn = Icons.loadStateIconBitmap(context, Icons.IconState.StateOn, port.uuid);
                    break;
                }
                case TypeButton: {
                    current_viewHolder.imageView.setImageResource(R.drawable.netpowerctrl);
                    current_viewHolder.seekBar.setVisibility(View.GONE);
                    break;
                }
                case TypeRangedValue:
                    current_viewHolder.imageView.setImageResource(R.drawable.netpowerctrl);
                    current_viewHolder.seekBar.setVisibility(View.VISIBLE);
                    current_viewHolder.seekBar.setOnSeekBarChangeListener(this);
                    current_viewHolder.seekBar.setMax(port.max_value - port.min_value);
                    break;
            }

        }

        // This has to be done more often
        switch (port.getType()) {
            case TypeToggle: {
                if (port.current_value <= port.min_value)
                    current_viewHolder.imageView.setImageBitmap(current_viewHolder.bitmapOff);
                else if (port.current_value >= port.max_value)
                    current_viewHolder.imageView.setImageBitmap(current_viewHolder.bitmapOn);
                else
                    current_viewHolder.imageView.setImageBitmap(current_viewHolder.bitmapDefault);

                break;
            }
            case TypeRangedValue:
                current_viewHolder.seekBar.setTag(-1);
                current_viewHolder.seekBar.setProgress(port.current_value - port.min_value);
                current_viewHolder.seekBar.setTag(position);
                break;
        }

        return convertView;
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
        if (blockUpdates)
            return;

        update(NetpowerctrlApplication.getDataController().configuredDevices);
    }

    @Override
    public void setGroupFilter(UUID groupFilter) {
        super.setGroupFilter(groupFilter);
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

    public void handleClick(int position) {
        DevicePortListItem info = all_outlets.get(position);
        Executor.execute(info.port, DevicePort.TOGGLE);
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
        Executor.execute(info.port, info.command_value);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        blockUpdates = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        blockUpdates = false;
        onDevicesUpdated(null);
    }
}
