package oly.netpowerctrl.executables.adapter;

import android.support.annotation.NonNull;

import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.groups.Group;
import oly.netpowerctrl.groups.GroupCollection;
import oly.netpowerctrl.utils.ObserverUpdateActions;
import oly.netpowerctrl.utils.onCollectionUpdated;

;

/**
 * Created by david on 07.07.14.
 */
public class InputGroupChanges extends AdapterInput implements onCollectionUpdated<GroupCollection, Group> {
    private GroupCollection groupCollection = null;

    @Override
    void onFinish() {
        if (groupCollection != null) groupCollection.unregisterObserver(this);
        groupCollection = null;
    }

    @Override
    void doUpdateNow() {

    }

    @Override
    void onStart(DataService dataService) {
        groupCollection = dataService.groups;
        groupCollection.registerObserver(this);
    }

    @Override
    public boolean updated(@NonNull GroupCollection collection, Group item, @NonNull ObserverUpdateActions action) {
        if (item == null)
            return true;

        ExecutablesAdapter adapter = adapterSource.getAdapter();
        if (adapter == null) {
            return true;
        }

        if (action == ObserverUpdateActions.UpdateAction) { // if a group is renamed just update existing items
            updateGroupName(item.uid, item.name);
        } else // make complete update if a group is removed or added or if an item is added to one or more groups.
            adapterSource.updateNow();
        return true;
    }


    public void updateGroupName(String groupUID, String name) {
        for (int i = 0; i < adapterSource.mItems.size(); ++i) {
            if (adapterSource.mItems.get(i).isGroup(groupUID)) {
                adapterSource.mItems.get(i).groupName = name;
                adapterSource.getAdapter().notifyItemChanged(i);
                return;
            }
        }
    }
}
