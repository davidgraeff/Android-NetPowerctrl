package oly.netpowerctrl.device_ports;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
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
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.groups.GroupCollection;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.scenes.Scene;
import oly.netpowerctrl.utils.IconDeferredLoadingThread;
import oly.netpowerctrl.utils.ListItemMenu;
import oly.netpowerctrl.utils.SortCriteriaInterface;
import oly.netpowerctrl.utils.Sorting;
import oly.netpowerctrl.utils_gui.AnimationController;

public class DevicePortsBaseAdapter extends BaseAdapter implements SortCriteriaInterface,
        SharedPrefs.IShowBackground {

    protected final IconDeferredLoadingThread mIconCache;
    final List<DevicePortAdapterItem> mItems;
    private final LayoutInflater mInflater;
    // Source of values for this adapter.
    private final DevicePortSource mSource;
    protected int mOutlet_res_id = 0;
    protected boolean mShowHidden = true;
    protected DevicePortViewHolder cViewHolder;
    // Some observers
    protected ListItemMenu mListContextMenu;
    // Animation ids
    protected WeakReference<AnimationController> mAnimationWeakReference = new WeakReference<>(null);
    private boolean drawShadows;
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
        drawShadows = SharedPrefs.isBackground();
        SharedPrefs.getInstance().registerShowBackground(this);
    }

    @Override
    public void backgroundChanged(boolean showBackground) {
        drawShadows = showBackground;
        notifyDataSetChanged();
    }

    public DevicePortSource getSource() {
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

    @Override
    public boolean isEnabled(int position) {
        DevicePortAdapterItem item = mItems.get(position);
        return item.port != null && item.port.device.getFirstReachableConnection() != null;
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
        for (DevicePortAdapterItem info : mItems) {
            if (info.port == null) // skip header items
                continue;
            list_of_scene_items.add(new Scene.SceneItem(info.port.uuid, info.command_value));
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

    @SuppressLint("InflateParams")
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final DevicePortAdapterItem item = mItems.get(position);
        final DevicePort port = item.port;

        if (convertView != null) {
            cViewHolder = (DevicePortViewHolder) convertView.getTag();
            if (!cViewHolder.isStillValid(mLayoutChangeId)) {
                cViewHolder = null;
            } else
                cViewHolder.isNew = false;
        } else {
            cViewHolder = null;
        }

        if (cViewHolder == null) {
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
            cViewHolder = new DevicePortViewHolder(convertView, mListContextMenu, mLayoutChangeId,
                    item.groupType());

            convertView.setTag(cViewHolder);
        }

        cViewHolder.position = position;

        if (port == null) { // header
            switch (item.groupType()) {
                case GROUP_SPAN_TYPE:
                case GROUP_TYPE:
                    cViewHolder.title.setText(item.displayText);
                    if (cViewHolder.line != null)
                        cViewHolder.line.setVisibility(item.displayText.isEmpty() ? View.INVISIBLE : View.VISIBLE);
                    break;
            }
        } else { // no header
            cViewHolder.title.setTypeface(
                    port.Hidden ? Typeface.MONOSPACE : Typeface.DEFAULT,
                    port.Hidden ? Typeface.ITALIC : Typeface.NORMAL);
            cViewHolder.title.setText(port.getDescription());
            cViewHolder.title.setEnabled(item.isEnabled());
            if (drawShadows)
                cViewHolder.title.setShadowLayer(4f, 0, 0, Color.WHITE);

            cViewHolder.subtitle.setText(port.device.DeviceName);

            if (cViewHolder.progress != null) {
                if (cViewHolder.progress.getVisibility() == View.VISIBLE && item.isEnabled() &&
                        cViewHolder.animation == null) {
                    animateHideProgressbar(cViewHolder.position, cViewHolder);
                } else
                    cViewHolder.progress.setVisibility(item.isEnabled() ? View.GONE : View.VISIBLE);
            }

            if (port.device.getFirstReachableConnection() != null)
                cViewHolder.title.setPaintFlags(
                        cViewHolder.title.getPaintFlags() & ~(Paint.STRIKE_THRU_TEXT_FLAG));
            else
                cViewHolder.title.setPaintFlags(
                        cViewHolder.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }

        return convertView;
    }

    private void animateHideProgressbar(final int current_position, final DevicePortViewHolder viewHolder) {
        Animation a = new AlphaAnimation(1, 0);
        cViewHolder.animation = a;
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
        cViewHolder.progress.startAnimation(a);
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
     */
    public void addItem(DevicePort devicePort, int command_value, boolean finalAction) {
        assert devicePort.device != null;
        if (devicePort.Disabled || (devicePort.Hidden && !mShowHidden))
            return;

        // FilterGroup
        if (mFilterGroup != null) {
            if (!devicePort.groups.contains(mFilterGroup))
                return;
        }

        if (!mShowGroups || devicePort.groups.isEmpty() || mFilterGroup != null)
            addItemToGroup(devicePort, command_value, 0);
        else {
            for (UUID group : devicePort.groups) {
                // Get group header item
                int positionOfGroup = addHeaderIfNotExists(group, devicePort);
                // add child
                if (addItemToGroup(devicePort, command_value, positionOfGroup + 1) &&
                        positionOfGroup != -1) {
                    // Increase child count
                    mItems.get(positionOfGroup).groupItems++;

                }
            }
        }

        if (finalAction)
            computeGroupSpans();
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
        AnimationController a = mAnimationWeakReference.get();

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
                // Animate if value has changed
                if (l.command_value != command_value && a != null) {
                    a.addSmallHighlight(l.id);
                }
                // Apply new values to existing item
                l.command_value = command_value;
                l.setPort(devicePort);
                l.clearState();
                return false;
            }
        }

        // Insert or append new item
        DevicePortAdapterItem new_oi = new DevicePortAdapterItem(devicePort, command_value, mNextId++);
        mItems.add(destination_index, new_oi);
        return true;
    }

    private int addHeaderIfNotExists(UUID group, DevicePort oi) {
        GroupCollection.GroupItem groupItem = NetpowerctrlApplication.getDataController().groupCollection.get(group);
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

        Iterator<DevicePort> it = device.getDevicePortIterator();
        while (it.hasNext()) {
            DevicePort oi = it.next();
            addItem(oi, oi.current_value, false);
        }
        device.releaseDevicePorts();

        if (finalAction)
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
            DevicePortAdapterItem headerItem = mItems.get(indexGroup);
            if (headerItem.port == null) { // is header
                --headerItem.groupItems;
                break;
            }
        }

        if (finalAction)
            computeGroupSpans();
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
}
