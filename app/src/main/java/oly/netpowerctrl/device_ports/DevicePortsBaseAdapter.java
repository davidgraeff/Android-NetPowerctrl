package oly.netpowerctrl.device_ports;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.devices.DeviceInfo;
import oly.netpowerctrl.devices.DevicePort;
import oly.netpowerctrl.groups.GroupCollection;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.scenes.Scene;
import oly.netpowerctrl.utils.IconDeferredLoadingThread;
import oly.netpowerctrl.utils.ListItemMenu;
import oly.netpowerctrl.utils.SortCriteriaInterface;
import oly.netpowerctrl.utils.Sorting;
import oly.netpowerctrl.utils.gui.AnimationController;

public class DevicePortsBaseAdapter extends BaseAdapter implements SortCriteriaInterface {

    protected final IconDeferredLoadingThread mIconCache;
    final List<DevicePortListItem> mItems;
    private final LayoutInflater mInflater;
    // Source of values for this adapter.
    private final DevicePortSource mSource;
    protected int mOutlet_res_id = 0;
    protected boolean mShowHidden = true;
    protected DevicePortViewHolder mCurrent_devicePortViewHolder;
    // Some observers
    protected ListItemMenu mListContextMenu;
    // Animation ids
    protected WeakReference<AnimationController> mAnimationWeakReference = new WeakReference<>(null);
    private int mNextId = 0; // we need stable IDs
    // If you change the layout or an image we increment this layout change id
    // to invalidate ViewHolders (for reloading images or layout items).
    private int mLayoutChangeId = 0;
    private UUID mFilterGroup = null;
    private boolean mShowGroups;
    private int mItemsInRow = 1;

    DevicePortsBaseAdapter(Context context, ListItemMenu listContextMenu,
                           DevicePortSource source, IconDeferredLoadingThread iconCache,
                           boolean showGroups) {
        mSource = source;
        mListContextMenu = listContextMenu;
        mShowGroups = showGroups;
        mInflater = LayoutInflater.from(context);
        mIconCache = iconCache;
        mItems = new ArrayList<>();
        if (source != null) {
            source.setTargetAdapter(this);
        }
    }

    public DevicePortSource getSource() {
        return mSource;
    }

    public void setGroupFilter(UUID groupFilter) {
        this.mFilterGroup = groupFilter;
    }

    /**
     * Needs update of all items
     *
     * @param mShowGroups
     */
    public void setShowGroups(boolean mShowGroups) {
        this.mShowGroups = mShowGroups;
        if (mSource != null)
            mSource.updateNow();
    }

    public int getLayoutRes() {
        return mOutlet_res_id;
    }

    public void setLayoutRes(int layout_res) {
        this.mOutlet_res_id = layout_res;
    }

    public void setListItemMenu(ListItemMenu listItemMenu) {
        this.mListContextMenu = listItemMenu;
    }

    public boolean isShowingHidden() {
        return mShowHidden;
    }

    public void setRemoveAnimation(AnimationController animationController) {
        mAnimationWeakReference = new WeakReference<>(animationController);
    }

    public void setShowHidden(boolean b) {
        mShowHidden = b;
        SharedPrefs.setShowHiddenOutlets(mShowHidden);
        if (mSource != null)
            mSource.updateNow();
    }

    public void setItemsInRow(int itemsInRow) {
        if (itemsInRow == this.mItemsInRow)
            return;

        this.mItemsInRow = itemsInRow;
        if (itemsInRow == 1) {
            // Remove all group span items
            for (int i = mItems.size() - 1; i >= 0; --i) {
                DevicePortListItem c = mItems.get(i);
                switch (c.groupType()) {
                    case GROUP_SPAN_TYPE:
                    case PRE_GROUP_FILL_ELEMENT_TYPE:
                        mItems.remove(i);
                }
            }
        } else
            computeGroupSpans();
        notifyDataSetChanged();
    }

    @Override
    public boolean isEnabled(int position) {
        DevicePortListItem item = mItems.get(position);
        return item.port != null && item.port.device.isReachable();
    }

    @Override
    public int getViewTypeCount() {
        return DevicePort.DevicePortType.values().length + DevicePortListItem.groupTypeEnum.values().length - 1;
    }

    @Override
    public int getItemViewType(int position) {
        DevicePortListItem item = mItems.get(position);
        if (item.groupType() == DevicePortListItem.groupTypeEnum.NOGROUP_TYPE)
            return item.port.getType().ordinal() + DevicePortListItem.groupTypeEnum.values().length - 1;
        else
            return item.groupType().ordinal() - 1;
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    public DevicePort getItem(int position) {
        return mItems.get(position).port;
    }

    @Override
    public long getItemId(int position) {
        if (position >= mItems.size()) return -1;
        return mItems.get(position).id;
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
            addItem(port, sceneItem.command, false);
        }
        computeGroupSpans();
    }

