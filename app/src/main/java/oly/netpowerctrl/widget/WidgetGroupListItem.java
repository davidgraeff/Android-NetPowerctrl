package oly.netpowerctrl.widget;

import android.support.annotation.NonNull;

import java.lang.ref.WeakReference;

import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.executables.ExecutableCollection;
import oly.netpowerctrl.utils.ObserverUpdateActions;
import oly.netpowerctrl.utils.onCollectionUpdated;

/**
 * Represents an entry in the group list home screen widget.
 */
public class WidgetGroupListItem implements onCollectionUpdated<ExecutableCollection, Executable> {
    Executable executable;
    int viewID = WidgetGroupList.nextViewID++;
    int cached_last_state = -1000;
    private WidgetGroupList widgetGroupList;

    WidgetGroupListItem(WidgetGroupList widgetGroupList, Executable executable) {
        this.widgetGroupList = widgetGroupList;
        DataService.useService(new WeakReference<Object>(this));
        DataService dataService = widgetGroupList.widgetUpdateService.service;

        this.executable = executable;
        dataService.executables.registerObserver(this);
    }

    @Override
    public boolean updated(@NonNull ExecutableCollection collection, Executable item, @NonNull ObserverUpdateActions action) {
        //Log.w("widget", di != null ? di.DeviceName : "empty di");
        if (item == null) return true;

        if (executable.getCurrentValue() == cached_last_state || item != executable)
            return true;
        widgetGroupList.requestUpdateRemoteList();

        return true;
    }

    public void destroy() {
        if (DataService.isServiceReady()) {
            DataService dataService = widgetGroupList.widgetUpdateService.service;
            dataService.credentials.unregisterObserver(this);
        }

        DataService.stopUseService(this);
    }
}
