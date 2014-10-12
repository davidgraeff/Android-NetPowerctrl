package oly.netpowerctrl.device_ports;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.BaseAdapter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.IconDeferredLoadingThread;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.groups.Group;
import oly.netpowerctrl.scenes.Scene;
import oly.netpowerctrl.scenes.SceneItem;
import oly.netpowerctrl.utils.AnimationController;
import oly.netpowerctrl.utils.SortCriteriaInterface;
import oly.netpowerctrl.utils.Sorting;
import oly.netpowerctrl.utils.controls.onListItemElementClicked;

public class DevicePortsBaseAdapter extends BaseAdapter implements SortCriteriaInterface {

    final IconDeferredLoadingThread mIconCache;
    final List<DevicePortAdapterItem> mItems;
    private final LayoutInflater mInflater;
    // Source of values for this adapter.
    private final DevicePortSourceInterface mSource;
    int mOutlet_res_id = 0;
    boolean mShowHidden = true;
    DevicePortViewHolder current_viewHolder;
    // Some observers
    onListItemElementClicked mListContextMenu;
    // Animation ids
    WeakReference<AnimationController> mAnimationWeakReference = new WeakReference<>(null);

    private int mNextId = 0; // we need stable IDs
    // If you change the layout or an image we increment this layout change id
    // to invalidate ViewHolders (for reloading images or layout items).
    private int mLayoutChangeId = 0;
    private UUID mFilterGroup = null;
    private boolean mShowGroups;
    private int mItemsInRow = 1;