    /**
     * Create a list of scene items by all visible device ports.
     *
     * @return List of scene items.
     */
    public List<Scene.SceneItem> getScene() {
        List<Scene.SceneItem> list_of_scene_items = new ArrayList<>();
        for (DevicePortListItem info : mItems) {
            if (info.port == null) // skip header items
                continue;
            list_of_scene_items.add(new Scene.SceneItem(info.port.uuid, info.command_value));
        }
        return list_of_scene_items;
    }

    int findIndexByUUid(UUID uuid) {
        if (uuid == null)
            return -1;

        int i = 0;
        for (DevicePortListItem info : mItems) {
            if (info.port == null) // skip header items
                continue;
            if (info.port.uuid.equals(uuid))
                return i;
            ++i;
        }

        return -1;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final DevicePortListItem item = mItems.get(position);
        final DevicePort port = item.port;

        if (convertView != null) {
            mCurrent_devicePortViewHolder = (DevicePortViewHolder) convertView.getTag();
            if (!mCurrent_devicePortViewHolder.isStillValid(mLayoutChangeId)) {
                mCurrent_devicePortViewHolder = null;
            } else
                mCurrent_devicePortViewHolder.isNew = false;
        } else {
            mCurrent_devicePortViewHolder = null;
        }

        if (mCurrent_devicePortViewHolder == null) {
            switch (item.groupType()) {
                case PRE_GROUP_FILL_ELEMENT_TYPE:
                case NOGROUP_TYPE:
                    convertView = mInflater.inflate(mOutlet_res_id, null);
                    break;
                case GROUP_TYPE:
                case GROUP_SPAN_TYPE:
                    convertView = mInflater.inflate(R.layout.list_icon_header, null);
                    break;
            }
            assert convertView != null;
            mCurrent_devicePortViewHolder = new DevicePortViewHolder(convertView, mListContextMenu, mLayoutChangeId, port == null);
            convertView.setTag(mCurrent_devicePortViewHolder);
        }

        mCurrent_devicePortViewHolder.position = position;

        if (port == null) { // header
            switch (item.groupType()) {
                case GROUP_SPAN_TYPE:
                case GROUP_TYPE:
                    mCurrent_devicePortViewHolder.title.setText(item.displayText);
                    break;
                case PRE_GROUP_FILL_ELEMENT_TYPE:
                    mCurrent_devicePortViewHolder.title.setText("");
            }
        } else { // no header
            mCurrent_devicePortViewHolder.title.setTypeface(
                    port.Hidden ? Typeface.MONOSPACE : Typeface.DEFAULT,
                    port.Hidden ? Typeface.ITALIC : Typeface.NORMAL);
            mCurrent_devicePortViewHolder.title.setText(port.getDescription());
            mCurrent_devicePortViewHolder.title.setEnabled(item.isEnabled());

            mCurrent_devicePortViewHolder.subtitle.setText(port.device.DeviceName);

            mCurrent_devicePortViewHolder.entry.setEnabled(item.isEnabled());

            if (port.device.isReachable())
                mCurrent_devicePortViewHolder.title.setPaintFlags(
                        mCurrent_devicePortViewHolder.title.getPaintFlags() & ~(Paint.STRIKE_THRU_TEXT_FLAG));
            else
                mCurrent_devicePortViewHolder.title.setPaintFlags(
                        mCurrent_devicePortViewHolder.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }

        return convertView;
    }

    //////////////// Group Spans //////////////

    /**
     * Always call this method after adding/removing items.
     * This is implicitly done if you set the finalAction parameter
     * of the add/remove methods to true.
     * If you remove/add items in a batch, you only have to call this
     * method once at the end and set the finalAction parameter
     * of the add/remove methods to false.
     * TODO optimize
     */
    public void computeGroupSpans() {
        if (mItemsInRow == 1) // Do nothing it only one item per row
            return;

//        int positionOfLastGroup = -1;
//        DevicePortListItem lastGroup = null;

        // Remove all group span items
        for (int i = mItems.size() - 1; i >= 0; --i) {
            DevicePortListItem c = mItems.get(i);
            switch (c.groupType()) {
                case GROUP_SPAN_TYPE:
                case PRE_GROUP_FILL_ELEMENT_TYPE:
                    mItems.remove(i);
            }
        }

        for (int i = 0; i < mItems.size(); ++i) {
            DevicePortListItem c = mItems.get(i);
            if (c.groupType() == DevicePortListItem.groupTypeEnum.GROUP_TYPE) {

                int added = 0;

                // Add group span items type2: add as many group span items, as itemPerRow-1
                int missingFillElements = mItemsInRow - 1;
                while (missingFillElements > 0) {
//                    Log.w("base","groupSpan "+c.displayText+" "+String.valueOf(i));
                    DevicePortListItem new_oi = DevicePortListItem.createGroupSpan(c, mNextId++);
                    mItems.add(i, new_oi);
                    ++added;
                    --missingFillElements;
                }

                // Add group span items type1: fill until group is in own row with normal but empty items
                missingFillElements = i % mItemsInRow;
                while (missingFillElements > 0) {
//                    Log.w("base","addPreFill "+c.displayText+" "+String.valueOf(i));
                    DevicePortListItem new_oi = DevicePortListItem.createGroupPreFillElemenet(c, mNextId++);
                    mItems.add(i, new_oi);
                    ++added;
                    --missingFillElements;
                }

                i += added;
            }
        }
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
    public void addItem(DevicePort oi, int command_value, boolean finalAction) {
        assert oi.device != null;
        if (oi.Disabled || (oi.Hidden && !mShowHidden))
            return;

        // FilterGroup
        if (mFilterGroup != null) {
            if (!oi.groups.contains(mFilterGroup))
                return;
        }

        if (!mShowGroups || oi.groups.isEmpty())
            addItemToGroup(oi, command_value, 0);
        else {
            for (UUID group : oi.groups) {
                // Get group header item
                int positionOfGroup = addHeaderIfNotExists(group, oi);
                // Increase child count
                mItems.get(positionOfGroup).groupItems++;
                // add child
                addItemToGroup(oi, command_value, positionOfGroup + 1);
            }
        }

        if (finalAction)
            computeGroupSpans();
    }

    private void addItemToGroup(DevicePort oi, int command_value, int start_position) {
        boolean found = false;
        AnimationController a = mAnimationWeakReference.get();

        int destination_index = mItems.size();
        for (int i = start_position; i < mItems.size(); ++i) {
            DevicePortListItem l = mItems.get(i);
            if (l.port == null) { // stop on header
                destination_index = i;
                break;
            }

            // Find the right position for the DevicePort.
            boolean behind_current = l.port.positionRequest > oi.positionRequest;
            if (!found && behind_current) {
                destination_index = i;
                found = true;
            }

            // If the same DevicePort already exists in this adapter, we will update that instead
            if (l.port.uuid.equals(oi.uuid)) {
                // Animate if value has changed
                if (l.command_value != command_value && a != null) {
                    a.addHighlight(l.id);
                }
                // Apply new values to existing item
                l.command_value = command_value;
                l.setPort(oi);
                l.clearState();
                return;
            }
        }

        // Insert or append new item
        DevicePortListItem new_oi = new DevicePortListItem(oi, command_value, mNextId++);
        mItems.add(destination_index, new_oi);
    }

    private int addHeaderIfNotExists(UUID group, DevicePort oi) {
        GroupCollection.GroupItem groupItem = NetpowerctrlApplication.getDataController().groupCollection.get(group);
        if (groupItem == null) {
            // Group does not exist. Remove it from oi
            oi.groups.remove(group);
            return 0;
        }

        // Try to find group first
        for (int i = 0; i < mItems.size(); ++i) {
            if (mItems.get(i).isGroup(group)) {
                // Return group
                return i;
            }
        }

        // Insert or append new group
        DevicePortListItem new_oi = new DevicePortListItem(group, groupItem.name, mNextId++);
        mItems.add(new_oi);
        return mItems.size() - 1;
    }

    public void addAll(DeviceInfo device, boolean finalAction) {
        device.lockDevicePorts();

        Iterator<DevicePort> it = device.getDevicePortIterator();
        while (it.hasNext()) {
            DevicePort oi = it.next();
            // Add item. On success add returned id to mUpdated_id_list
            addItem(oi, oi.current_value, false);
        }
        device.releaseDevicePorts();

        if (finalAction)
            computeGroupSpans();
    }

    //////////////// Removing ////////////////

    public void removeAll(DeviceInfo device, boolean finalAction) {
        device.lockDevicePorts();
        Iterator<DevicePort> it = device.getDevicePortIterator();
        while (it.hasNext()) {
            removeAt(findIndexByUUid(it.next().uuid), false);
        }
        device.releaseDevicePorts();

        if (finalAction)
            computeGroupSpans();
    }

    public void removeAll(DevicePortsBaseAdapter adapter, boolean finalAction) {
        // ToBeRemoved will be an ordered list of indecies to be removed
        int[] toBeRemoved = new int[mItems.size()];
        int lenToBeRemoved = 0;
        for (int index = 0; index < mItems.size(); ++index) {
            for (DevicePortListItem adapter_list_item : adapter.mItems) {
                if (adapter_list_item.port != null && adapter_list_item.port.equals(mItems.get(index).port)) {
                    toBeRemoved[lenToBeRemoved] = index;
                    lenToBeRemoved++;
                }
            }
        }
        // Remove now
        for (int i = lenToBeRemoved - 1; i >= 0; --i)
            removeAt(toBeRemoved[i], false);

        if (finalAction)
            computeGroupSpans();
    }

    public void clear() {
        mItems.clear();
        ++mLayoutChangeId;
        if (mSource != null && mSource.isAutomaticUpdateEnabled())
            mSource.updateNow();
        else
            notifyDataSetChanged();
    }

    public void removeAt(int position, boolean finalAction) {
        if (position == -1) return;
        AnimationController a = mAnimationWeakReference.get();
        if (a != null)
            a.beforeRemoval(position);

        mItems.remove(position);

        /**
         * Search for a header item before the given position.
         * If found it decrements the item count. If that reaches
         * 0 the group header item is removed.
         */
        for (int indexGroup = position - 1; indexGroup >= 0; --indexGroup) {
            DevicePortListItem headerItem = mItems.get(indexGroup);
            if (headerItem.port == null) { // is header
                if (--headerItem.groupItems > 0)
                    continue;

                // remove group
                if (a != null)
                    a.beforeRemoval(indexGroup);
                mItems.remove(indexGroup);
                break;
            }
        }

        if (finalAction)
            computeGroupSpans();
    }

    public void remove(DevicePort oi, boolean finalAction) {
        removeAt(findIndexByUUid(oi.uuid), finalAction);
    }

    public void invalidateViewHolders() {
        ++mLayoutChangeId;
        notifyDataSetChanged();
    }

    public void markAllRemoved() {
        for (DevicePortListItem item : mItems)
            item.markRemoved();
    }

    public void removeAllMarked(boolean finalAction) {
        int[] toBeRemoved = new int[mItems.size()];
        int lenToBeRemoved = 0;
        for (int index = 0; index < mItems.size(); ++index) {
            if (mItems.get(index).isMarkedRemoved()) {
                toBeRemoved[lenToBeRemoved++] = index;
            }
        }
        // Remove now
        for (int i = lenToBeRemoved - 1; i >= 0; --i)
            removeAt(toBeRemoved[i], false);

        if (finalAction)
            computeGroupSpans();
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        AnimationController a = mAnimationWeakReference.get();
        if (a != null)
            a.animate();
    }

    //////////////// Sorting ////////////////

    @Override
    public String[] getContentList(int startPosition) {
        int c = 0;
        for (DevicePortListItem mItem : mItems) {
            if (mItem.port != null)
                ++c;
        }

        String[] l = new String[c];
        c = 0;
        for (DevicePortListItem mItem : mItems) {
            DevicePort port = mItem.port;
            if (port == null)
                continue;
            l[c++] = port.device.DeviceName + ": " + port.getDescription();
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

    private void removeHeaders() {
        Iterator<DevicePortListItem> iterator = mItems.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().port == null)
                iterator.remove();
        }
    }

    @Override
    public void applySortCriteria(final boolean[] criteria) {
        removeHeaders();

        // Sort
        Sorting.qSort(mItems, 0, mItems.size() - 1, new Sorting.qSortComparable<DevicePortListItem>() {
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
        for (int i = 0; i < mItems.size(); ++i) {
            mItems.get(i).port.positionRequest = i;
        }

        if (mSource != null)
            mSource.updateNow();
    }

    @Override
    public boolean allowCustomSort() {
        return true;
    }

    @Override
    public void setSortOrder(int[] sortOrder) {
        removeHeaders();

        if (sortOrder.length != mItems.size()) {
            Log.e("DevicePortsBaseAdapter", "setSortOrder length wrong");
            if (mSource != null)
                mSource.updateNow();
            return;
        }

        // Assign positionRequest numbers
        DevicePortListItem temp;
        for (int i = 0; i < mItems.size(); ++i) {
            // change id
            mItems.get(sortOrder[i]).port.positionRequest = i;
            // exchange in list
            temp = mItems.get(i);
            mItems.set(i, mItems.get(sortOrder[i]));
            mItems.set(sortOrder[i], temp);
        }

        if (mSource != null)
            mSource.updateNow();
    }
}
