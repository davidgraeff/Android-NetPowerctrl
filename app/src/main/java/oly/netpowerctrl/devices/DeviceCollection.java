package oly.netpowerctrl.devices;

import android.content.Context;
import android.graphics.Bitmap;

import java.util.List;

import oly.netpowerctrl.data.CollectionWithStorableItems;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.onStorageUpdate;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.listen_service.PluginInterface;

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

    static public boolean isNetworkDevice(Device device) {
        PluginInterface pi = (PluginInterface) device.getPluginInterface();
        return pi != null && pi.isNetworkPlugin();
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
                save(device);
                notifyObservers(device, ObserverUpdateActions.UpdateAction, i);
                return true;
            }
        }

        items.add(device);
        notifyObservers(device, ObserverUpdateActions.AddAction, items.size() - 1);
        save(device);
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
        device.setConfigured(true);
        if (storage != null)
            storage.save(this, device);
    }

    public void remove(Device device) {
        int position = items.indexOf(device);
        if (position == -1)
            return;
        device.setConfigured(false);
        items.remove(position);
        notifyObservers(device, ObserverUpdateActions.RemoveAction, position);
        if (storage != null)
            storage.remove(this, device);
    }

    public int getPosition(Device newValues_device) {
        int position = -1;
        for (Device existing_device : items) {
            ++position;

            if (newValues_device.equalsByUniqueID(existing_device))
                return position;
        }
        return -1;
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
