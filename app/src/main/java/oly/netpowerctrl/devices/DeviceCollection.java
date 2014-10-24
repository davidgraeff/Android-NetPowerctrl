package oly.netpowerctrl.devices;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.Toast;

import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.CollectionWithStorableItems;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.data.onStorageUpdate;
import oly.netpowerctrl.device_ports.DevicePort;

/**
 * Contains DeviceInfos. Used for NFC and backup transfers
 */
public class DeviceCollection extends CollectionWithStorableItems<DeviceCollection, Device> {
    private static final String TAG = "DeviceCollection";

    public static DeviceCollection fromDevices(List<Device> devices, onStorageUpdate storage) {
        DeviceCollection dc = new DeviceCollection();
        dc.storage = storage;
        dc.items = devices;
        return dc;
    }

    /**
     * Add a new device to this collection. If there is a device with the same unique id
     * it will be overwritten.
     *
     * @param device The new device to add (or replace an existing one).
     * @return Return true if an existing device has been replaced instead of adding a new entry.
     */
    public boolean add(Device device) {
        // Already in configured devices?
        for (int i = items.size() - 1; i >= 0; --i) {
            if (device.equalsByUniqueID(items.get(i))) {
                items.set(i, device);
                if (storage != null)
                    storage.save(this, device);
                notifyObservers(device, ObserverUpdateActions.UpdateAction, i);
                return true;
            }
        }

        items.add(device);
        notifyObservers(device, ObserverUpdateActions.AddAction, items.size() - 1);
        if (storage != null)
            storage.save(this, device);
        return false;
    }

    public void removeAll() {
        int all = items.size();
        items.clear();
        notifyObservers(null, ObserverUpdateActions.RemoveAllAction, all - 1);
        if (storage != null)
            storage.clear(this);
    }

    public void save(Device device) {
        if (storage != null)
            storage.save(this, device);
    }

    public void remove(Device device) {
        int position = items.indexOf(device);
        if (position == -1)
            return;
        device.configured = false;
        items.remove(position);
        notifyObservers(device, ObserverUpdateActions.RemoveAction, position);
        if (storage != null)
            storage.remove(this, device);
    }

    public void updateNotReachable(Context context, Device device) {
        notifyObservers(device, ObserverUpdateActions.UpdateAction, items.indexOf(device));

        if (SharedPrefs.getInstance().notifyDeviceNotReachable()) {
            long current_time = System.currentTimeMillis();
            Toast.makeText(context,
                    context.getString(R.string.error_setting_outlet, device.DeviceName,
                            (int) ((current_time - device.getUpdatedTime()) / 1000)),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    /**
     * Do not call this directly! This is called by {@link oly.netpowerctrl.data.AppData}
     *
     * @param newValues_device Incoming device with new updated values. You may use a reference to a configured device
     *                         here
     * @return If a matching device has been found and updated
     * return that {@link oly.netpowerctrl.devices.Device}, null otherwise.
     */
    public Device update(Device newValues_device) {
        // If a device has now unique id, we do not have to care
        if (newValues_device.UniqueDeviceID == null)
            return null;

        int position = -1;
        for (Device existing_device : items) {
            ++position;

            if (!newValues_device.equalsByUniqueID(existing_device))
                continue;

            if (existing_device.updateConnection(newValues_device.DeviceConnections)) {
                if (storage != null)
                    storage.save(this, existing_device);
            }

            if (existing_device.copyValuesFromUpdated(newValues_device)) {
                //Log.w(TAG, "-- update: " + existing_device.DeviceName + " " + String.valueOf(System.identityHashCode(existing_device)));
                notifyObservers(existing_device, ObserverUpdateActions.UpdateAction, position);
            }

            return existing_device;
        }
        return null;
    }

    /**
     * Call this if you have made your changes to the given device and want to propagate those now.
     *
     * @param existing_device
     */
    public void updateExisting(Device existing_device) {
        if (AppData.observersOnDataLoaded.dataLoaded && existing_device.copyValuesFromUpdated(existing_device)) {
            //Log.w(TAG, "-- updateExisting: " + existing_device.DeviceName + " " + String.valueOf(System.identityHashCode(existing_device)));
            notifyObservers(existing_device, ObserverUpdateActions.UpdateAction, items.indexOf(existing_device));
        }
    }

    public boolean hasDevices() {
        return items.size() > 0;
    }

    public void setDevicePortBitmap(Context context, DevicePort port, Bitmap bitmap, LoadStoreIconData.IconState state) {
        if (port == null)
            return;

        LoadStoreIconData.saveIcon(context, LoadStoreIconData.resizeBitmap(context, bitmap, 128, 128), port.getUid(),
                LoadStoreIconData.IconType.DevicePortIcon, state);
        notifyObservers(port.device, ObserverUpdateActions.UpdateAction, items.indexOf(port.device));
    }

    @Override
    public String type() {
        return "devices";
    }

    public void groupsUpdated(Device device) {
        notifyObservers(device, ObserverUpdateActions.ClearAndNewAction, -1);
    }

    public void setHasChangedAll() {
        for (Device existing_device : items)
            existing_device.setHasChanged();
    }
}
