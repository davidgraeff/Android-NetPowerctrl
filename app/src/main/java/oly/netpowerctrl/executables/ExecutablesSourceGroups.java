package oly.netpowerctrl.executables;

import java.util.UUID;

import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.data.onDataLoaded;
import oly.netpowerctrl.groups.Group;

/**
 * Created by david on 07.07.14.
 */
public class ExecutablesSourceGroups extends ExecutablesSourceBase implements onCollectionUpdated<Object, Object>, onDataLoaded {
    public ExecutablesSourceGroups(ExecutablesSourceChain executablesSourceChain) {
        super(executablesSourceChain);
    }

    @Override
    public void fullUpdate(ExecutablesBaseAdapter adapter) {
        // Nothing to do here. Groups are automatically set up by the
        // oly.netpowerctrl.executables.ExecutablesSourceDevicePorts class.
    }

    @Override
    public int doCountIfGroup(UUID uuid) {
        return 0;
    }

    @Override
    protected void automaticUpdatesDisable() {
        AppData.getInstance().groupCollection.unregisterObserver(this);
    }

    @Override
    protected void automaticUpdatesEnable() {
        // If no data has been loaded so far, wait for load action to be completed before
        // registering to deviceCollection changes.
        if (!AppData.isDataLoaded())
            AppData.observersOnDataLoaded.register(this);
        else {
            AppData.getInstance().groupCollection.registerObserver(this);
        }
    }

    @Override
    public boolean onDataLoaded() {
        if (automaticUpdatesEnabled)
            automaticUpdatesEnable();
        return false;
    }

    @Override
    public boolean updated(Object collection, Object item, ObserverUpdateActions action, int position) {
        if (adapterWeakReference == null || item == null)
            return true;

        ExecutablesBaseAdapter adapter = adapterWeakReference.get();
        if (adapter == null) {
            return true;
        }

        if (action == ObserverUpdateActions.UpdateAction) { // if a group is renamed just update existing items
            Group group = ((Group) item);
            adapter.updateGroupName(group.uuid, group.name);
            if (onChangeListener != null)
                onChangeListener.sourceChanged();
        } else // make complete update if a group is removed or added
            updateNow();
        return true;
    }
}
