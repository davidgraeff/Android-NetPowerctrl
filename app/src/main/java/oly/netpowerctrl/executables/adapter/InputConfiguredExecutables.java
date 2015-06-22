package oly.netpowerctrl.executables.adapter;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.executables.ExecutableCollection;
import oly.netpowerctrl.utils.ObserverUpdateActions;
import oly.netpowerctrl.utils.onCollectionUpdated;

/**
 * For AdapterSource which can be used for a recyclerview Adapter if wrapped in ExecutablesAdapter etc.
 * This input will provide all executable items and update them accordingly to the DataService.executable collection
 * changes.
 */
public class InputConfiguredExecutables extends AdapterInput implements onCollectionUpdated<ExecutableCollection, Executable> {
    private ExecutableCollection executableCollection = null;
    // To avoid allocating a list each time, an item is searched for, we allocate a result list here instead.
    private List<Integer> position_list = new ArrayList<>();

    @Override
    public void doUpdateNow() {
        for (Executable executable : executableCollection.getItems().values()) {
            if (executable.getCredentials() == null || executable.getCredentials().isConfigured())
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

        // Ignore not configured credentials
        if (executable.getCredentials() != null && !executable.getCredentials().isConfigured())
            return true;

        switch (action) {
            case RemoveAction:
                Log.w("ADAPTER REMOVE", executable.getTitle());
                position_list.clear();
                adapterSource.findPositionsByUUid(executable.getUid(), position_list);
                for (int pos : position_list)
                    adapterSource.removeAt(pos);
                break;

            case UpdateReachableAction:
                position_list.clear();
                adapterSource.findPositionsByUUid(executable.getUid(), position_list);
                if (position_list.isEmpty()) {
                    break;
//                    if (adapterSource.filtered(executable))
//                        break;
//                    else
//                        throw new RuntimeException("UpdateReachableAction, but never added to adapter!");
                }

                for (int pos : position_list) {
                    if (adapterSource.filtered(executable))
                        adapterSource.removeAt(pos);
                    else {
                        Log.w("ADAPTER UPDATE REACHABL", executable.getTitle());
                        adapterSource.getAdapter().notifyItemChanged(pos);
                    }
                }
                break;

            case UpdateAction:
            case AddAction:
                Log.w("ADAPTER UPDATE/Add", executable.getTitle());
                adapterSource.addItem(executable, executable.current_value);
                break;

            case ClearAndNewAction:
            case RemoveAllAction:
                //Log.w("CLEAR source ports", device.getDeviceName());
                adapterSource.updateNow();
                break;
        }
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
