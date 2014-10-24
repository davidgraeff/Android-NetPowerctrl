package oly.netpowerctrl.utils.navigation;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;

/**
 * Adapter with items and headers
 */
public class DrawerAdapter extends RecyclerView.Adapter<DrawerAdapter.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private final List<DrawerItem> mItems = new ArrayList<>();
    private int selectedItemPosition = -1;
    private int lastSelectedItemPosition = -1;

    public DrawerAdapter() {
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

    public DrawerItem getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mItems.get(position).isHeader ? TYPE_HEADER : TYPE_ITEM;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if (viewType == TYPE_HEADER) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_header_drawer, viewGroup, false);
            return new ViewHolder(v, true);
        } else {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_drawer, viewGroup, false);
            return new ViewHolder(v, false);
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        DrawerItem item = mItems.get(position);

        if (item.isHeader) {
            viewHolder.headerTitle.setText(item.mTitle);
        } else {
//            View layout_item = viewHolder.layout_item;
//            layout_item.setPadding(v.getPaddingRight() + layout_item.getPaddingRight() * item.intendLevel, v.getPaddingTop(),
//                    v.getPaddingRight(), v.getPaddingBottom());

            ImageView image = viewHolder.image;
            if (item.bitmap != null) {
                image.setImageBitmap(item.bitmap);
                image.setVisibility(View.VISIBLE);
            } else
                image.setVisibility(View.GONE);

            viewHolder.title.setText(item.mTitle);

            viewHolder.summary.setVisibility(item.mSummary.isEmpty() ? View.GONE : View.VISIBLE);
            viewHolder.summary.setText(item.mSummary);

            if (lastSelectedItemPosition == position) {
                viewHolder.layout_item.setActivated(false);
                lastSelectedItemPosition = -1;
            }

            if (selectedItemPosition == position) {
                viewHolder.layout_item.setActivated(true);
            }
        }
    }

    public DrawerItem get(int position) {
        return mItems.get(position);
    }

    public void setSelectedItem(int pos) {
        lastSelectedItemPosition = selectedItemPosition;
        if (lastSelectedItemPosition != -1)
            notifyItemChanged(lastSelectedItemPosition);

        selectedItemPosition = pos;
        notifyItemChanged(pos);
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView title;
        public TextView summary;
        public TextView headerTitle;
        public ImageView image;
        public View layout_item;
        public boolean isHeader;

        public ViewHolder(View itemView, boolean isHeader) {
            super(itemView);
            this.isHeader = isHeader;

            if (isHeader) {
                headerTitle = (TextView) itemView.findViewById(R.id.headerTitle);
            } else {
                title = (TextView) itemView.findViewById(R.id.title);
                summary = (TextView) itemView.findViewById(R.id.summary);
                image = (ImageView) itemView.findViewById(R.id.drawer_icon_bitmap);
                layout_item = itemView.findViewById(R.id.drawer_list_item);
            }
        }
    }
}
