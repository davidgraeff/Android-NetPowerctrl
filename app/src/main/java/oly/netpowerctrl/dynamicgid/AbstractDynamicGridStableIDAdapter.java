package oly.netpowerctrl.dynamicgid;

import java.util.HashMap;
import java.util.List;

/**
 * The gridView needs stable ids. This is a helper adapter that provides stable ids.
 */
public abstract class AbstractDynamicGridStableIDAdapter extends AbstractDynamicGridAdapter {
    private final HashMap<Object, Integer> mIdMap = new HashMap<Object, Integer>();
    private int nextID = 0; // always incrementing id counter

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
    void addStableId(Object item) {
        mIdMap.put(item, nextID++);
    }

    /**
     * create stable ids for list
     *
     * @param items
     */
    void addAllStableId(List<?> items) {
        for (Object item : items) {
            mIdMap.put(item, nextID++);
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
    void clearStableIdMap() {
        mIdMap.clear();
    }

    /**
     * remove stable id for <code>item</code>. Should called on remove data item from adapter
     *
     * @param item
     */
    void removeStableID(Object item) {
        mIdMap.remove(item);
    }

}
