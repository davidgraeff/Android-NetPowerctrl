package oly.netpowerctrl.navigation_drawer;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.datastructure.GroupCollection;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.datastructure.SceneCollection;
import oly.netpowerctrl.main.OutletsFragment;
import oly.netpowerctrl.preferences.SharedPrefs;

/**
 * Adapter with items and headers
 */
public class DrawerAdapter extends BaseAdapter implements GroupCollection.IGroupsUpdated, SceneCollection.IScenesUpdated {

    private final List<DrawerItem> mItems = new ArrayList<DrawerItem>();
    private final LayoutInflater inflater;
    private UUID groups_position = null;
    private int groups_size = 0;
    private UUID scenes_position = null;
    private int scenes_size = 0;

    public DrawerAdapter(Context context) {
        inflater = LayoutInflater.from(context);
    }

    @Override
    public void groupsUpdated(boolean addedOrRemoved) {
        if (groups_position == null)
            return;
        int startPosition = indexOf(groups_position);
        if (startPosition == -1)
            return;
        ++startPosition; // Add 1, otherwise we point to the item before the first group item

        GroupCollection g = NetpowerctrlApplication.getDataController().groupCollection;
        int maxLength = SharedPrefs.getMaxFavGroups();
        if (g.length() < maxLength) maxLength = g.length();

        if (addedOrRemoved || groups_size != maxLength) {
            // Remove all groups first
            for (int i = 0; i < groups_size; ++i)
                mItems.remove(startPosition);
            groups_size = 0;

            // Readd groups
            for (int i = 0; i < maxLength; ++i) {
                GroupCollection.GroupItem groupItem = g.groupItems.get(i);
                DrawerItem item = new DrawerItem(groupItem.name, "");
                item.uuid = groupItem.uuid;
                item.bitmap = groupItem.getBitmap();
                item.fragmentClassName = OutletsFragment.class.getName();
                item.mExtra = new Bundle();
                item.mExtra.putString("filter", groupItem.uuid.toString());
                item.intendLevel = 1;
                mItems.add(startPosition + i, item);
            }
            groups_size = maxLength;
            notifyDataSetChanged();
        } else { // just update names
            for (int i = 0; i < groups_size; ++i) {
                GroupCollection.GroupItem groupItem = g.groupItems.get(i);
                DrawerItem item = mItems.get(i + startPosition);
                item.bitmap = groupItem.getBitmap();
                item.mTitle = groupItem.name;
            }
            notifyDataSetChanged();
        }
    }


