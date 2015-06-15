package oly.netpowerctrl.ioconnection;

;
import oly.netpowerctrl.data.CollectionWithStorableItems;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.credentials.Credentials;

/**
 * Contains DeviceInfos. Used for NFC and backup transfers
 */
public class IOConnectionsNotConfiguredCollection extends CollectionWithStorableItems<IOConnectionsNotConfiguredCollection, IOConnection> {
    private static final String TAG = "UnconfiguredDeviceCollection";

    public IOConnectionsNotConfiguredCollection( PluginService pluginService) {
        super(PluginService);
    }


    /**
     * Add a new device to this collection. If there is a device with the same unique id
     * it will be overwritten.
     *
     * @param ioConnection The new device to add (or replace an existing one).
     * @return Return true if an existing device has been replaced instead of adding a new entry.
     */
    public boolean add(IOConnection ioConnection) {
        // Already in this collection of devices?
        for (int i = items.size() - 1; i >= 0; --i) {
            if (ioConnection.equalsByDestinationAddress(items.get(i), false)) {
                items.set(i, ioConnection);
                notifyObservers(ioConnection, ObserverUpdateActions.UpdateAction, i);
                return true;
            }
        }

        items.add(ioConnection);
        notifyObservers(ioConnection, ObserverUpdateActions.AddAction, items.size() - 1);
        return false;
    }

    public void removeAll() {
        int all = items.size();
        items.clear();
        notifyObservers(null, ObserverUpdateActions.RemoveAllAction, all - 1);
    }

    public void remove(int position) {
        IOConnection ioConnection = items.get(position);
        items.remove(position);
        notifyObservers(ioConnection, ObserverUpdateActions.RemoveAction, position);
    }

    public void remove(IOConnection ioConnection) {
        int position = items.indexOf(ioConnection);
        if (position == -1)
            return;
        items.remove(position);
        notifyObservers(ioConnection, ObserverUpdateActions.RemoveAction, position);
    }

    @Override
    public String type() {
        return "unconfigured_devices";
    }
}
