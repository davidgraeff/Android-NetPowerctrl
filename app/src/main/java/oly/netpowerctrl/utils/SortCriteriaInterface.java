package oly.netpowerctrl.utils;

/**
 * Created by david on 10.05.14.
 */
public interface SortCriteriaInterface {
    public String[] getContentList();

    public String[] getSortCriteria();

    public void applySortCriteria(boolean[] criteria);

    public boolean allowCustomSort();

    public void setSortOrder(int[] sortOrder);
}
