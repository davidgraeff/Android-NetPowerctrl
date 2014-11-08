package oly.netpowerctrl.executables;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by david on 04.11.14.
 */
public class ExecutablesSourceChain {
    List<ExecutablesSourceBase> list = new ArrayList<>();

    public void setAutomaticUpdate(boolean enabled) {
        for (ExecutablesSourceBase base : list)
            base.applyAutomaticUpdate(enabled);
    }

    public void fullUpdate(ExecutablesBaseAdapter adapter) {
        for (ExecutablesSourceBase base : list)
            base.fullUpdate(adapter);
    }

    public void add(ExecutablesSourceBase executablesSourceBase) {
        list.add(executablesSourceBase);
    }

    public int doCountIfGroup(UUID uuid) {
        int count = 0;
        for (ExecutablesSourceBase base : list)
            count += base.doCountIfGroup(uuid);
        return count;
    }
}
