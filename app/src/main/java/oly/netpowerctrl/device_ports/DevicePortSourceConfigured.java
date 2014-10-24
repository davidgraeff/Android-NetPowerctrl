package oly.netpowerctrl.device_ports;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.data.onDataLoaded;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.groups.Group;
import oly.netpowerctrl.groups.GroupCollection;

/**
 * Created by david on 07.07.14.
 */
public class DevicePortSourceConfigured implements DevicePortSourceInterface, onCollectionUpdated<Object, Object>, onDataLoaded {
    final static String TAG = "DevicePortSourceConfigured";
    private WeakReference<DevicePortsBaseAdapter> adapterWeakReference;
    private List<DevicePort> mList = new ArrayList<>();
    private boolean automaticUpdatesEnabled = false;
    private boolean hideNotReachable = false;
    private onChange onChangeListener = null;

    public void setOnChangeListener(onChange onChangeListener) {
        this.onChangeListener = onChangeListener;
    }

    public void setHideNotReachable(boolean hideNotReachable) {
        this.hideNotReachable = hideNotReachable;
    }

    @Override
    public void updateNow() {
        // NEW
        mList.clear();
        for (Device device : AppData.getInstance().deviceCollection.getItems()) {
            if (hideNotReachable && device.getFirstReachableConnection() == null)
                continue;

            device.lockDevicePorts();
            Iterator<DevicePort> iterator = device.getDevicePortIterator();
            while (iterator.hasNext()) {
                DevicePort devicePort = iterator.next();
                if (devicePort.Disabled)
                    continue;
                mList.add(devicePort);
            }
            device.releaseDevicePorts();
        }

        if (adapterWeakReference == null)
            return;

        // OLD
        DevicePortsBaseAdapter adapter = adapterWeakReference.get();
        if (adapter == null) {
            setAutomaticUpdate(false);
            return;
        }

        adapter.markAllRemoved();

        for (Device device : AppData.getInstance().deviceCollection.getItems()) {
            if (hideNotReachable && device.getFirstReachableConnection() == null)
                continue;
            device.lockDevicePorts();

            Iterator<DevicePort> iterator = device.getDevicePortIterator();
            while (iterator.hasNext()) {
                DevicePort devicePort = iterator.next();
                if (devicePort.Disabled)
                    continue;
                adapter.addItem(devicePort, devicePort.current_value);
            }
            device.releaseDevicePorts();
        }

        adapter.removeAllMarked();

        adapter.notifyDataSetChanged();
        if (onChangeListener != null)
            onChangeListener.sourceChanged();
    }

    @Override
    public void setAutomaticUpdate(boolean enabled) {
        automaticUpdatesEnabled = enabled;
        if (!enabled) {
            AppData.getInstance().deviceCollection.unregisterObserver(this);
            AppData.getInstance().groupCollection.unregisterObserver(this);
        } else {
            // If no data has been loaded so far, wait for load action to be completed before
            // registering to deviceCollection changes.
            if (!AppData.observersOnDataLoaded.dataLoaded)
                AppData.observersOnDataLoaded.register(this);
            else {
                AppData.getInstance().deviceCollection.registerObserver(this);
                AppData.getInstance().groupCollection.registerObserver(this);
            }
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
    public boolean onDataLoaded() {
        setAutomaticUpdate(automaticUpdatesEnabled);
        // Remove listener now
        return false;
    }

    @Override
    public boolean updated(Object collection, Object item, ObserverUpdateActions action, int position) {
        if (adapterWeakReference == null || item == null)
            return true;

        DevicePortsBaseAdapter adapter = adapterWeakReference.get();
        if (adapter == null) {
            return true;
        }

        if (collection instanceof GroupCollection) {
            if (action == ObserverUpdateActions.UpdateAction) { // if a group is renamed just update existing items
                Group group = ((Group) item);
                adapter.updateGroupName(group.uuid, group.name);
                if (onChangeListener != null)
                    onChangeListener.sourceChanged();
            } else
                updateNow(); // make complete update if a group is removed
            return true;
        }

        Device device = (Device) item;

        if (action == ObserverUpdateActions.RemoveAction || (hideNotReachable && device.getFirstReachableConnection() == null)) {

            device.lockDevicePorts();
            Iterator<DevicePort> it = device.getDevicePortIterator();
            while (it.hasNext()) {
                adapter.removeAt(findPositionByUUid(adapter, it.next().getUid()));
            }
            device.releaseDevicePorts();

        } else if (action == ObserverUpdateActions.AddAction || action == ObserverUpdateActions.UpdateAction) {

            Iterator<DevicePort> iterator = device.getDevicePortIterator();
            while (iterator.hasNext()) {
                DevicePort devicePort = iterator.next();
                if (devicePort.Disabled)
                    continue;
                adapter.addItem(devicePort, devicePort.current_value);
            }
            device.releaseDevicePorts();

        } else if (action == ObserverUpdateActions.ClearAndNewAction || action == ObserverUpdateActions.RemoveAllAction) {
            updateNow();
            return true;
        }

        if (onChangeListener != null)
            onChangeListener.sourceChanged();

        return true;
    }

    private int findPositionByUUid(DevicePortsBaseAdapter adapter, String uuid) {
        if (uuid == null)
            return -1;

        int i = -1;
        for (ExecutableAdapterItem info : adapter.mItems) {
            ++i;
            String uid = info.getExecutableUid();
            if (uid == null) // skip header items
                continue;
            if (uid.equals(uuid))
                return i;
        }

        return -1;
    }

    public List<DevicePort> getDevicePortList() {
        return mList;
    }

    public int indexOf(DevicePort port) {
        for (int i = 0; i < mList.size(); ++i)
            if (mList.get(i) == port)
                return i;
        return -1;
    }

    public interface onChange {
        void sourceChanged();
    }

}
