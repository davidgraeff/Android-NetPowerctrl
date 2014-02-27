package oly.netpowerctrl.datastructure;

import android.graphics.Bitmap;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.Icons;
import oly.netpowerctrl.utils.JSONHelper;

public class Groups {
    public static long nextStableID = 0;

    public static class GroupItem {
        public UUID uuid;
        public String name;
        public Bitmap bitmap = null;
        public long id = nextStableID++;

        public GroupItem(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        public GroupItem setUUIDandReturn(UUID uuid) {
            this.uuid = uuid;
            return this;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof GroupItem)
                return uuid.equals(((GroupItem) other).uuid);
            else if (other instanceof UUID)
                return uuid.equals(((UUID) other));
            return false;
        }

        public Bitmap getBitmap() {
            if (bitmap == null) {
                bitmap = Icons.loadIcon(NetpowerctrlApplication.instance, uuid,
                        Icons.IconType.SceneIcon, R.drawable.netpowerctrl);
            }
            return bitmap;
        }
    }

    GroupItem groupItemIndexOfHelper = new GroupItem(null, null);

    public interface IGroupsUpdated {
        void groupsUpdated(boolean addedOrRemoved);
    }

    private ArrayList<IGroupsUpdated> observers = new ArrayList<IGroupsUpdated>();

    @SuppressWarnings("unused")
    public boolean registerObserver(IGroupsUpdated o) {
        if (!observers.contains(o)) {
            observers.add(o);
            return true;
        }
        return false;
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

    public List<GroupItem> groupItems = new ArrayList<GroupItem>();

    public Groups() {
    }

    //bitmap = Icons.loadIcon(NetpowerctrlApplication.instance,uuid, Icons.IconType.GroupIcon);
    public void setBitmap(GroupItem item, Bitmap bitmap) {
        if (item == null)
            return;
        item.bitmap = bitmap;
        Icons.saveIcon(NetpowerctrlApplication.instance, item.uuid, bitmap, Icons.IconType.GroupIcon);
        notifyObservers(false);
    }

    public UUID add(String name) {
        UUID group_uuid = UUID.randomUUID();
        groupItems.add(new GroupItem(group_uuid, name));
        SharedPrefs.saveGroups(this);
        notifyObservers(true);
        return group_uuid;
    }

    public GroupItem get(UUID group_uuid) {
        int index = groupItems.indexOf(groupItemIndexOfHelper.setUUIDandReturn(group_uuid));
        if (index == -1) {
            return null;
        }
        return groupItems.get(index);
    }

    /**
     * Edit an exiting groups name or add a new group with the given uuid and the given name.
     *
     * @param group_uuid Either existing uuid or new uuid
     * @param name       New name for the group
     */
    public void edit(UUID group_uuid, String name) {
        int index = groupItems.indexOf(groupItemIndexOfHelper.setUUIDandReturn(group_uuid));
        if (index == -1)
            groupItems.add(new GroupItem(group_uuid, name));
        else
            groupItems.get(index).name = name;
        SharedPrefs.saveGroups(this);
        notifyObservers(false);
    }

    public boolean remove(UUID group_uuid) {
        int index = groupItems.indexOf(groupItemIndexOfHelper.setUUIDandReturn(group_uuid));
        if (index == -1) {
            return false;
        }
        groupItems.remove(index);
        SharedPrefs.saveGroups(this);
        notifyObservers(true);
        return true;
    }

    public void reorderItems(int originalPosition, int newPosition, boolean saveReordering) {
        if (newPosition >= groupItems.size()) {
            return;
        }
        GroupItem temp = groupItems.get(originalPosition);
        groupItems.remove(originalPosition);
        groupItems.add(newPosition, temp);
        notifyObservers(true);
        if (saveReordering)
            SharedPrefs.saveGroups(this);
    }

    public int length() {
        return groupItems.size();
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

    private static void readGroupItem(JsonReader reader, Groups scene) throws IOException {
        String group_name = null;
        UUID group_uuid = null;

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("name")) {
                group_name = reader.nextString();
            } else if (name.equals("uuid")) {
                group_uuid = UUID.fromString(reader.nextString());
            } else
                reader.skipValue();
        }
        reader.endObject();

        if (group_name == null || group_uuid == null)
            return;
        GroupItem item = new GroupItem(group_uuid, group_name);
        scene.groupItems.add(item);
    }

    public static Groups fromJSON(JsonReader reader) throws IOException {
        Groups scene = new Groups();

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("groupItems")) {
                reader.beginArray();
                while (reader.hasNext()) {
                    readGroupItem(reader, scene);
                }
                reader.endArray();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return scene;
    }

    public void toJSON(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("groupItems").beginArray();
        for (GroupItem c : groupItems) {
            writer.beginObject();
            writer.name("uuid").value(c.uuid.toString());
            writer.name("name").value(c.name);
            writer.endObject();
        }
        writer.endArray();
        writer.endObject();
    }

    public String[] getGroupsArray() {
        String[] a = new String[groupItems.size()];
        for (int i = 0; i < a.length; ++i)
            a[i] = groupItems.get(i).name;
        return a;
    }
}
