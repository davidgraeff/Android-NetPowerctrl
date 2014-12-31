package oly.netpowerctrl.executables;

import android.support.annotation.NonNull;

import java.util.UUID;

import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.groups.Group;
import oly.netpowerctrl.groups.GroupCollection;

/**
 * Created by david on 07.07.14.
 */
public class AdapterSourceInputGroups extends AdapterSourceInput implements onCollectionUpdated<Object, Object> {
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
    void onStart(AppData appData) {
        groupCollection = appData.groupCollection;
        groupCollection.registerObserver(this);
    }

    @Override
    public boolean updated(@NonNull Object collection, Object item, @NonNull ObserverUpdateActions action, int position) {
        if (item == null)
            return true;

        ExecutablesAdapter adapter = adapterSource.getAdapter();
        if (adapter == null) {
            return true;
        }

        if (action == ObserverUpdateActions.UpdateAction) { // if a group is renamed just update existing items
            Group group = ((Group) item);
            updateGroupName(group.uuid, group.name);
            adapterSource.sourceChanged();
        } else // make complete update if a group is removed or added or if an item is added to one or more groups.
            adapterSource.updateNow();
        return true;
    }


    public void updateGroupName(UUID uuid, String name) {
        for (int i = 0; i < adapterSource.mItems.size(); ++i) {
            if (adapterSource.mItems.get(i).isGroup(uuid)) {
                adapterSource.mItems.get(i).groupName = name;
                adapterSource.getAdapter().notifyItemChanged(i);
                return;
            }
        }
    }
}
