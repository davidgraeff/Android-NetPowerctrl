package oly.netpowerctrl.executables.adapter;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.data.onServiceReady;
import oly.netpowerctrl.data.query.onDataQueryCompleted;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.groups.Group;
import oly.netpowerctrl.ui.EmptyListener;

/**
 * Created by david on 04.11.14.
 */
public class AdapterSource implements onServiceReady, onDataQueryCompleted {
    public final List<ExecutableAdapterItem> mItems = new ArrayList<>();
    private final List<AdapterInput> sourceInputs = new ArrayList<>();
    private final List<AdapterFilter> mFilters = new ArrayList<>();
    protected int mNextId = 0; // we need stable IDs
    Executable ignoreUpdatesExecutable;
    private ExecutablesAdapter adapter;
    private boolean automaticUpdatesEnabled = false;
    private WeakReference<DataService> PluginServiceWeakReference = new WeakReference<>(null);
    private boolean mShowHeaders = true;
    private EmptyListener emptyListener = new EmptyListener() {
        public void onEmptyListener(boolean empty) {
        }
    };
    public AdapterSource(final AutoStartEnum autoStart) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (autoStart == AutoStartEnum.AutoStartOnServiceReady) {
                    DataService.observersServiceReady.register(AdapterSource.this);
                } else if (autoStart == AutoStartEnum.AutoStartAfterFirstQuery) {
                    DataService.observersDataQueryCompleted.register(AdapterSource.this);
                }
            }
        });
    }

    public void setEmptyListener(EmptyListener emptyListener) {
        this.emptyListener = emptyListener;
    }

    public ExecutableAdapterItem getItem(int position) {
        return mItems.get(position);
    }

    public boolean isShowHeaders() {
        return mShowHeaders;
    }

    public void setShowHeaders(boolean showHeaders) {
        this.mShowHeaders = showHeaders;
    }

    //////////////// Adding ////////////////

    /**
     * Add an item to this device port adapter. The given command value will be used
     * if clicked on an item. If one of the added filters decides against the executable
     * it will not be added.
     *
     * @param executable    The device port to add.
     * @param command_value The command value to issue if clicked or interacted with.
     */
    public void addItem(Executable executable, int command_value) {
        boolean empty = mItems.isEmpty();

        if (filtered(executable)) return;

        if (!mShowHeaders || executable.getGroupUIDs().isEmpty())
            addItemToGroup(executable, command_value, 0);
        else {
            for (String groupUID : executable.getGroupUIDs()) {
                // Get group header item
                int positionOfGroup = addHeaderIfNotExists(groupUID, executable);
                // add child
                boolean newItemAdded = addItemToGroup(executable, command_value, positionOfGroup + 1);
                if (newItemAdded && positionOfGroup != -1) {
                    // Increase child count
                    mItems.get(positionOfGroup).groupItems++;
                }
            }
        }

        if (empty) emptyListener.onEmptyListener(false);
    }

    /**
     * Return true if the given item would be filtered out.
     * @param executable The executable
     * @return True if filtered
     */
    public boolean filtered(Executable executable) {
        for (AdapterFilter filter : mFilters)
            if (filter.filter(executable)) return true;
        return false;
    }

    /**
     * @param executable     The device port to add.
     * @param command_value  The command value to issue if clicked or interacted with.
     * @param start_position Start to look from this index for a fitting position.
     * @return Return true if a new item has been added.
     * If it is only an update of an item false is returned.
     */
    private boolean addItemToGroup(Executable executable, int command_value, int start_position) {
        boolean positionFound = false;

        int destination_index = mItems.size();
        for (int i = start_position; i < mItems.size(); ++i) {
            ExecutableAdapterItem executableAdapterItem = mItems.get(i);
            if (executableAdapterItem.getExecutable() == null) { // stop on header
                destination_index = i;
                break;
            }

            // If the same DevicePort already exists in this adapter, we will update that instead
            if (executableAdapterItem.getExecutableUid().equals(executable.getUid())) {
                // Apply new values to existing item
                executableAdapterItem.command_value = command_value;
                executableAdapterItem.setExecutable(executable);
                executableAdapterItem.clearMarkRemoved();
                if (adapter != null) adapter.notifyItemChanged(i);
                return false;
            }

            if (!positionFound &&
                    executableAdapterItem.getExecutable().getTitle().compareTo(executable.getTitle()) >= 0) {
                destination_index = i;
                positionFound = true;
            }
        }

        // Insert or append new item
        ExecutableAdapterItem new_oi = new ExecutableAdapterItem(executable, command_value, mNextId++);
        mItems.add(destination_index, new_oi);
        if (adapter != null) adapter.notifyItemInserted(destination_index);
        return true;
    }

    private int addHeaderIfNotExists(String group, Executable executable) {
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

        DataService dataService = getPluginService();
        Group groupItem = dataService != null ? dataService.groups.getByUID(group) : null;
        if (groupItem == null) {
            // Group does not exist. Remove it from oi
            executable.getGroupUIDs().remove(group);
            return -1;
        }

        // Insert or append new group
        ExecutableAdapterItem new_oi = new ExecutableAdapterItem(group, groupItem.name, mNextId++);
        mItems.add(new_oi);
        if (adapter != null) adapter.notifyItemInserted(mItems.size() - 1);
        return mItems.size() - 1;
    }

    //////////////// Removing ////////////////

    public void clear() {
        int all = mItems.size();
        mItems.clear();

        if (all > 0 && adapter != null)
            adapter.notifyItemRangeRemoved(0, all - 1);

        if (all > 0) emptyListener.onEmptyListener(true);
    }

    /**
     * Return a list of all positions where the given executable resides. The list is sorted in reverse order
     * so that an iteration and removal, for example, is easily possible.
     *
     * @param uuid The uuid of an executable.
     * @param list The output list.
     */
    public void findPositionsByUUid(@NonNull String uuid, @NonNull List<Integer> list) {
        int i = -1;
        for (ExecutableAdapterItem info : mItems) {
            ++i;
            String uid = info.getExecutableUid();
            if (uid == null) // skip header items
                continue;
            if (uid.equals(uuid))
                list.add(0,i);
        }
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
        if (adapter != null) adapter.notifyItemRemoved(position);

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
                    if (adapter != null) adapter.notifyItemRemoved(indexGroup);
                    ++removedItems;
                }
                break;
            }
        }

        if (mItems.isEmpty()) emptyListener.onEmptyListener(true);

        return removedItems;
    }

    void removeAllMarked() {
        for (int index = mItems.size() - 1; index >= 0; ) {
            if (mItems.get(index).isMarkedRemoved()) {
                index -= removeAt(index);
            } else
                --index;
        }
    }


    @Override
    public boolean onDataQueryFinished(DataService dataService) {
        start(true, dataService);
        return false;
    }

    @Override
    public boolean onServiceReady(DataService service) {
        start(true, service);
        return false;
    }

    @Override
    public void onServiceFinished(DataService service) {
        for (AdapterInput base : sourceInputs)
            base.onFinish();
        PluginServiceWeakReference = new WeakReference<>(null);
    }

    public void ignoreUpdates(Executable executable) {
        ignoreUpdatesExecutable = executable;
    }

    public DataService getPluginService() {
        return PluginServiceWeakReference.get();
    }

    public ExecutablesAdapter getAdapter() {
        return adapter;
    }

    /**
     * If automatic updates are automaticUpdates, new values are automatically
     * synced with the target adapter.
     *
     * @param automaticUpdates Enable automatic updates
     * @param dataService          A valid reference to DataService. Call this method in onServiceReady
     *                         to get a valid reference.
     */
    final public void start(boolean automaticUpdates, DataService dataService) {
        this.PluginServiceWeakReference = new WeakReference<>(dataService);
        automaticUpdatesEnabled = automaticUpdates;

        for (AdapterInput base : sourceInputs)
            base.onStart(dataService);

        if (adapter != null)
            adapter.notifyDataSetChanged();
    }

    final public boolean isAutomaticUpdateEnabled() {
        return automaticUpdatesEnabled;
    }

    final public void setTargetAdapter(ExecutablesAdapter adapter) {
        this.adapter = adapter;
        if (mItems.size() > 0)
            adapter.notifyDataSetChanged();
    }

    /**
     * Update the target adapter with new values immediately.
     * You do not need to call this method if automatic updates
     * are enabled.
     *
     * @see #start(boolean, DataService)
     */
    final public void updateNow() {
        if (adapter == null || PluginServiceWeakReference.get() == null || sourceInputs.isEmpty())
            return;

        for (ExecutableAdapterItem item : mItems)
            item.markRemoved();

        for (AdapterInput base : sourceInputs)
            base.doUpdateNow();

        removeAllMarked();

        if (mItems.isEmpty()) {
            adapter.notifyDataSetChanged();
            emptyListener.onEmptyListener(true);
        }
    }

    public void addInput(AdapterInput... inputs) {
        DataService dataService = getPluginService();
        for (AdapterInput adapterInput : inputs) {
            sourceInputs.add(adapterInput);
            adapterInput.setAdapterSource(this);
            if (dataService != null)
                adapterInput.onStart(dataService);
        }
    }

    public void addFilter(AdapterFilter filter) {
        mFilters.add(filter);
        filter.setAdapterSource(this);
    }

    public boolean isEmpty() {
        return mItems.isEmpty();
    }

    public enum AutoStartEnum {
        AutoStartOnServiceReady, AutoStartAfterFirstQuery, ManualCallToStart
    }
}
