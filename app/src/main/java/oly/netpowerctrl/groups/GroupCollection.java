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

    //bitmap = Icons.loadIcon(NetpowerctrlApplication.instance,uuid, Icons.IconType.GroupIcon);
    public void setBitmap(Context context, Group item, Bitmap bitmap) {
        if (item == null)
            return;
        item.bitmap = bitmap;
        LoadStoreIconData.saveIcon(context, bitmap, item.uuid,
                LoadStoreIconData.IconType.GroupIcon, LoadStoreIconData.IconState.StateUnknown);
        notifyObservers(item, ObserverUpdateActions.UpdateAction);
    }

    /**
     * Add a group with the given name. If a group with this name already exist,
     * that group id will be returned and no new group will be added.
     *
     * @param name
     * @return
     */
    public UUID add(String name) {
        for (Group group : items)
            if (group.name.equals(name))
                return group.uuid;

        UUID group_uuid = UUID.randomUUID();
        Group group = new Group(group_uuid, name);
        items.add(group);
        if (storage != null)
            storage.save(this, group);
        notifyObservers(group, ObserverUpdateActions.AddAction);
        return group_uuid;
    }

    public Group get(UUID group_uuid) {
        int index = items.indexOf(groupIndexOfHelper.setUUID(group_uuid));
        if (index == -1) {
            return null;
        }
        return items.get(index);
    }

    public int indexOf(UUID group_uuid) {
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
            items.add(group);
        } else {
            group = items.get(index);
            group.name = name;
        }

        if (storage != null)
            storage.save(this, group);

        notifyObservers(group, ObserverUpdateActions.UpdateAction);
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

        notifyObservers(group, ObserverUpdateActions.RemoveAction);
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
     * @return
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
