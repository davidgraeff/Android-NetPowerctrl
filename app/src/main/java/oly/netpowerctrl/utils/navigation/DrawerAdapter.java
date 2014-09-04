package oly.netpowerctrl.utils.navigation;

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
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.scenes.Scene;
import oly.netpowerctrl.scenes.SceneCollection;

/**
 * Adapter with items and headers
 */
public class DrawerAdapter extends BaseAdapter implements onCollectionUpdated<SceneCollection, Scene> {

    private final List<DrawerItem> mItems = new ArrayList<>();
    private final LayoutInflater inflater;
    private UUID scenes_position = null;
    private int scenes_size = 0;

    public DrawerAdapter(Context context) {
        inflater = LayoutInflater.from(context);
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

    public void usePositionForScenes() {
        scenes_position = mItems.get(mItems.size() - 1).uuid;
        AppData.getInstance().sceneCollection.registerObserver(this);
        updated(AppData.getInstance().sceneCollection, null, null);
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

    public DrawerItem addItem(String title, String summary, String clazz, boolean dialog) {
        DrawerItem item = new DrawerItem(title, summary);
        item.fragmentClassName = clazz;
        item.mDialog = dialog;
        mItems.add(item);
        return item;
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

    public DrawerItem get(int position) {
        return mItems.get(position);
    }

    @Override
    public boolean updated(SceneCollection sceneCollection, Scene item, ObserverUpdateActions action) {
        if (scenes_position == null)
            return false;
        int startPosition = indexOf(scenes_position);
        if (startPosition == -1)
            return true;
        ++startPosition; // Add 1, otherwise we point to the item before the first scene item

        int maxLength = 0;
        for (Scene scene : sceneCollection.getItems()) {
            if (scene.isFavourite())
                ++maxLength;
        }

        if (action == ObserverUpdateActions.RemoveAction || action == ObserverUpdateActions.AddAction || scenes_size != maxLength) {
            // Remove all scenes first
            for (int i = 0; i < scenes_size; ++i)
                mItems.remove(startPosition);
            scenes_size = 0;

            // Read scenes, insert scene if its a favourite
            int counter = 0;
            for (final Scene scene : sceneCollection.getItems()) {
                if (!scene.isFavourite())
                    continue;
                DrawerItem drawerItem = new DrawerItem(scene.sceneName, "");
                drawerItem.uuid = scene.uuid;
                //item.bitmap = scene.getBitmap();
                drawerItem.intendLevel = 1;
                drawerItem.clickHandler = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AppData.getInstance().execute(scene, null);
                    }
                };
                mItems.add(startPosition + counter++, drawerItem);
            }
            scenes_size = maxLength;
            notifyDataSetChanged();
        } else { // just update names
            int counter = 0;
            for (final Scene scene : sceneCollection.getItems()) {
                if (!scene.isFavourite())
                    continue;
                DrawerItem drawerItem = mItems.get(counter++ + startPosition);
                drawerItem.uuid = scene.uuid;
                //item.bitmap = scene.getBitmap();
                drawerItem.mTitle = scene.sceneName;
                drawerItem.clickHandler = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AppData.getInstance().execute(scene, null);
                    }
                };
            }
            notifyDataSetChanged();
        }

        return true;
    }

    public static class DrawerItem {

        public final boolean isHeader;
        public final Bitmap bitmap = null;
        public String mTitle;
        public String mSummary;
        public String fragmentClassName;
        public boolean mDialog;
        public Bundle mExtra = null;
        public UUID uuid = UUID.randomUUID();
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
