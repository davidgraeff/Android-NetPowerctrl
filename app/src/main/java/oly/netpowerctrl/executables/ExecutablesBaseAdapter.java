package oly.netpowerctrl.executables;

import android.graphics.Paint;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.IconDeferredLoadingThread;
import oly.netpowerctrl.device_base.executables.Executable;
import oly.netpowerctrl.groups.Group;
import oly.netpowerctrl.main.App;

public class ExecutablesBaseAdapter extends RecyclerView.Adapter<ExecutableViewHolder> {

    public final List<ExecutableAdapterItem> mItems;
    final IconDeferredLoadingThread mIconCache;
    // Source of values for this adapter.
    private final ExecutablesSourceBase mSource;
    protected int mNextId = 0; // we need stable IDs
    int mOutlet_res_id = 0;
    private UUID mFilterGroup = null;
    private boolean mShowGroups;
    private int mItemsInRow = 1;
    GridLayoutManager.SpanSizeLookup spanSizeLookup = new GridLayoutManager.SpanSizeLookup() {
        @Override
        public int getSpanSize(int position) {
            final ExecutableAdapterItem item = mItems.get(position);
            final Executable executable = item.getExecutable();
            if (executable == null)
                return mItemsInRow;
            else
                return 1;
        }
    };

    ExecutablesBaseAdapter(ExecutablesSourceBase source, IconDeferredLoadingThread iconCache,
                           boolean showGroups) {
        mSource = source;
        mShowGroups = showGroups;
        mIconCache = iconCache;
        mItems = new ArrayList<>();
        if (source != null) {
            source.setTargetAdapter(this);
        }
    }

    public ExecutablesSourceBase getSource() {
        return mSource;
    }

    public boolean setGroupFilter(UUID groupFilter) {
        boolean changed = groupFilter == null ? mFilterGroup != null : !groupFilter.equals(mFilterGroup);
        this.mFilterGroup = groupFilter;
        return changed;
    }

    public void setLayoutRes(int layout_res) {
        this.mOutlet_res_id = layout_res;
    }

    @Override
    public int getItemViewType(int position) {
        return mItems.get(position).getItemViewType() + mOutlet_res_id;
    }

    @Override
    public ExecutableViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        viewType -= mOutlet_res_id;

        ExecutableAdapterItem.groupTypeEnum groupTypeEnum;
        if (viewType >= 100) {
            groupTypeEnum = ExecutableAdapterItem.groupTypeEnum.values()[viewType - 100];
        } else
            groupTypeEnum = ExecutableAdapterItem.groupTypeEnum.NOGROUP_TYPE;

        View view = null;

