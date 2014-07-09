package oly.netpowerctrl.device_ports;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.devices.DeviceInfo;
import oly.netpowerctrl.devices.DevicePort;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.scenes.Scene;
import oly.netpowerctrl.utils.IconDeferredLoadingThread;
import oly.netpowerctrl.utils.ListItemMenu;
import oly.netpowerctrl.utils.SortCriteriaInterface;
import oly.netpowerctrl.utils.Sorting;
import oly.netpowerctrl.utils.gui.RemoveAnimation;

public class DevicePortsBaseAdapter extends BaseAdapter implements SortCriteriaInterface {

    final List<DevicePortListItem> all_outlets;
    private final IconDeferredLoadingThread iconCache = new IconDeferredLoadingThread();
    private final LayoutInflater inflater;
    // Source of values for this adapter.
    private final DevicePortSource source;
    protected int outlet_res_id = 0;
    protected boolean showHidden = true;
    protected DevicePortViewHolder current_devicePortViewHolder;
    // Animation ids
    protected long animate_click_id = -1;
    // Some observers
    protected ListItemMenu mListContextMenu = null;
    HashSet<Long> updated_id_list = new HashSet<>();
    private int nextId = 0; // we need stable IDs
    // If you change the layout or an image we increment this layout change id
    // to invalidate ViewHolders (for reloading images or layout items).
    private int layoutChangeId = 0;
    private WeakReference<RemoveAnimation> removeAnimationWeakReference = new WeakReference<>(null);
    private UUID filterGroup = null;

    DevicePortsBaseAdapter(Context context, ListItemMenu mListContextMenu, DevicePortSource source) {
        this.source = source;
        this.mListContextMenu = mListContextMenu;
        inflater = LayoutInflater.from(context);
        iconCache.start();
        all_outlets = new ArrayList<>();
        if (source != null) {
            source.setTargetAdapter(this);
            source.updateNow();
        }
    }

    public DevicePortSource getSource() {
        return source;
    }

    public void setGroupFilter(UUID groupFilter) {
        this.filterGroup = groupFilter;
    }

    public int getLayoutRes() {
        return outlet_res_id;
    }

    public void setLayoutRes(int layout_res) {
        this.outlet_res_id = layout_res;
    }

    public void setListItemMenu(ListItemMenu listItemMenu) {
        this.mListContextMenu = listItemMenu;
    }

    public boolean isShowingHidden() {
        return showHidden;
    }

    public void setRemoveAnimation(RemoveAnimation removeAnimation) {
        removeAnimationWeakReference = new WeakReference<>(removeAnimation);
    }

    public void setShowHidden(boolean b) {
        showHidden = b;
        SharedPrefs.setShowHiddenOutlets(showHidden);
        if (source != null)
            source.updateNow();
    }

    @Override
    public int getCount() {
        return all_outlets.size();
    }

    public DevicePort getItem(int position) {
        return all_outlets.get(position).port;
    }

    @Override
    public long getItemId(int position) {
        if (position >= all_outlets.size()) return -1;
        return all_outlets.get(position).id;
    }

    /**
     * Call this to load device ports from a scene.
     * This will not update the view.
     *
     * @param scene
     */
    public void loadItemsOfScene(Scene scene) {
        for (Scene.SceneItem sceneItem : scene.sceneItems) {
            DevicePort port = NetpowerctrlApplication.getDataController().findDevicePort(sceneItem.uuid);
            if (port == null) {
                continue;
            }
            addItem(port, sceneItem.command);
        }
    }

    /**
     * Create a list of scene items by all visible device ports.
     *
     * @return List of scene items.
     */
    public List<Scene.SceneItem> getScene() {
        List<Scene.SceneItem> list_of_scene_items = new ArrayList<>();
        for (DevicePortListItem info : all_outlets) {
            list_of_scene_items.add(new Scene.SceneItem(info.port.uuid, info.command_value));
        }
        return list_of_scene_items;
    }

