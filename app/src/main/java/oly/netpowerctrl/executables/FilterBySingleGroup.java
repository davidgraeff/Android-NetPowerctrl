package oly.netpowerctrl.executables;

import java.util.UUID;

import oly.netpowerctrl.device_base.executables.Executable;

/**
 * Created by david on 30.12.14.
 */
public class FilterBySingleGroup extends AdapterSourceFilter {
    private UUID mFilterGroup;

    public FilterBySingleGroup(UUID group) {
        mFilterGroup = group;
    }

    public UUID getFilterGroup() {
        return mFilterGroup;
    }

    public void setFilterGroup(UUID groupFilter) {
        boolean changed = this.mFilterGroup == null ? groupFilter != null : !this.mFilterGroup.equals(groupFilter);
        this.mFilterGroup = groupFilter;
        if (changed)
            adapterSource.updateNow();
    }

    @Override
    public boolean filter(Executable executable) {
        return mFilterGroup != null && !executable.getGroups().contains(mFilterGroup);
    }
}
