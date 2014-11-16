package oly.netpowerctrl.groups;

import android.content.Context;
import android.graphics.Bitmap;

import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.data.CollectionWithStorableItems;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.data.ObserverUpdateActions;

public class GroupCollection extends CollectionWithStorableItems<GroupCollection, Group> {
    static long nextStableID = 0;
    private final Group groupIndexOfHelper = new Group(null, null);

    //bitmap = Icons.loadBitmap(NetpowerctrlApplication.instance,uuid, Icons.IconType.GroupIcon);
    public void setBitmap(Context context, Group item, Bitmap bitmap) {
        if (item == null)
            return;
        item.bitmap = bitmap;
        LoadStoreIconData.saveIcon(context, bitmap, item.uuid.toString(),
                LoadStoreIconData.IconType.GroupIcon, LoadStoreIconData.IconState.StateUnknown);
        notifyObservers(item, ObserverUpdateActions.UpdateAction, -1);
    }

    /**
     * Add a group with the given name. If a group with this name already exist,
     * that group id will be returned and no new group will be added.
     *
     * @param name The name of the new group.
     * @return Return the group_index of the new group or the existing one.
     * Call {@link #get(int)} to get the group uid.
     */
    public int add(String name) {
        int index = 0;
        for (Group group : items) {
            if (group.name.equals(name))
                return index;
            ++index;
        }

        UUID group_uuid = UUID.randomUUID();
        Group group = new Group(group_uuid, name);
        items.add(group);
        index = items.size() - 1;
        save(group);
        notifyObservers(group, ObserverUpdateActions.AddAction, index);
        return index;
    }

    public Group get(UUID group_uuid) {
        int index = items.indexOf(groupIndexOfHelper.setUUID(group_uuid));
        if (index == -1) {
            return null;
        }
        return items.get(index);
    }

    public int indexOf(UUID group_uuid) {
        if (group_uuid == null)
            return -1;
        return items.indexOf(groupIndexOfHelper.setUUID(group_uuid));
    }

    /**
     * Edit an exiting groups name or add a new group with the given uuid and the given name.
     *
     * @param group_uuid Either existing uuid or new uuid
     * @param name       New name for the group
     */
    public void edit(UUID group_uuid, String name) {
        int index = items.indexOf(groupIndexOfHelper.setUUID(group_uuid));
        Group group;
        if (index == -1) {
            group = new Group(group_uuid, name);
            index = items.size();
            items.add(group);

            notifyObservers(group, ObserverUpdateActions.AddAction, index);

        } else {
            group = items.get(index);
            group.name = name;

            notifyObservers(group, ObserverUpdateActions.UpdateAction, index);
        }

        save(group);
    }

    public boolean remove(UUID group_uuid) {
        int index = items.indexOf(groupIndexOfHelper.setUUID(group_uuid));
        if (index == -1) {
            return false;
        }
        Group group = items.get(index);
        items.remove(index);
        if (storage != null)
            storage.remove(this, group);

        notifyObservers(group, ObserverUpdateActions.RemoveAction, index);
        return true;
    }

    public int length() {
        return items.size();
    }

    public String[] getGroupsArray() {
        String[] a = new String[items.size()];
        for (int i = 0; i < a.length; ++i)
            a[i] = items.get(i).name;
        return a;
    }

    /**
     * Return true if the group uuid at index is inside the groupUUids list.
     *
     * @param index
     * @param groupUUids
     * @return Return true if the group at the given index equals with one of the given uids.
     */
    public boolean equalsAtIndex(int index, List<UUID> groupUUids) {
        Group g = items.get(index);
        return groupUUids.contains(g.uuid);
    }

    @Override
    public String type() {
        return "groups";
    }
}
