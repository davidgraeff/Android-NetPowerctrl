package oly.netpowerctrl.groups;

import android.graphics.Bitmap;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.utils.Icons;
import oly.netpowerctrl.utils.JSONHelper;

public class GroupCollection {
    private static long nextStableID = 0;
    private IGroupsSave storage;

    public void setStorage(IGroupsSave storage) {
        this.storage = storage;
    }

    public static class GroupItem {
        public UUID uuid;
        public String name;
        public Bitmap bitmap = null;
        public final long id = nextStableID++;

        public GroupItem(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        public GroupItem setUUID(UUID uuid) {
            this.uuid = uuid;
            return this;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof GroupItem)
                return uuid.equals(((GroupItem) other).uuid);
            else if (other instanceof UUID)
                return uuid.equals(other);
            return false;
        }

        public Bitmap getBitmap() {
            if (bitmap == null) {
                bitmap = Icons.loadIcon(NetpowerctrlApplication.instance, uuid,
                        Icons.IconType.SceneIcon, Icons.IconState.StateUnknown, R.drawable.netpowerctrl);
            }
            return bitmap;
        }
    }

    private final GroupItem groupItemIndexOfHelper = new GroupItem(null, null);

    public interface IGroupsUpdated {
        void groupsUpdated(boolean addedOrRemoved);
    }

    private final ArrayList<IGroupsUpdated> observers = new ArrayList<>();

    @SuppressWarnings("unused")
    public void registerObserver(IGroupsUpdated o) {
        if (!observers.contains(o)) {
            observers.add(o);
        }
    }

    @SuppressWarnings("unused")
    public void unregisterObserver(IGroupsUpdated o) {
        observers.remove(o);
    }

    @SuppressWarnings("unused")
    private void notifyObservers(boolean addedOrRemoved) {
        for (IGroupsUpdated o : observers)
            o.groupsUpdated(addedOrRemoved);
    }

    public List<GroupItem> groups = new ArrayList<>();

    //bitmap = Icons.loadIcon(NetpowerctrlApplication.instance,uuid, Icons.IconType.GroupIcon);
    public void setBitmap(GroupItem item, Bitmap bitmap) {
        if (item == null)
            return;
        item.bitmap = bitmap;
        Icons.saveIcon(NetpowerctrlApplication.instance, bitmap, item.uuid,
                Icons.IconType.GroupIcon, Icons.IconState.StateUnknown);
        notifyObservers(false);
    }

    public UUID add(String name) {
        UUID group_uuid = UUID.randomUUID();
        groups.add(new GroupItem(group_uuid, name));
        if (storage != null)
            storage.groupsSave(this);
        notifyObservers(true);
        return group_uuid;
    }

    public GroupItem get(UUID group_uuid) {
        int index = groups.indexOf(groupItemIndexOfHelper.setUUID(group_uuid));
        if (index == -1) {
            return null;
        }
        return groups.get(index);
    }

    /**
     * Edit an exiting groups name or add a new group with the given uuid and the given name.
     *
     * @param group_uuid Either existing uuid or new uuid
     * @param name       New name for the group
     */
    public void edit(UUID group_uuid, String name) {
        int index = groups.indexOf(groupItemIndexOfHelper.setUUID(group_uuid));
        if (index == -1)
            groups.add(new GroupItem(group_uuid, name));
        else
            groups.get(index).name = name;
        if (storage != null)
            storage.groupsSave(this);

        notifyObservers(false);
    }

    public boolean remove(UUID group_uuid) {
        int index = groups.indexOf(groupItemIndexOfHelper.setUUID(group_uuid));
        if (index == -1) {
            return false;
        }
        groups.remove(index);
        if (storage != null)
            storage.groupsSave(this);

        notifyObservers(true);
        return true;
    }

    public void reorderItems(int originalPosition, int newPosition, boolean saveReordering) {
        if (newPosition >= groups.size()) {
            return;
        }
        GroupItem temp = groups.get(originalPosition);
        groups.remove(originalPosition);
        groups.add(newPosition, temp);
        notifyObservers(true);
        if (saveReordering && storage != null) storage.groupsSave(this);

    }

    public int length() {
        return groups.size();
    }

    /**
     * Return the json representation of all groups
     *
     * @return JSON String
     */
    @Override
    public String toString() {
        return toJSON();
    }

    /**
     * Return the json representation of this scene
     *
     * @return JSON String
     */
    public String toJSON() {
        try {
            JSONHelper h = new JSONHelper();
            toJSON(h.createWriter());
            return h.getString();
        } catch (IOException ignored) {
            return null;
        }
    }

    private static void readGroupItem(JsonReader reader, GroupCollection groupCollection, boolean tryToMerge) throws IOException {
        String group_name = null;
        UUID group_uuid = null;

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            assert name != null;
            switch (name) {
                case "name":
                    group_name = reader.nextString();
                    break;
                case "uuid":
                    group_uuid = UUID.fromString(reader.nextString());
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();

        if (group_name == null || group_uuid == null)
            return;
        GroupItem item = new GroupItem(group_uuid, group_name);

        if (tryToMerge) {
            for (GroupItem di : groupCollection.groups)
                groupCollection.edit(di.uuid, di.name);
        } else
            groupCollection.groups.add(item);
    }

    /**
     * Read data from a json source and either replace existing data or merge data.
     *
     * @param reader     A json reader
     * @param tryToMerge If you merge the data instead of replacing the process is slower.
     * @throws IOException
     */
    public void fromJSON(JsonReader reader, boolean tryToMerge) throws IOException {
        if (reader == null)
            return;

        if (!tryToMerge)
            groups.clear();

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            assert name != null;
            if (name.equals("groupItems")) {
                reader.beginArray();
                while (reader.hasNext()) {
                    readGroupItem(reader, this, tryToMerge);
                }
                reader.endArray();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }

    public void save() {
        notifyObservers(true);
        if (storage != null)
            storage.groupsSave(this);
    }

    void toJSON(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("groupItems").beginArray();
        for (GroupItem c : groups) {
            writer.beginObject();
            writer.name("uuid").value(c.uuid.toString());
            writer.name("name").value(c.name);
            writer.endObject();
        }
        writer.endArray();
        writer.endObject();
    }

    public String[] getGroupsArray() {
        String[] a = new String[groups.size()];
        for (int i = 0; i < a.length; ++i)
            a[i] = groups.get(i).name;
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
        GroupItem g = groups.get(index);
        return groupUUids.contains(g.uuid);
    }

    public interface IGroupsSave {
        void groupsSave(GroupCollection groups);
    }
}