    DevicePortsBaseAdapter(Context context, onListItemElementClicked listContextMenu,
                           DevicePortSourceInterface source, IconDeferredLoadingThread iconCache,
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

    public DevicePortSourceInterface getSource() {
        return mSource;
    }

    public boolean setGroupFilter(UUID groupFilter) {
        boolean changed = groupFilter == null ? mFilterGroup != null : !groupFilter.equals(mFilterGroup);
        this.mFilterGroup = groupFilter;
        return changed;
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

    public void setListItemElementClickedListener(onListItemElementClicked listItemMenu) {
        this.mListContextMenu = listItemMenu;
    }

    public boolean isShowingHidden() {
        return mShowHidden;
    }

    public void setAnimationController(AnimationController animationController) {
        mAnimationWeakReference = new WeakReference<>(animationController);
    }

    public void setShowHidden(boolean b) {
        mShowHidden = b;
        SharedPrefs.getInstance().setShowHiddenOutlets(mShowHidden);
        if (mSource != null)
            mSource.updateNow();
    }

    @Override
    public int getViewTypeCount() {
        return DevicePort.DevicePortType.values().length + DevicePortAdapterItem.groupTypeEnum.values().length - 1;
    }

    @Override
    public int getItemViewType(int position) {
        DevicePortAdapterItem item = mItems.get(position);
        if (item.groupType() == DevicePortAdapterItem.groupTypeEnum.NOGROUP_TYPE)
            return item.port.getType().ordinal() + DevicePortAdapterItem.groupTypeEnum.values().length - 1;
        else
            return item.groupType().ordinal() - 1;
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    public DevicePort getDevicePort(int position) {
        return mItems.get(position).port;
    }

    public DevicePortAdapterItem getItem(int position) {
        return mItems.get(position);
    }

    public DevicePortAdapterItem getGroup(UUID uuid) {
        for (DevicePortAdapterItem item : mItems)
            if (uuid.equals(item.groupID()))
                return item;
        return null;
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
        for (SceneItem sceneItem : scene.sceneItems) {
            DevicePort port = AppData.getInstance().findDevicePort(sceneItem.uuid);
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
    public List<SceneItem> getScene() {
        List<SceneItem> list_of_scene_items = new ArrayList<>();
        for (DevicePortAdapterItem info : mItems) {
            if (info.port == null) // skip header items
                continue;
            list_of_scene_items.add(new SceneItem(info.port.uuid, info.command_value));
        }
        return list_of_scene_items;
    }

    int findPositionByUUid(UUID uuid) {
        if (uuid == null)
            return -1;

        int i = -1;
        for (DevicePortAdapterItem info : mItems) {
            ++i;
            if (info.port == null) // skip header items
                continue;
            if (info.port.uuid.equals(uuid))
                return i;
        }

        return -1;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final DevicePortAdapterItem item = mItems.get(position);
        final DevicePort port = item.port;

        if (convertView != null) {
            current_viewHolder = (DevicePortViewHolder) convertView.getTag();
            if (!current_viewHolder.isStillValid(mLayoutChangeId)) {
                current_viewHolder = null;
            } else
                current_viewHolder.isNew = false;
        } else {
            current_viewHolder = null;
        }

        if (current_viewHolder == null) {
            switch (item.groupType()) {
                case PRE_GROUP_FILL_ELEMENT_TYPE:
                case NOGROUP_TYPE:
                    convertView = mInflater.inflate(mOutlet_res_id, parent, false);
                    break;
                case GROUP_TYPE:
                case GROUP_SPAN_TYPE:
                    convertView = mInflater.inflate(R.layout.list_header_icon, parent, false);
                    break;
            }
            assert convertView != null;
            current_viewHolder = new DevicePortViewHolder(convertView, mListContextMenu, mLayoutChangeId,
                    item.groupType());

            convertView.setTag(current_viewHolder);
        }

        current_viewHolder.position = position;

        if (port == null) { // header
            switch (item.groupType()) {
                case GROUP_SPAN_TYPE:
                case GROUP_TYPE:
                    current_viewHolder.title.setText(item.displayText);
                    if (current_viewHolder.line != null)
                        current_viewHolder.line.setVisibility(item.displayText.isEmpty() ? View.INVISIBLE : View.VISIBLE);
                    break;
            }
        } else { // no header
            current_viewHolder.title.setTypeface(
                    port.Hidden ? Typeface.MONOSPACE : Typeface.DEFAULT,
                    port.Hidden ? Typeface.ITALIC : Typeface.NORMAL);
            current_viewHolder.title.setText(port.getDescription());
            current_viewHolder.title.setEnabled(item.isEnabled());

            if (current_viewHolder.subtitle != null) {
                current_viewHolder.subtitle.setText(port.device.DeviceName);
            }

            if (current_viewHolder.progress != null) {
                if (current_viewHolder.progress.getVisibility() == View.VISIBLE && item.isEnabled() &&
                        current_viewHolder.animation == null) {
                    animateHideProgressbar(current_viewHolder.position, current_viewHolder);
                } else
                    current_viewHolder.progress.setVisibility(item.isEnabled() ? View.GONE : View.VISIBLE);
            }

            if (port.device.getFirstReachableConnection() != null)
                current_viewHolder.title.setPaintFlags(
                        current_viewHolder.title.getPaintFlags() & ~(Paint.STRIKE_THRU_TEXT_FLAG));
            else
                current_viewHolder.title.setPaintFlags(
                        current_viewHolder.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }

        return convertView;
    }

    private void animateHideProgressbar(final int current_position, final DevicePortViewHolder viewHolder) {
        Animation a = new AlphaAnimation(1, 0);
        current_viewHolder.animation = a;
        a.setDuration(1200);
        a.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (current_position == viewHolder.position)
                    viewHolder.progress.setVisibility(View.GONE);
                viewHolder.animation = null;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        current_viewHolder.progress.startAnimation(a);
    }

    //////////////// Group Spans //////////////

    public void setItemsInRow(int itemsInRow) {
        if (itemsInRow == this.mItemsInRow)
            return;

        this.mItemsInRow = itemsInRow;
        if (itemsInRow <= 1) {
            removeGroupSpanAndFillElements();
        } else {
            computeGroupSpans();
        }
        notifyDataSetChanged();
    }

    private void removeGroupSpanAndFillElements() {
        AnimationController a = mAnimationWeakReference.get();
        // Remove all group span items
        for (int i = mItems.size() - 1; i >= 0; --i) {
            DevicePortAdapterItem c = mItems.get(i);
            switch (c.groupType()) {
                case GROUP_SPAN_TYPE:
                case PRE_GROUP_FILL_ELEMENT_TYPE:
                    if (a != null)
                        a.beforeRemoval(i);
                    mItems.remove(i);
                    break;
                case GROUP_TYPE:
                    if (c.groupItems == 0) { // remove empty groups
                        if (a != null)
                            a.beforeRemoval(i);
                        mItems.remove(i);
                    }
            }
        }
    }

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
        if (mItemsInRow <= 1) // Do nothing it only one item per row
            return;

        removeGroupSpanAndFillElements();

        for (int i = 0; i < mItems.size(); ++i) {
            DevicePortAdapterItem c = mItems.get(i);
            if (c.groupType() == DevicePortAdapterItem.groupTypeEnum.GROUP_TYPE) {

                int added = 0;

                // Add group span items type2: add as many group span items, as itemPerRow-1
                int missingFillElements;
                // Not all group headers need a title. If we are at the end of the list,
                // only some of the headers should have a title.
                int elementsWithGroupTitle = mItems.size() - i - 1 < mItemsInRow ? mItems.size() - i - 1 : mItemsInRow;
                for (int missingElements = 1; missingElements < mItemsInRow; ++missingElements) {
//                    Log.w("base","groupSpan "+c.displayText+" "+String.valueOf(i));
                    DevicePortAdapterItem new_oi = DevicePortAdapterItem.createGroupSpan(c,
                            missingElements < elementsWithGroupTitle, mNextId++);
                    mItems.add(i + missingElements, new_oi);
                    ++added;
                }

                // Add group span items type1: fill until group is in own row with normal but empty items
                missingFillElements = (i % mItemsInRow);
                if (missingFillElements != 0)
                    missingFillElements = mItemsInRow - missingFillElements;
                while (missingFillElements > 0) {
//                    Log.w("base","addPreFill "+c.displayText+" "+String.valueOf(i));
                    DevicePortAdapterItem new_oi = DevicePortAdapterItem.createGroupPreFillElement(c, mNextId++);
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
     * @param devicePort    The device port to add.
     * @param command_value The command value to issue if clicked or interacted with.
     * @return Return true if a recomputation of the group spans is necessary (if the
     * device port didn't exist before)
     */
    public boolean addItem(DevicePort devicePort, int command_value, boolean finalAction) {
        assert devicePort.device != null;
        if (devicePort.Disabled || (devicePort.Hidden && !mShowHidden))
            return false;

        // FilterGroup
        if (mFilterGroup != null) {
            if (!devicePort.groups.contains(mFilterGroup))
                return false;
        }

        boolean needGroupSpanRecompute = false;
        if (!mShowGroups || devicePort.groups.isEmpty() || mFilterGroup != null)
            addItemToGroup(devicePort, command_value, 0);
        else {
            for (UUID group : devicePort.groups) {
                // Get group header item
                int positionOfGroup = addHeaderIfNotExists(group, devicePort);
                // add child
                boolean newItemAdded = addItemToGroup(devicePort, command_value, positionOfGroup + 1);
                needGroupSpanRecompute |= newItemAdded;
                if (newItemAdded && positionOfGroup != -1) {
                    // Increase child count
                    mItems.get(positionOfGroup).groupItems++;

                }
            }
        }

        if (finalAction) {
            computeGroupSpans();
            return false;
        }
        return needGroupSpanRecompute;
    }

    /**
     * @param devicePort     The device port to add.
     * @param command_value  The command value to issue if clicked or interacted with.
     * @param start_position Start to look from this index for a fitting position.
     * @return Return true if a new item has been added.
     * If it is only an update of an item false is returned.
     */
    private boolean addItemToGroup(DevicePort devicePort, int command_value, int start_position) {
        boolean found = false;
//        AnimationController a = mAnimationWeakReference.get();

        int destination_index = mItems.size();
        for (int i = start_position; i < mItems.size(); ++i) {
            DevicePortAdapterItem l = mItems.get(i);
            if (l.port == null) { // stop on header
                destination_index = i;
                break;
            }

            // Find the right position for the DevicePort.
            boolean behind_current = l.port.positionRequest > devicePort.positionRequest;
            if (!found && behind_current) {
                destination_index = i;
                found = true;
            }

            // If the same DevicePort already exists in this adapter, we will update that instead
            if (l.port.uuid.equals(devicePort.uuid)) {
                // Apply new values to existing item
                l.command_value = command_value;
                l.setPort(devicePort);
                l.clearMarkRemoved();
                return false;
            }
        }

        // Insert or append new item
        DevicePortAdapterItem new_oi = new DevicePortAdapterItem(devicePort, command_value, mNextId++);
        mItems.add(destination_index, new_oi);
        return true;
    }

    private int addHeaderIfNotExists(UUID group, DevicePort oi) {
        Group groupItem = AppData.getInstance().groupCollection.get(group);
        if (groupItem == null) {
            // Group does not exist. Remove it from oi
            oi.groups.remove(group);
            return -1;
        }

        // Try to find group first
        int group_position = -1;
        for (int i = 0; i < mItems.size(); ++i) {
            if (mItems.get(i).isGroup(group)) {
                group_position = i;
                mItems.get(i).clearMarkRemoved();
            } else if (group_position != -1)
                return group_position;
        }

        if (group_position != -1)
            return group_position;

        // Insert or append new group
        DevicePortAdapterItem new_oi = new DevicePortAdapterItem(group, groupItem.name, mNextId++);
        mItems.add(new_oi);
        return mItems.size() - 1;
    }

    /**
     * Add all device ports of a device to this adapter.
     *
     * @param device
     * @param finalAction If true, recalculate the group span items
     *                    (empty items to make the title items be aligned)
     */
    public void addAll(Device device, boolean finalAction) {
        device.lockDevicePorts();

        boolean needGroupSpanRecompute = false;

        Iterator<DevicePort> it = device.getDevicePortIterator();
        while (it.hasNext()) {
            DevicePort oi = it.next();
            needGroupSpanRecompute |= addItem(oi, oi.current_value, false);
        }
        device.releaseDevicePorts();

        if (finalAction && needGroupSpanRecompute)
            computeGroupSpans();
    }

    //////////////// Removing ////////////////

    public void removeAll(Device device, boolean finalAction) {
        device.lockDevicePorts();
        Iterator<DevicePort> it = device.getDevicePortIterator();
        while (it.hasNext()) {
            removeAt(findPositionByUUid(it.next().uuid), false);
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
            for (DevicePortAdapterItem adapter_list_item : adapter.mItems) {
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

    /**
     * Remove item at position.
     *
     * @param position    Index of item to remove
     * @param finalAction If this is a final action, group spans are recomputed.
     * @return Return amount of items that have been removed
     * (0, 1 or 2 if a group has been removed)
     */
    public int removeAt(int position, boolean finalAction) {
        if (position == -1) return 0;
        AnimationController a = mAnimationWeakReference.get();
        if (a != null)
            a.beforeRemoval(position);

        int removedItems = 1;
        mItems.remove(position);

        /**
         * Search for a header item before the given position.
         * If found it decrements the item count. If that reaches
         * 0 the group header item is removed.
         */
        for (int indexGroup = position - 1; indexGroup >= 0; --indexGroup) {
            DevicePortAdapterItem headerItem = mItems.get(indexGroup);
            if (headerItem.groupType() == DevicePortAdapterItem.groupTypeEnum.GROUP_TYPE) { // is header
                if (--headerItem.groupItems <= 0) {
                    mItems.remove(indexGroup);
                    ++removedItems;
                }
                break;
            }
        }

        if (finalAction)
            computeGroupSpans();

        return removedItems;
    }

    public void remove(DevicePort oi, boolean finalAction) {
        removeAt(findPositionByUUid(oi.uuid), finalAction);
    }

    public void invalidateViewHolders() {
        ++mLayoutChangeId;
        notifyDataSetChanged();
    }

    public void markAllRemoved() {
        for (DevicePortAdapterItem item : mItems)
            item.markRemoved();
    }

    public void removeAllMarked(boolean finalAction) {
        for (int index = mItems.size() - 1; index >= 0; ) {
            if (mItems.get(index).isMarkedRemoved()) {
                index -= removeAt(index, false);
            } else
                --index;
        }

        if (finalAction)
            computeGroupSpans();
    }

    @Override
    public void notifyDataSetChanged() {
        AnimationController a = mAnimationWeakReference.get();
        if (a != null)
            a.animate();
        super.notifyDataSetChanged();
    }

    //////////////// Sorting ////////////////

    @Override
    public String[] getContentList(int startPosition) {
        int c = 0;
        for (DevicePortAdapterItem mItem : mItems) {
            if (mItem.port != null)
                ++c;
        }

        String[] l = new String[c];
        c = 0;
        for (DevicePortAdapterItem mItem : mItems) {
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
        Iterator<DevicePortAdapterItem> iterator = mItems.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().port == null)
                iterator.remove();
        }
    }

    @Override
    public void applySortCriteria(final boolean[] criteria) {
        removeHeaders();

        // Sort
        Sorting.qSort(mItems, 0, mItems.size() - 1, new Sorting.qSortComparable<DevicePortAdapterItem>() {
            @Override
            public boolean isGreater(DevicePortAdapterItem first, DevicePortAdapterItem second) {
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
        DevicePortAdapterItem temp;
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

    public int indexOf(DevicePort port) {
        for (int i = 0; i < mItems.size(); ++i) {
            if (mItems.get(i).port != null && mItems.get(i).port.equals(port))
                return i;
        }
        return -1;
    }
}
