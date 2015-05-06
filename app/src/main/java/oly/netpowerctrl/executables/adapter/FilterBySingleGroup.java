package oly.netpowerctrl.executables.adapter;

import oly.netpowerctrl.executables.Executable;

/**
 * Created by david on 30.12.14.
 */
public class FilterBySingleGroup extends AdapterFilter {
    private String mFilterGroup;

    public FilterBySingleGroup(String group) {
        mFilterGroup = group;
    }

    public String getFilterGroup() {
        return mFilterGroup;
    }

    public void setFilterGroup(String groupFilter) {
        boolean changed = this.mFilterGroup == null ? groupFilter != null : !this.mFilterGroup.equals(groupFilter);
        this.mFilterGroup = groupFilter;
        if (changed)
            adapterSource.updateNow();
    }

    @Override
    public boolean filter(Executable executable) {
        return mFilterGroup != null && !executable.getGroupUIDs().contains(mFilterGroup);
    }
}