    int findIndexByUUid(UUID uuid) {
        if (uuid == null)
            return -1;

        int i = 0;
        for (DevicePortListItem info : all_outlets) {
            if (info.port.uuid.equals(uuid))
                return i;
            ++i;
        }

        return -1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView != null) {
            current_devicePortViewHolder = (DevicePortViewHolder) convertView.getTag();
            if (!current_devicePortViewHolder.isStillValid(layoutChangeId))
                current_devicePortViewHolder = null;
            else
                current_devicePortViewHolder.isNew = false;
        } else
            current_devicePortViewHolder = null;

        DevicePortListItem info = all_outlets.get(position);

        if (current_devicePortViewHolder == null) {
            convertView = inflater.inflate(outlet_res_id, null);
            assert convertView != null;
            current_devicePortViewHolder = new DevicePortViewHolder(convertView, mListContextMenu, iconCache, layoutChangeId);
            convertView.setTag(current_devicePortViewHolder);
        }

        current_devicePortViewHolder.position = position;

        if (info.isEnabled() != current_devicePortViewHolder.entry.isEnabled())
            current_devicePortViewHolder.entry.setEnabled(!current_devicePortViewHolder.entry.isEnabled());

        current_devicePortViewHolder.title.setTypeface(null, info.port.Hidden ? Typeface.ITALIC : Typeface.NORMAL);
        current_devicePortViewHolder.title.setText(info.port.getDescription());
        current_devicePortViewHolder.title.setEnabled(info.isEnabled());

        current_devicePortViewHolder.subtitle.setTypeface(null, info.port.Hidden ? Typeface.ITALIC : Typeface.NORMAL);
        current_devicePortViewHolder.subtitle.setText(info.port.device.DeviceName);
        current_devicePortViewHolder.subtitle.setEnabled(info.isEnabled());

        long id = getItemId(position);
        if (animate_click_id != -1 && id == animate_click_id) {
            Animation a = AnimationUtils.loadAnimation(NetpowerctrlApplication.instance,
                    R.anim.button_zoom);
            a.reset();
            convertView.clearAnimation();
            convertView.startAnimation(a);
            animate_click_id = -1;
        }
        if (updated_id_list.contains(id)) {
            Log.w("base", "animate2 " + info.port.getDescription());
            Animation a = AnimationUtils.loadAnimation(NetpowerctrlApplication.instance,
                    R.anim.button_zoom);
            a.reset();
            convertView.clearAnimation();
            convertView.startAnimation(a);
            updated_id_list.remove(id);
        }

