package oly.netpowerctrl.dynamicgid;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: alex askerov
 * Date: 9/7/13
 * Time: 10:49 PM
 */
public abstract class StableIDDynamicGridAdapter extends AbstractDynamicGridStableIDAdapter {
    private final Context mContext;

    private final ArrayList<Object> mItems = new ArrayList<Object>();

    protected StableIDDynamicGridAdapter(Context context) {
        this.mContext = context;
    }

    public StableIDDynamicGridAdapter(Context context, List<?> items) {
        mContext = context;
        init(items);
    }

    private void init(List<?> items) {
        addAllStableId(items);
        this.mItems.addAll(items);
    }


    public void set(List<?> items) {
        clear();
        init(items);
        notifyDataSetChanged();
    }

    void clear() {
        clearStableIdMap();
        mItems.clear();
        notifyDataSetChanged();
    }

    public void add(Object item) {
        addStableId(item);
        mItems.add(item);
        notifyDataSetChanged();
    }


    public void add(List<?> items) {
        addAllStableId(items);
        this.mItems.addAll(items);
        notifyDataSetChanged();
    }


    public void remove(Object item) {
        mItems.remove(item);
        removeStableID(item);
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
    public void reorderItems(int originalPosition, int newPosition) {
        if (newPosition < getCount()) {
            DynamicGridUtils.reorder(mItems, originalPosition, newPosition);
            notifyDataSetChanged();
        }
    }

    public List getItems() {
        return mItems;
    }

    protected Context getContext() {
        return mContext;
    }
}
