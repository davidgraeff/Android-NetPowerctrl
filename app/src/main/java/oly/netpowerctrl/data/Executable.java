package oly.netpowerctrl.data;

import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.executables.ExecutableType;

/**
 * Created by david on 22.10.14.
 */
public interface Executable {
    List<UUID> getGroups();

    String getUid();

    boolean isEnabled();

    ExecutableType getType();

    String getTitle();

    String getDescription();

    boolean isReachable();

    ;

    int getCurrentValue();

    int getMaximumValue();
}
