package oly.netpowerctrl.device_ports;

import java.lang.ref.WeakReference;

import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.data.onDataLoaded;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.devices.DeviceCollection;

/**
 * Created by david on 07.07.14.
 */
public class DevicePortSourceConfigured implements DevicePortSource, onCollectionUpdated<DeviceCollection, Device>, onDataLoaded {
    final static String TAG = "DevicePortSourceConfigured";
    private WeakReference<DevicePortsBaseAdapter> adapterWeakReference;
    private boolean automaticUpdatesEnabled = false;
    private boolean hideNotReachable = false;
    private onChange onChangeListener = null;

    public void setOnChangeListener(onChange onChangeListener) {
        this.onChangeListener = onChangeListener;
    }

    public boolean isHideNotReachable() {
        return hideNotReachable;
    }

    public void setHideNotReachable(boolean hideNotReachable) {
        this.hideNotReachable = hideNotReachable;
    }

    @Override
    public void updateNow() {
        if (adapterWeakReference == null)
            return;

        DevicePortsBaseAdapter adapter = adapterWeakReference.get();
        if (adapter == null) {
            setAutomaticUpdate(false);
            return;
        }

        adapter.markAllRemoved();

        for (Device device : AppData.getInstance().deviceCollection.getItems()) {
            if (hideNotReachable && device.getFirstReachableConnection() == null)
                continue;
            adapter.addAll(device, false);
        }

        adapter.removeAllMarked(true);

        adapter.notifyDataSetChanged();
        if (onChangeListener != null)
            onChangeListener.devicePortSourceChanged();
    }

    @Override
    public void setAutomaticUpdate(boolean enabled) {
        automaticUpdatesEnabled = enabled;
        if (!enabled) {
            AppData.getInstance().deviceCollection.unregisterObserver(this);
        } else {
            // If no data has been loaded so far, wait for load action to be completed before
            // registering to deviceCollection changes.
            if (!AppData.observersOnDataLoaded.dataLoaded)
                AppData.observersOnDataLoaded.register(this);
            else
                AppData.getInstance().deviceCollection.registerObserver(this);
        }
    }

    @Override
    public void setTargetAdapter(DevicePortsBaseAdapter adapter) {
        adapterWeakReference = new WeakReference<>(adapter);
    }

    @Override
    public boolean isAutomaticUpdateEnabled() {
        return automaticUpdatesEnabled;
    }

    public void onPause() {
        boolean temp = automaticUpdatesEnabled;
        setAutomaticUpdate(false);
        automaticUpdatesEnabled = temp;
    }

    public void onResume() {
        setAutomaticUpdate(automaticUpdatesEnabled);
    }

    @Override
    public boolean updated(DeviceCollection deviceCollection, Device device, ObserverUpdateActions action) {
        if (adapterWeakReference == null || device == null)
            return true;

        DevicePortsBaseAdapter adapter = adapterWeakReference.get();
        if (adapter == null) {
            return true;
        }

        if (action == ObserverUpdateActions.RemoveAction || (hideNotReachable && device.getFirstReachableConnection() == null))
            adapter.removeAll(device, true);
        else {
            adapter.addAll(device, true);
        }

        adapter.notifyDataSetChanged();
        if (onChangeListener != null)
            onChangeListener.devicePortSourceChanged();

        return true;
    }

    @Override
    public boolean onDataLoaded() {
        setAutomaticUpdate(automaticUpdatesEnabled);
        // Remove listener now
        return false;
    }

    public interface onChange {
        void devicePortSourceChanged();
    }

}