        return convertView;
    }

    //////////////// Adding ////////////////

    /**
     * Add an item to this device port adapter. The given command value will be used
     * if clicked on an item. Device ports are not added, if they are disabled or hidden or
     * if a group filter is active and the device port is not part of that group. See
     * {@link #setGroupFilter(java.util.UUID)} for setting a group filter.
     *
     * @param oi            The device port to add.
     * @param command_value The command value to issue if clicked or interacted with.
     */
    public void addItem(DevicePort oi, int command_value) {
        assert oi.device != null;
        if (oi.Disabled || (oi.Hidden && !showHidden))
            return;

        // FilterGroup
        if (filterGroup != null) {
            if (!oi.groups.contains(filterGroup))
                return;
        }

        boolean found = false;
        int destination_index = all_outlets.size();
        for (int i = 0; i < all_outlets.size(); ++i) {
            DevicePortListItem l = all_outlets.get(i);
            // Find the right position for the DevicePort.
            boolean behind_current = l.port.positionRequest > oi.positionRequest;
            if (!found && behind_current) {
                destination_index = i;
                found = true;
            }

            // If the same DevicePort already exists in this adapter, we will update that instead
            if (l.port.uuid.equals(oi.uuid)) {
                // Animate if value has changed
                if (l.command_value != command_value) {
                    updated_id_list.add(l.id);
                    Log.w("base", "animate1 " + oi.getDescription());

                }
                // Apply new values to existing item
                l.command_value = command_value;
                l.setPort(oi);
                l.clearState();
                return;
            }
        }

        // Insert or append new item
        DevicePortListItem new_oi = new DevicePortListItem(oi, command_value, nextId++);
        all_outlets.add(destination_index, new_oi);
    }

    public void addAll(DeviceInfo device) {
        device.lockDevicePorts();

        Iterator<DevicePort> it = device.getDevicePortIterator();
        while (it.hasNext()) {
            DevicePort oi = it.next();
            // Add item. On success add returned id to updated_id_list
            addItem(oi, oi.current_value);
        }
        device.releaseDevicePorts();
    }

    //////////////// Removing ////////////////

    public void removeAll(DeviceInfo device) {
        device.lockDevicePorts();
        Iterator<DevicePort> it = device.getDevicePortIterator();
        while (it.hasNext()) {
            removeAt(findIndexByUUid(it.next().uuid));
        }
        device.releaseDevicePorts();
    }

    public void removeAll(DevicePortsBaseAdapter adapter) {
        // ToBeRemoved will be an ordered list of indecies to be removed
        int[] toBeRemoved = new int[all_outlets.size()];
        int lenToBeRemoved = 0;
        for (int index = 0; index < all_outlets.size(); ++index) {
            for (DevicePortListItem adapter_list_item : adapter.all_outlets) {
                if (adapter_list_item.port.equals(all_outlets.get(index).port)) {
                    toBeRemoved[lenToBeRemoved] = index;
                    lenToBeRemoved++;
                }
            }
        }
        // Remove now
        for (int i = lenToBeRemoved - 1; i >= 0; --i)
            removeAt(toBeRemoved[i]);
    }

    public void clear() {
        all_outlets.clear();
        ++layoutChangeId;
        if (source != null && source.isAutomaticUpdateEnabled())
            source.updateNow();
        else
            notifyDataSetChanged();
    }

    public void removeAt(int position) {
        if (position == -1) return;
        RemoveAnimation a = removeAnimationWeakReference.get();
        if (a != null)
            a.beforeRemoval(position);
        all_outlets.remove(position);
    }

    public void remove(DevicePort oi) {
        removeAt(findIndexByUUid(oi.uuid));
    }

    public void invalidateViewHolders() {
        ++layoutChangeId;
        notifyDataSetChanged();
    }

    public void markAllRemoved() {
        for (DevicePortListItem item : all_outlets)
            item.markRemoved();
    }

    public void removeAllMarked() {
        int[] toBeRemoved = new int[all_outlets.size()];
        int lenToBeRemoved = 0;
        for (int index = 0; index < all_outlets.size(); ++index) {
            if (all_outlets.get(index).isMarkedRemoved()) {
                toBeRemoved[lenToBeRemoved++] = index;
            }
        }
        // Remove now
        for (int i = lenToBeRemoved - 1; i >= 0; --i)
            removeAt(toBeRemoved[i]);
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        RemoveAnimation a = removeAnimationWeakReference.get();
        if (a != null)
            a.animateRemoval();
    }

    //////////////// Sorting ////////////////

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

        notifyDataSetChanged();
    }

    @Override
    public boolean allowCustomSort() {
        return true;
    }

    @Override
    public void setSortOrder(int[] sortOrder) {
        if (sortOrder.length != all_outlets.size()) {
            Log.e("DevicePortsBaseAdapter", "setSortOrder length wrong");
            return;
        }

        // Assign positionRequest numbers
        DevicePortListItem temp;
        for (int i = 0; i < all_outlets.size(); ++i) {
            // change id
            all_outlets.get(sortOrder[i]).port.positionRequest = i;
            // exchange in list
            temp = all_outlets.get(i);
            all_outlets.set(i, all_outlets.get(sortOrder[i]));
            all_outlets.set(sortOrder[i], temp);
        }

        notifyDataSetChanged();
    }

}
