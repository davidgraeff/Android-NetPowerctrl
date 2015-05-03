package oly.netpowerctrl.groups;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.UUID;

import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.data.storage_container.CollectionMapItems;
import oly.netpowerctrl.utils.ObserverUpdateActions;

;

public class GroupCollection extends CollectionMapItems<GroupCollection, Group> {
    static long nextStableID = 0;

    public GroupCollection(DataService dataService) {
        super(dataService, "groups");
    }

    public Group getByUID(String groupUID) {
        return items.get(groupUID);
    }

    /**
     * Edit an exiting groups name or add a new group with the given uid and the given name.
     *
     * @param groupUID Either existing uid or null
     * @param name       New name for the group
     * @return Return the group uid.
     */
    public Group put(@Nullable String groupUID, @NonNull String name) {
        Group group = items.get(groupUID);
        if (group == null) {
            group = new Group(UUID.randomUUID().toString(), name);
            items.put(group.getUid(), group);
            notifyObservers(group, ObserverUpdateActions.AddAction);
        } else {
            group.name = name;
            notifyObservers(group, ObserverUpdateActions.UpdateAction);
        }

        storage.save(group);
        return group;
    }

    public boolean remove(String groupUID) {
        Group group = items.get(groupUID);
        if (group == null) return false;

        items.remove(groupUID);
        storage.remove(group);
        notifyObservers(group, ObserverUpdateActions.RemoveAction);
        return true;
    }

//    public boolean equalsAtIndex(int index, List<String> listGroupUID) {
//        Group g = items.get(index);
//        return listGroupUID.contains(g.uid);
//    }

    public void executableToGroupAdded() {
        notifyObservers(null, ObserverUpdateActions.ClearAndNewAction);
    }
}
