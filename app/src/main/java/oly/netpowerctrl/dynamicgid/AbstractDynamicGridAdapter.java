package oly.netpowerctrl.dynamicgid;

import android.widget.BaseAdapter;

/**
 * Author: alex askerov
 * Date: 9/6/13
 * Time: 7:43 PM
 */
public abstract class AbstractDynamicGridAdapter extends BaseAdapter {
    public static final int INVALID_ID = -1;

    /**
     * Determines how to reorder items dragged from <code>originalPosition</code> to <code>newPosition</code>
     *
     * @param originalPosition
     * @param newPosition
     */
    public abstract void reorderItems(int originalPosition, int newPosition);

    /**
     * Called after edit finished.
     */
    public abstract void finishedReordering();

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public abstract long getItemId(int position);
}
