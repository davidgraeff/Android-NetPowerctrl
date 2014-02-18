package oly.netpowerctrl.dynamicgid;

import java.util.HashMap;
import java.util.List;

/**
 * Created by david on 14.02.14.
 */
public abstract class AbstractDynamicGridStableIDAdapter extends AbstractDynamicGridAdapter {
    private HashMap<Object, Integer> mIdMap = new HashMap<Object, Integer>();
    int nextID = 0; // always incrementing id counter

    /**
     * Adapter must have stable id
     *
     * @return
     */
    @Override
    public final boolean hasStableIds() {
        return true;
    }

    /**
     * creates stable id for object
     *
     * @param item
     */
    protected void addStableId(Object item) {
        mIdMap.put(item, nextID++);
    }

    /**
     * create stable ids for list
     *
     * @param items
     */
    protected void addAllStableId(List<?> items) {
        for (int i = 0; i < items.size(); i++) {
            mIdMap.put(items.get(i), nextID++);
        }
    }

    /**
     * get id for position
     *
     * @param position
     * @return
     */
    @Override
    public final long getItemId(int position) {
        if (position < 0 || position >= mIdMap.size()) {
            return INVALID_ID;
        }
        Object item = getItem(position);
        return mIdMap.get(item);
    }

    /**
     * clear stable id map
     * should called when clear adapter data;
     */
    protected void clearStableIdMap() {
        mIdMap.clear();
    }

    /**
     * remove stable id for <code>item</code>. Should called on remove data item from adapter
     *
     * @param item
     */
    protected void removeStableID(Object item) {
        mIdMap.remove(item);
    }

}
