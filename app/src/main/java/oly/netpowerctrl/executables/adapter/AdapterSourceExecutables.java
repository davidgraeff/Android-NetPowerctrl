package oly.netpowerctrl.executables.adapter;

import android.support.annotation.NonNull;

import java.util.Set;
import java.util.TreeSet;

import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.executables.ExecutableCollection;
import oly.netpowerctrl.utils.ObserverUpdateActions;
import oly.netpowerctrl.utils.onCollectionUpdated;

;

/**
 * Created by david on 07.07.14.
 */
public class AdapterSourceExecutables extends AdapterSourceInput implements onCollectionUpdated<ExecutableCollection, Executable> {
    private ExecutableCollection executableCollection = null;

    @Override
    public void doUpdateNow() {
        for (Executable executable : executableCollection.getItems().values()) {
            if (executable.isHidden()) continue;
            adapterSource.addItem(executable, executable.current_value);
        }
    }

    @Override
    void onStart(DataService dataService) {
        this.executableCollection = dataService.executables;
        executableCollection.registerObserver(this);
        doUpdateNow();
    }

    @Override
    void onFinish() {
        if (executableCollection != null) executableCollection.unregisterObserver(this);
        executableCollection = null;
    }


    @Override
    public boolean updated(@NonNull ExecutableCollection collection, Executable executable, @NonNull ObserverUpdateActions action) {
        if (executable == null || adapterSource.ignoreUpdatesExecutable == executable)
            return true;

        switch (action) {
            case RemoveAction:
                //Log.w("REMOVE source ports", device.getDeviceName());
                adapterSource.removeAt(adapterSource.findPositionByUUid(executable.getUid()));
                break;
            case UpdateReachableAction:
                int pos = adapterSource.findPositionByUUid(executable.getUid());
                if (pos != -1) adapterSource.getAdapter().notifyItemChanged(pos);
                break;
            case UpdateAction:
                //Log.w("UPDATE source ports", device.getDeviceName());
                pos = adapterSource.findPositionByUUid(executable.getUid());
                if (pos != -1) adapterSource.getItem(pos).markRemoved();
                if (executable.isHidden()) break;
                adapterSource.addItem(executable, executable.current_value);
                adapterSource.removeAllMarked();
                break;
            case AddAction:
                if (executable.isHidden()) break;
                adapterSource.addItem(executable, executable.current_value);
                break;
            case ClearAndNewAction:
            case RemoveAllAction:
                //Log.w("CLEAR source ports", device.getDeviceName());
                adapterSource.updateNow();
                break;
        }

        adapterSource.sourceChanged();

        return true;
    }

    public Set<String> visibleExecutables() {
        Set<String> set = new TreeSet<>();
        for (Executable executable : executableCollection.getItems().values()) {
            if (executable.isHidden()) continue;
            set.add(executable.getUid());
        }
        return set;
    }
}
