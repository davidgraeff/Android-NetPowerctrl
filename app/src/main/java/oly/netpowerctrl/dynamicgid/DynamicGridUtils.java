package oly.netpowerctrl.dynamicgid;

import android.view.View;

import java.util.List;

/**
 * Author: alex askerov
 * Date: 9/7/13
 * Time: 10:14 PM
 */
public class DynamicGridUtils {
    /**
     * Delete item in <code>list</code> from position <code>indexFrom</code> and insert it to <code>indexTo</code>
     *
     * @param list
     * @param indexFrom
     * @param indexTo
     */
    public static void reorder(List list, int indexFrom, int indexTo) {
        if (indexTo >= list.size())
            return;
        Object obj = list.remove(indexFrom);
        list.add(indexTo, obj);
    }

    public static float getViewX(View view) {
        return Math.abs((view.getRight() - view.getLeft()) / 2);
    }

    public static float getViewY(View view) {
        return Math.abs((view.getBottom() - view.getTop()) / 2);
    }
}
