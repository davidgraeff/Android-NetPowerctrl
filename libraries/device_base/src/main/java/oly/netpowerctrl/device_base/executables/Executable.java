package oly.netpowerctrl.device_base.executables;

import android.content.Context;

import java.util.List;
import java.util.UUID;

/**
 * Created by david on 22.10.14.
 */
public interface Executable {
    List<UUID> getGroups();

    String getUid();

    boolean isEnabled();

    ExecutableType getType();

    String getTitle(Context context);

    String getDescription(Context context);

    boolean isReachable();

    int getCurrentValue();

    int getMaximumValue();
}
