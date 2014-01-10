package oly.netpowerctrl.listadapter;

import android.app.Fragment;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import oly.netpowerctrl.R;
import oly.netpowerctrl.main.NetpowerctrlActivity;
import oly.netpowerctrl.main.PluginFragment;

/**
 * Adapter with items and headers
 */
public class DrawerAdapter extends BaseAdapter {

    private List<Object> mItems = new ArrayList<Object>();
    private LayoutInflater inflater;
    private Map<String, Fragment> mCachedFragments = new TreeMap<String, Fragment>();
    private int plugins_position = 0;

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
            if (getItemViewType(i) == 1) {
                DrawerItem item = (DrawerItem) mItems.get(i);
                if (item.mClazz.equals(className)) return i;
            }

        }
        return -1;
    }

    public void usePositionForPlugins() {
        plugins_position = mItems.size();
    }

    public void addCacheFragment(String name) {
        mCachedFragments.put(name, Fragment.instantiate(NetpowerctrlActivity.instance, name));
    }

    public Fragment getCachedFragment(String name) {
        return mCachedFragments.get(name);
    }

    public void updatePluginItem(int pluginId, String name) {
        for (int i = 0; i < mItems.size(); i++) {
            if (getItemViewType(i) == 1) {
                DrawerItem item = (DrawerItem) mItems.get(i);
                if (item.mExtra == pluginId) {
                    item.mTitle = name;
                    notifyDataSetChanged();
                    return;
                }
            }

        }
    }

    public static class Header {

        String mTitle;

        public Header(String title) {
            mTitle = title;
        }
    }

    public static class DrawerItem {

        public String mTitle;
        public String mSummary;
        public String mClazz;
        public boolean mDialog;
        public int mExtra;

        public DrawerItem(String title, String summary, String clazz, boolean dialog, int extra) {
            mTitle = title;
            mSummary = summary;
            mClazz = clazz;
            mDialog = dialog;
            mExtra = extra;
        }
    }

    public DrawerAdapter(Context context) {
        inflater = LayoutInflater.from(context);
    }

    public void addHeader(String title) {
        mItems.add(new Header(title));
    }

    public void addPluginHeader(String title) {
        mItems.add(plugins_position, new Header(title));
        ++plugins_position;
        notifyDataSetChanged();
    }

    public void addItem(String title, String summary, String clazz, boolean dialog) {
        mItems.add(new DrawerItem(title, summary, clazz, dialog, -1));
    }

    public void addPluginItem(String title, String summary, int extra) {
        mItems.add(plugins_position, new DrawerItem(title, summary, PluginFragment.class.getName(), false, extra));
        ++plugins_position;
        notifyDataSetChanged();
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
        return getItem(position) instanceof Header ? 0 : 1;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public View getView(int position, View v, ViewGroup parent) {
        if (getItemViewType(position) == 0) {
            Header header = (Header) getItem(position);

            if (v == null) {
                v = inflater.inflate(R.layout.drawer_list_header, parent, false);
            }

            assert v != null;
            ((TextView) v.findViewById(R.id.headerTitle)).setText(header.mTitle);

            return v;

        } else {
            DrawerItem sample = (DrawerItem) getItem(position);

            if (v == null) {
                v = inflater.inflate(R.layout.drawer_list_item, parent, false);
            }

            assert v != null;
            ((TextView) v.findViewById(R.id.title)).setText(sample.mTitle);
            TextView summary = ((TextView) v.findViewById(R.id.summary));
            summary.setVisibility(sample.mSummary.isEmpty() ? View.GONE : View.VISIBLE);
            summary.setText(sample.mSummary);

            return v;
        }
    }
}
