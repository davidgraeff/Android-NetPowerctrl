package oly.netpowerctrl.utils;

/**
 * For all collections that support sorting
 */
public interface SortCriteriaInterface {
    public String[] getContentList();

    public String[] getSortCriteria();

    public void applySortCriteria(boolean[] criteria);

    public boolean allowCustomSort();

    public void setSortOrder(int[] sortOrder);
}