    @Override
    public void scenesUpdated(boolean addedOrRemoved) {
        if (scenes_position == null)
            return;
        int startPosition = indexOf(scenes_position);
        if (startPosition == -1)
            return;
        ++startPosition; // Add 1, otherwise we point to the item before the first scene item

        SceneCollection g = NetpowerctrlApplication.getDataController().sceneCollection;
        int maxLength = 0;
        for (Scene scene : g.scenes) {
            if (scene.isFavourite())
                ++maxLength;
        }

        if (addedOrRemoved || scenes_size != maxLength) {
            // Remove all scenes first
            for (int i = 0; i < scenes_size; ++i)
                mItems.remove(startPosition);
            scenes_size = 0;

            // Read scenes, insert scene if its a favourite
            int counter = 0;
            for (final Scene scene : g.scenes) {
                if (!scene.isFavourite())
                    continue;
                DrawerItem item = new DrawerItem(scene.sceneName, "");
                item.uuid = scene.uuid;
                //item.bitmap = scene.getBitmap();
                item.intendLevel = 1;
                item.clickHandler = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        NetpowerctrlApplication.getDataController().execute(scene, null);
                    }
                };
                mItems.add(startPosition + counter++, item);
            }
            scenes_size = maxLength;
            notifyDataSetChanged();
        } else { // just update names
            int counter = 0;
            for (final Scene scene : g.scenes) {
                if (!scene.isFavourite())
                    continue;
                DrawerItem item = mItems.get(counter++ + startPosition);
                item.uuid = scene.uuid;
                //item.bitmap = scene.getBitmap();
                item.mTitle = scene.sceneName;
                item.clickHandler = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        NetpowerctrlApplication.getDataController().execute(scene, null);
                    }
                };
            }
            notifyDataSetChanged();
        }
    }

    public void add(String[] mFragmentNames, String[] mFragmentDesc, String[] mFragmentClasses) {
        for (int i = 0; i < mFragmentNames.length; ++i) {
            if (mFragmentDesc[i].equals("-")) {
                addHeader(mFragmentNames[i]);
            } else {
                addItem(mFragmentNames[i], mFragmentDesc[i], mFragmentClasses[i], mFragmentClasses[i].contains("Dialog"));
            }
        }
    }

    public void setAccompanyFragment(String existingFragment, String accompanyFragment) {
        int i = indexOf(existingFragment);
        if (i == -1)
            return;
        DrawerItem item = mItems.get(i);
        item.mAccompanyClazz = accompanyFragment;
    }

    public int indexOf(String className) {
        for (int i = 0; i < mItems.size(); i++) {
            DrawerItem item = mItems.get(i);
            if (!item.isHeader) {
                if (item.fragmentClassName != null && item.fragmentClassName.equals(className))
                    return i;
            }
        }
        return -1;
    }

    private int indexOf(UUID uuid) {
        for (int i = 0; i < mItems.size(); i++) {
            DrawerItem item = mItems.get(i);
            if (item.uuid.equals(uuid)) return i;

        }
        return -1;
    }

    public void usePositionForGroups() {
        groups_position = mItems.get(mItems.size() - 1).uuid;
        NetpowerctrlApplication.getDataController().groupCollection.registerObserver(this);
        groupsUpdated(true);
    }

    public void usePositionForScenes() {
        scenes_position = mItems.get(mItems.size() - 1).uuid;
        NetpowerctrlApplication.getDataController().sceneCollection.registerObserver(this);
        scenesUpdated(true);
    }

    public void remove(UUID id) {
        for (int i = 0; i < mItems.size(); i++) {
            DrawerItem item = mItems.get(i);
            if (item.uuid.equals(id)) {
                mItems.remove(i);
                notifyDataSetChanged();
                return;
            }
        }
    }

    public DrawerItem get(UUID id) {
        for (DrawerItem item : mItems) {
            if (item.uuid.equals(id)) {
                return item;
            }
        }
        return null;
    }

    public void addHeader(String title) {
        mItems.add(new DrawerItem(title));
    }

    public void addItem(String title, String summary, String clazz, boolean dialog) {
        DrawerItem item = new DrawerItem(title, summary);
        item.fragmentClassName = clazz;
        item.mDialog = dialog;
        mItems.add(item);
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return mItems.get(position).isHeader ? 0 : 1;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public View getView(int position, View v, ViewGroup parent) {
        DrawerItem item = (DrawerItem) getItem(position);

        if (item.isHeader) {

            if (v == null) {
                v = inflater.inflate(R.layout.drawer_list_header, parent, false);
            }

            assert v != null;
            ((TextView) v.findViewById(R.id.headerTitle)).setText(item.mTitle);

            return v;

        } else {
            if (v == null) {
                v = inflater.inflate(R.layout.drawer_list_item, parent, false);
            }

            assert v != null;
            View layout_item = v.findViewById(R.id.drawer_list_item);
            layout_item.setPadding(v.getPaddingRight() + layout_item.getPaddingRight() * item.intendLevel, v.getPaddingTop(),
                    v.getPaddingRight(), v.getPaddingBottom());

            ImageView image = (ImageView) v.findViewById(R.id.drawer_icon_bitmap);
            if (item.bitmap != null) {
                image.setImageBitmap(item.bitmap);
                image.setVisibility(View.VISIBLE);
            } else
                image.setVisibility(View.GONE);

            TextView title = ((TextView) v.findViewById(R.id.text1));
            title.setText(item.mTitle);

            TextView summary = ((TextView) v.findViewById(R.id.summary));
            summary.setVisibility(item.mSummary.isEmpty() ? View.GONE : View.VISIBLE);
            summary.setText(item.mSummary);

            return v;
        }
    }

    public static class DrawerItem {

        public String mTitle;
        public String mSummary;
        public String fragmentClassName;
        public String mAccompanyClazz;
        public boolean mDialog;
        public Bundle mExtra = null;
        public UUID uuid = UUID.randomUUID();
        public Bitmap bitmap = null;
        public final boolean isHeader;
        public int intendLevel = 0;
        public View.OnClickListener clickHandler = null;

        public DrawerItem(String title) {
            mTitle = title;
            isHeader = true;
        }

        public DrawerItem(String title, String summary) {
            mTitle = title;
            mSummary = summary;
            isHeader = false;
        }
    }
}
