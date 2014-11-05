package oly.netpowerctrl.ui.navigation;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.TypedValue;
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
    private final List<DrawerItem> mItems = new ArrayList<>();
    private final float indention_shift;
    private final float listItemMinHeight;
    private final float listItemMinHeightSmall;
    private int selectedItemPosition = -1;
    private int lastSelectedItemPosition = -1;

    public DrawerAdapter(Context context) {
        // Use 30dp per indention level
        Resources r = context.getResources();
        DisplayMetrics metrics = r.getDisplayMetrics();
        indention_shift = 15 * (metrics.densityDpi / 160f);

        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.listPreferredItemHeightSmall, value, true);
        listItemMinHeight = value.getDimension(metrics);

        listItemMinHeightSmall = listItemMinHeight - 10 * (metrics.densityDpi / 160f);

        mItems.add(new DrawerItem(DrawerItemType.TYPE_FRONT));
    }

    public int indexOf(String className) {
        for (int i = 0; i < mItems.size(); i++) {
            DrawerItem item = mItems.get(i);
            if (item.type == DrawerItemType.TYPE_NORMAL) {
                if (item.fragmentClassName != null && item.fragmentClassName.equals(className))
                    return i;
            }
        }
        return -1;
    }

    public DrawerItem get(UUID id) {
        for (DrawerItem item : mItems) {
            if (item.uuid.equals(id)) {
                return item;
            }
        }
        return null;
    }

    public void addSeparator() {
        mItems.add(new DrawerItem(DrawerItemType.TYPE_SEPARATOR));
    }

    public DrawerItem addItem(String title, String clazz, int indention, boolean isMainItem) {
        DrawerItem item = new DrawerItem(title, indention, isMainItem);
        item.fragmentClassName = clazz;
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
        return mItems.get(position).type.ordinal();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v;
        switch (viewType) {
            case 0:
                v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_drawer_bitmap, viewGroup, false);
                return new ViewHolder(v, false);
            case 1:
                v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_drawer_separator, viewGroup, false);
                return new ViewHolder(v, false);
            case 2:
                v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_drawer, viewGroup, false);
                return new ViewHolder(v, true);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        DrawerItem item = mItems.get(position);

        if (item.type == DrawerItemType.TYPE_NORMAL) {
            ImageView image = viewHolder.image;
            if (item.bitmap != null) {
                image.setImageBitmap(item.bitmap);
                image.setVisibility(View.VISIBLE);
            } else {
                image.setVisibility(View.GONE);
            }

            if (item.isMainItem) {
                viewHolder.layout_item.setMinimumHeight((int) listItemMinHeight);
            } else
                viewHolder.layout_item.setMinimumHeight((int) listItemMinHeightSmall);

            viewHolder.title.setText(item.mTitle);

            if (item.indention > 0) {
                if (viewHolder.image != null)
                    viewHolder.image.setTranslationX(item.indention * indention_shift);
                viewHolder.title.setTranslationX(item.indention * indention_shift);
            }

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

    private enum DrawerItemType {
        TYPE_FRONT, TYPE_SEPARATOR, TYPE_NORMAL
    }

    public static class DrawerItem {

        public final DrawerItemType type;
        public final boolean isMainItem;
        public Bitmap bitmap = null;
        public String mTitle;
        public String fragmentClassName;
        public int indention = 0;
        public Bundle mExtra = null;
        public UUID uuid = UUID.randomUUID();
        public View.OnClickListener clickHandler = null;

        // Add separator
        public DrawerItem(DrawerItemType type) {
            this.type = type;
            isMainItem = false;
        }

        public DrawerItem(String title, int indention, boolean isMainItem) {
            this.indention = indention;
            this.isMainItem = isMainItem;
            mTitle = title;
            type = DrawerItemType.TYPE_NORMAL;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView title;
        public ImageView image;
        public View layout_item;

        public ViewHolder(View itemView, boolean isNormal) {
            super(itemView);

            if (isNormal) {
                title = (TextView) itemView.findViewById(R.id.title);
                image = (ImageView) itemView.findViewById(R.id.drawer_icon_bitmap);
                layout_item = itemView.findViewById(R.id.drawer_list_item);
            }
        }
    }
}