        switch (groupTypeEnum) {
            case NOGROUP_TYPE:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(mOutlet_res_id, viewGroup, false);
                break;
            case GROUP_TYPE:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_header_icon, viewGroup, false);
                break;
        }

        return new ExecutableViewHolder(view, groupTypeEnum);
    }

    public GridLayoutManager.SpanSizeLookup getSpanSizeLookup() {
        return spanSizeLookup;
    }

    @Override
    public void onBindViewHolder(ExecutableViewHolder executableViewHolder, int position) {
        final ExecutableAdapterItem item = mItems.get(position);
        final Executable executable = item.getExecutable();

        executableViewHolder.position = position;

        if (executable == null) { // header
            executableViewHolder.title.setText(item.groupName);
            if (executableViewHolder.line != null)
                executableViewHolder.line.setVisibility(item.groupName.isEmpty() ? View.INVISIBLE : View.VISIBLE);
        } else { // no header
//            current_viewHolder.title.setTypeface(
//                    port.Hidden ? Typeface.MONOSPACE : Typeface.DEFAULT,
//                    port.Hidden ? Typeface.ITALIC : Typeface.NORMAL);

            if (executableViewHolder.subtitle != null) {
                executableViewHolder.subtitle.setText(executable.getDescription(App.instance));
            }

            executableViewHolder.title.setText(executable.getTitle(App.instance));
            executableViewHolder.title.setEnabled(executable.isEnabled());

            if (executable.isReachable())
                executableViewHolder.title.setPaintFlags(
                        executableViewHolder.title.getPaintFlags() & ~(Paint.STRIKE_THRU_TEXT_FLAG));
            else
                executableViewHolder.title.setPaintFlags(
                        executableViewHolder.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
    }

    public ExecutableAdapterItem getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        if (position >= mItems.size()) return -1;
        return mItems.get(position).id;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    //////////////// Group Spans //////////////

    public void setItemsInRow(int itemsInRow) {
        if (itemsInRow == this.mItemsInRow)
            return;

        this.mItemsInRow = itemsInRow;

        notifyDataSetChanged();
    }

    //////////////// Adding ////////////////

    /**
     * Add an item to this device port adapter. The given command value will be used
     * if clicked on an item. Device ports are not added, if they are disabled or hidden or
     * if a group filter is active and the device port is not part of that group. See
     * {@link #setGroupFilter(java.util.UUID)} for setting a group filter.
     *
     * @param executable    The device port to add.
     * @param command_value The command value to issue if clicked or interacted with.
     * @return Return true if a recomputation of the group spans is necessary (if the
     * device port didn't exist before)
     */
    public void addItem(Executable executable, int command_value) {
        // FilterGroup
        if (mFilterGroup != null) {
            if (!executable.getGroups().contains(mFilterGroup))
                return;
        }

        if (!mShowGroups || executable.getGroups().isEmpty() || mFilterGroup != null)
            addItemToGroup(executable, command_value, 0);
        else {
            for (UUID group : executable.getGroups()) {
                // Get group header item
                int positionOfGroup = addHeaderIfNotExists(group, executable);
                // add child
                boolean newItemAdded = addItemToGroup(executable, command_value, positionOfGroup + 1);
                if (newItemAdded && positionOfGroup != -1) {
                    // Increase child count
                    mItems.get(positionOfGroup).groupItems++;

                }
            }
        }
    }

    /**
     * @param executable     The device port to add.
     * @param command_value  The command value to issue if clicked or interacted with.
     * @param start_position Start to look from this index for a fitting position.
     * @return Return true if a new item has been added.
     * If it is only an update of an item false is returned.
     */
    private boolean addItemToGroup(Executable executable, int command_value, int start_position) {
        boolean found = false;

        int destination_index = mItems.size();
        for (int i = start_position; i < mItems.size(); ++i) {
            ExecutableAdapterItem l = mItems.get(i);
            if (l.getExecutable() == null) { // stop on header
                destination_index = i;
                break;
            }

            boolean behind_current = l.getExecutable().getTitle(App.instance).compareToIgnoreCase(executable.getTitle(App.instance)) >= 0;
            if (!found && behind_current) {
                destination_index = i;
                found = true;
            }

            // If the same DevicePort already exists in this adapter, we will update that instead
            if (l.getExecutableUid().equals(executable.getUid())) {
                // Apply new values to existing item
                l.command_value = command_value;
                l.setExecutable(executable);
                l.clearMarkRemoved();
                notifyItemChanged(i);
                return false;
            }
        }

        // Insert or append new item
        ExecutableAdapterItem new_oi = new ExecutableAdapterItem(executable, command_value, mNextId++);
        mItems.add(destination_index, new_oi);
        notifyItemInserted(destination_index);
        return true;
    }

    private int addHeaderIfNotExists(UUID group, Executable executable) {
        Group groupItem = AppData.getInstance().groupCollection.get(group);
        if (groupItem == null) {
            // Group does not exist. Remove it from oi
            executable.getGroups().remove(group);
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
        ExecutableAdapterItem new_oi = new ExecutableAdapterItem(group, groupItem.name, mNextId++);
        mItems.add(new_oi);
        notifyItemInserted(mItems.size() - 1);
        return mItems.size() - 1;
    }

    //////////////// Removing ////////////////

    public void clear() {
        int all = mItems.size();
        mItems.clear();

        if (mSource != null && mSource.isAutomaticUpdateEnabled())
            mSource.updateNow();
        else if (all > 0)
            notifyItemRangeRemoved(0, all - 1);
    }

    /**
     * Remove item at position.
     *
     * @param position Index of item to remove
     * @return Return amount of items that have been removed
     * (0, 1 or 2 if a group has been removed)
     */
    public int removeAt(int position) {
        if (position == -1) return 0;

        int removedItems = 1;
        mItems.remove(position);
        notifyItemRemoved(position);

        /**
         * Search for a header item before the given position.
         * If found it decrements the item count. If that reaches
         * 0 the group header item is removed.
         */
        for (int indexGroup = position - 1; indexGroup >= 0; --indexGroup) {
            ExecutableAdapterItem headerItem = mItems.get(indexGroup);
            if (headerItem.groupType() == ExecutableAdapterItem.groupTypeEnum.GROUP_TYPE) { // is header
                if (--headerItem.groupItems <= 0) {
                    mItems.remove(indexGroup);
                    notifyItemRemoved(indexGroup);
                    ++removedItems;
                }
                break;
            }
        }

        return removedItems;
    }

    public void markAllRemoved() {
        for (ExecutableAdapterItem item : mItems)
            item.markRemoved();
    }

    public void removeAllMarked() {
        for (int index = mItems.size() - 1; index >= 0; ) {
            if (mItems.get(index).isMarkedRemoved()) {
                index -= removeAt(index);
            } else
                --index;
        }
    }

    public void updateGroupName(UUID uuid, String name) {
        for (int i = 0; i < mItems.size(); ++i) {
            if (mItems.get(i).isGroup(uuid)) {
                mItems.get(i).groupName = name;
                notifyItemChanged(i);
                return;
            }
        }
    }
}
