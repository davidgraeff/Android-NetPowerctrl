package oly.netpowerctrl.utils;

import java.util.List;

/**
 * Generics-using sort algorithm (QSort) with comparison callback.
 * This is used for sorting SceneCollection and DevicePorts (in DevicePortsExecuteAdapter)
 * and mainly self implemented to have a callback for the actual comparison.
 * Because we are on java 1.6 at the moment, we can't use java.util.TimSort.
 */
public class Sorting {
    public static interface qSortComparable<T> {
        boolean isGreater(T first, T second);
    }

    /**
     * Basic q-sort implementation. Not stable, in place.
     *
     * @param x          The list of data with type T
     * @param left       Usually 0
     * @param right      Usually list.size()-1
     * @param comparable A comparison callback interface with one method: isGreater(T, T)->bool
     * @param <T>        The object type in the list
     */
    public static <T> void qSort(List<T> x, int left, int right, qSortComparable<T> comparable) {
        if (left < right) {
            int i = partition(x, left, right, comparable);
            qSort(x, left, i - 1, comparable);
            qSort(x, i + 1, right, comparable);
        }
    }

    private static <T> int partition(List<T> x, int left, int right, qSortComparable<T> comparable) {
        int i, j;
        T pivot, help;
        pivot = x.get(right);
        i = left;
        j = right - 1;
        while (i <= j) {
            if (comparable.isGreater(x.get(i), pivot)) {
                // swap x[i] und x[j]
                help = x.get(i);
                x.set(i, x.get(j));
                x.set(j, help);
                j--;
            } else i++;
        }
        // swap x[i] and x[right]
        help = x.get(i);
        x.set(i, x.get(right));
        x.set(right, help);

        return i;
    }

}
