package oly.netpowerctrl.listadapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;

/**
 * Adapter with items and headers
 */
public class DrawerAdapter extends BaseAdapter {

    private List<Object> mItems = new ArrayList<Object>();
    private LayoutInflater inflater;

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

        public DrawerItem(String title, String summary, String clazz, boolean dialog) {
            mTitle = title;
            mSummary = summary;
            mClazz = clazz;
            mDialog = dialog;
        }
    }

    public DrawerAdapter(Context context) {
        inflater = LayoutInflater.from(context);
    }

    public void addHeader(String title) {
        mItems.add(new Header(title));
    }

    public void addItem(String title, String summary, String clazz, boolean dialog) {
        mItems.add(new DrawerItem(title, summary, clazz, dialog));
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
    public View getView(int position, View convertView, ViewGroup parent) {
        if (getItemViewType(position) == 0) {
            View v = convertView;
            if (v == null) {
                v = inflater.inflate(R.layout.drawer_list_header, parent, false);
            }

            assert v != null;
            ((TextView) v.findViewById(R.id.headertitle)).setText(((Header) getItem(position)).mTitle);

            return v;

        } else {
            DrawerItem sample = (DrawerItem) getItem(position);

            View v = convertView;
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
