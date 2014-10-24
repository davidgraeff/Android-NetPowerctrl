package oly.netpowerctrl.devices;

import oly.netpowerctrl.data.CollectionWithStorableItems;
import oly.netpowerctrl.data.ObserverUpdateActions;

/**
 * Contains DeviceInfos. Used for NFC and backup transfers
 */
public class UnconfiguredDeviceCollection extends CollectionWithStorableItems<UnconfiguredDeviceCollection, Device> {
    private static final String TAG = "UnconfiguredDeviceCollection";


    /**
     * Add a new device to this collection. If there is a device with the same unique id
     * it will be overwritten.
     *
     * @param device The new device to add (or replace an existing one).
     * @return Return true if an existing device has been replaced instead of adding a new entry.
     */
    public boolean add(Device device) {
        // Already in this collection of devices?
        for (int i = items.size() - 1; i >= 0; --i) {
            if (device.equalsByUniqueID(items.get(i))) {
                items.set(i, device);
                notifyObservers(device, ObserverUpdateActions.UpdateAction, i);
                return true;
            }
        }

        items.add(device);
        notifyObservers(device, ObserverUpdateActions.AddAction, items.size() - 1);
        return false;
    }

    public void removeAll() {
        int all = items.size();
        items.clear();
        notifyObservers(null, ObserverUpdateActions.RemoveAllAction, all - 1);
    }

    public void remove(Device device) {
        int position = items.indexOf(device);
        if (position == -1)
            return;
        items.remove(position);
        notifyObservers(device, ObserverUpdateActions.RemoveAction, position);
    }

    @Override
    public String type() {
        return "unconfigured_devices";
    }
}
