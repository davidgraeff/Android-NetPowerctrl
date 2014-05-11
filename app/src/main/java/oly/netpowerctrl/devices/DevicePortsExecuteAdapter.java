package oly.netpowerctrl.devices;

import android.content.Context;
import android.util.Log;
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
import oly.netpowerctrl.network.DeviceUpdate;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.Icons;
import oly.netpowerctrl.utils.ListItemMenu;
import oly.netpowerctrl.utils.SortCriteriaInterface;
import oly.netpowerctrl.utils.Sorting;

public class DevicePortsExecuteAdapter extends DevicePortsBaseAdapter implements
        DeviceUpdate, SeekBar.OnSeekBarChangeListener, RuntimeDataControllerStateChanged, SortCriteriaInterface {

    // We block updates while moving the range slider
    private boolean blockUpdates = false;
    private boolean isLoaded = false;
    private static final String TAG = "PortAdapter";

    // Some observers
    private WeakReference<NotReachableUpdate> notReachableObserver;

    /**
     * The constructor will not load the adapter data! Call setGroupFilter to load data!
     *
     * @param context
     * @param mListContextMenu
     */
    public DevicePortsExecuteAdapter(Context context, ListItemMenu mListContextMenu) {
        super(context, mListContextMenu);
        showHidden = SharedPrefs.getShowHiddenOutlets();
    }

    @Override
    public void setGroupFilter(UUID groupFilter) {
        super.setGroupFilter(groupFilter);
        isLoaded = true;

        onResume();
        onDeviceUpdated(null, true);
    }

    public void onPause() {
        NetpowerctrlApplication.getDataController().deviceCollection.unregisterDeviceObserver(this);
        NetpowerctrlApplication.getDataController().unregisterRuntimeDataControllerStateChanged(this);
    }

    public void onResume() {
        NetpowerctrlApplication.getDataController().deviceCollection.registerDeviceObserver(this);
        NetpowerctrlApplication.getDataController().registerRuntimeDataControllerStateChanged(this);
    }

    @Override
    public void onDataLoaded() {

    }

    /**
     * Update view with outlets after a complete device query finished
     */
    @Override
    public void onDataQueryFinished() {
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
                    current_viewHolder.loadIcon(port.uuid,
                            Icons.IconType.DevicePortIcon, Icons.IconState.StateOff,
                            Icons.getResIdForState(Icons.IconState.StateOff), 0);
                    current_viewHolder.loadIcon(port.uuid,
                            Icons.IconType.DevicePortIcon, Icons.IconState.StateOn,
                            Icons.getResIdForState(Icons.IconState.StateOn), 1);
                    break;
                }
                case TypeButton: {
                    current_viewHolder.loadIcon(port.uuid,
                            Icons.IconType.DevicePortIcon, Icons.IconState.StateToggle,
                            R.drawable.netpowerctrl, 0);
                    current_viewHolder.seekBar.setVisibility(View.GONE);
                    break;
                }
                case TypeRangedValue:
                    current_viewHolder.loadIcon(port.uuid,
                            Icons.IconType.DevicePortIcon, Icons.IconState.StateOff,
                            Icons.getResIdForState(Icons.IconState.StateOff), 0);
                    current_viewHolder.loadIcon(port.uuid,
                            Icons.IconType.DevicePortIcon, Icons.IconState.StateOn,
                            Icons.getResIdForState(Icons.IconState.StateOn), 1);
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
                break;
            }
            case TypeToggle: {
                current_viewHolder.setCurrentBitmapIndex(port.current_value >= port.max_value ? 1 : 0);
                break;
            }
            case TypeRangedValue:
                current_viewHolder.seekBar.setTag(-1);
                current_viewHolder.seekBar.setProgress(port.current_value - port.min_value);
                current_viewHolder.seekBar.setTag(position);
                current_viewHolder.setCurrentBitmapIndex(port.current_value <= port.min_value ? 0 : 1);
                break;
        }

        return convertView;
    }

    @Override
    public List<DeviceInfo> update(List<DeviceInfo> all_devices) {
        List<DeviceInfo> not_reachable = super.update(all_devices);

        if (notReachableObserver != null) {
            NotReachableUpdate u = notReachableObserver.get();
            if (u != null)
                u.onNotReachableUpdate(not_reachable);
        }

        return not_reachable;
    }

    @Override
    public void onDeviceUpdated(DeviceInfo di, boolean willBeRemoved) {
//        Log.w(TAG, "id " + String.valueOf(Thread.currentThread().getId()) +
//                "id main " + String.valueOf(NetpowerctrlApplication.instance.getMainLooper().getThread().getId()));

        if (blockUpdates || !isLoaded || !NetpowerctrlApplication.getDataController().isInitialDataQueryCompleted())
            return;

        if (willBeRemoved)
            update(NetpowerctrlApplication.getDataController().deviceCollection.devices);
        else
            notifyDataSetChanged();
    }

    @Override
    public String[] getContentList() {
        String[] l = new String[all_outlets.size()];
        for (int i = 0; i < all_outlets.size(); ++i) {
            DevicePort port = all_outlets.get(i).port;
            l[i] = port.device.DeviceName + ": " + port.getDescription();
        }
        return l;
    }

    @Override
    public String[] getSortCriteria() {
        String[] s = new String[2];
        s[0] = "Alphabetisch";
        s[1] = "Nach Geräten";
        return s;
    }

    @Override
    public void applySortCriteria(final boolean[] criteria) {
        Sorting.qSort(all_outlets, 0, all_outlets.size() - 1, new Sorting.qSortComparable<DevicePortListItem>() {
            @Override
            public boolean isGreater(DevicePortListItem first, DevicePortListItem second) {
                boolean isGreater = false;
                if (criteria[0] &&
                        first.port.getDescription().compareTo(second.port.getDescription()) > 0) { // alphabetical
                    isGreater = true;
                }
                if (criteria[1] &&
                        first.port.device.DeviceName.compareTo(second.port.device.DeviceName) > 0) {
                    isGreater = true;
                }
                return isGreater;
            }
        });

        // Assign positionRequest numbers
        for (int i = 0; i < all_outlets.size(); ++i) {
            all_outlets.get(i).port.positionRequest = i;
        }

        DeviceCollection c = NetpowerctrlApplication.getDataController().deviceCollection;
        update(c.devices);
        c.save();
    }

    @Override
    public boolean allowCustomSort() {
        return true;
    }

    @Override
    public void setSortOrder(int[] sortOrder) {
        if (sortOrder.length != all_outlets.size()) {
            Log.e(TAG, "setSortOrder length wrong");
            return;
        }

        // Assign positionRequest numbers
        for (int i = 0; i < all_outlets.size(); ++i) {
            all_outlets.get(sortOrder[i]).port.positionRequest = i;
        }

        DeviceCollection c = NetpowerctrlApplication.getDataController().deviceCollection;
        update(c.devices);
        c.save();
    }

    public boolean getIsShowingHidden() {
        return showHidden;
    }

    public void setShowHidden(boolean b) {
        showHidden = b;
        blockUpdates = false;
        update(NetpowerctrlApplication.getDataController().deviceCollection.devices);
        SharedPrefs.setShowHiddenOutlets(showHidden);
    }

    /**
     * Inform the given object about not reachable devices
     *
     * @param notReachableObserver The object to notify
     */
    public void setNotReachableObserver(NotReachableUpdate notReachableObserver) {
        this.notReachableObserver = new WeakReference<>(notReachableObserver);
        if (notReachableObserver == null)
            return;

        List<DeviceInfo> not_reachable = new ArrayList<>();
        List<DeviceInfo> all_devices = NetpowerctrlApplication.getDataController().deviceCollection.devices;
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
