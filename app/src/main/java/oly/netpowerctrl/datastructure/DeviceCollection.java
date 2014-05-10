package oly.netpowerctrl.datastructure;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;

import oly.netpowerctrl.network.DeviceUpdate;
import oly.netpowerctrl.utils.Icons;
import oly.netpowerctrl.utils.JSONHelper;

/**
 * Contains DeviceInfos. Used for NFC and backup transfers
 */
public class DeviceCollection {
    public List<DeviceInfo> devices;
    private final IDevicesSave storage;

    public DeviceCollection(IDevicesSave storage) {
        this.storage = storage;
    }

    public static DeviceCollection fromDevices(List<DeviceInfo> devices, IDevicesSave storage) {
        DeviceCollection dc = new DeviceCollection(storage);
        dc.devices = devices;
        return dc;
    }

    public static DeviceCollection fromJSON(JsonReader reader, IDevicesSave storage) throws IOException, IllegalStateException {
        DeviceCollection dc = new DeviceCollection(storage);
        dc.devices = new ArrayList<DeviceInfo>();
        if (reader == null)
            return dc;

        reader.beginArray();
        while (reader.hasNext()) {
            try {
                dc.devices.add(DeviceInfo.fromJSON(reader));
            } catch (ClassNotFoundException e) {
                // If we read a device description, where we do not support that device type,
                // we just ignore that device and go on. Nevertheless print a backtrace.
                e.printStackTrace();
            }
        }
        reader.endArray();
        return dc;
    }

    /**
     * Return the json representation of all groups
     *
     * @return JSON String
     */
    @Override
    public String toString() {
        return toJSON();
    }

    /**
     * Return the json representation of this scene
     *
     * @return JSON String
     */
    public String toJSON() {
        try {
            JSONHelper h = new JSONHelper();
            toJSON(h.createWriter());
            return h.getString();
        } catch (IOException ignored) {
            return null;
        }
    }

    public void toJSON(JsonWriter writer) throws IOException {
        writer.beginArray();
        for (DeviceInfo di : devices) {
            di.toJSON(writer);
        }
        writer.endArray();
    }


    private final WeakHashMap<DeviceUpdate, Boolean> observersConfiguredDevice = new WeakHashMap<DeviceUpdate, Boolean>();

    @SuppressWarnings("unused")
    public void registerConfiguredDeviceChangeObserver(DeviceUpdate o) {
        observersConfiguredDevice.put(o, true);
    }

    @SuppressWarnings("unused")
    public void unregisterConfiguredDeviceChangeObserver(DeviceUpdate o) {
        observersConfiguredDevice.remove(o);
    }

    public void notifyDeviceChangeObservers(DeviceInfo di, boolean willBeRemoved) {
        for (DeviceUpdate o : observersConfiguredDevice.keySet())
            o.onDeviceUpdated(di, willBeRemoved);
    }

    /**
     * Add a new device to this collection. If there is a device with the same unique id
     * it will be overwritten.
     *
     * @param device The new device to add (or replace an existing one).
     * @return Return true if an existing device has been replaced instead of adding a new entry.
     */
    public boolean add(DeviceInfo device) {
        // Determine next free devicePort position.
        // This position is only for the listView to let all devicePorts of this new device
        // be at the end of the list.
        int highestPosition = 0;
        int nextPos = 0;
        for (DeviceInfo local_device : devices) {
            nextPos += local_device.count();
            Iterator<DevicePort> it = device.getDevicePortIterator();
            while (it.hasNext()) {
                DevicePort port = it.next();
                highestPosition = port.positionRequest > highestPosition ? port.positionRequest : highestPosition;
            }
        }
        nextPos = highestPosition > nextPos ? highestPosition : nextPos;

        // Assign devicePort positions
        Iterator<DevicePort> it = device.getDevicePortIterator();
        while (it.hasNext()) {
            DevicePort port = it.next();
            port.positionRequest = nextPos;
            nextPos++;
        }

        // Already in configured devices?
        for (int i = devices.size() - 1; i >= 0; --i) {
            if (device.equalsByUniqueID(devices.get(i))) {
                devices.set(i, device);
                if (storage != null)
                    storage.devicesSave(this);
                notifyDeviceChangeObservers(device, false);
                return true;
            }
        }

        devices.add(device);
        notifyDeviceChangeObservers(device, false);
        if (storage != null)
            storage.devicesSave(this);
        return false;
    }

    public void clear() {
        devices.clear();
        notifyDeviceChangeObservers(null, true);
        if (storage != null)
            storage.devicesSave(this);
    }

    /**
     * Import String data (JSON) and either replace all existing data or merge it with the
     * existing data.
     *
     * @param tryToMerge If you merge the data instead of replacing the process is slower.
     */
    public boolean importData(boolean tryToMerge, String data) {
        DeviceCollection dc;
        try {
            dc = DeviceCollection.fromJSON(JSONHelper.getReader(data), null);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if (tryToMerge) {
            for (DeviceInfo di : dc.devices)
                add(di);
        } else {
            devices.clear();
            devices = dc.devices;

            notifyDeviceChangeObservers(null, true);
            if (storage != null)
                storage.devicesSave(this);
        }
        return true;
    }

    public void save() {
        if (storage != null)
            storage.devicesSave(this);
    }

    public void remove(DeviceInfo device) {
        int position = devices.indexOf(device);
        if (position == -1)
            return;
        device.configured = false;
        devices.remove(position);
        notifyDeviceChangeObservers(device, true);
        if (storage != null)
            storage.devicesSave(this);
    }

    public DeviceInfo update(DeviceInfo newValues_device) {
        for (DeviceInfo existing_device : devices) {
            if (!newValues_device.equalsByUniqueID(existing_device))
                continue;

            if (existing_device.copyFreshValues(newValues_device)) {
                notifyDeviceChangeObservers(existing_device, false);
            }

            return existing_device;
        }
        return null;
    }

    public boolean hasDevices() {
        return devices.size() > 0;
    }

    public void setDevicePortBitmap(Context context, DevicePort port, Bitmap bitmap) {
        if (port == null)
            return;

        Icons.saveIcon(context, port.uuid,
                Icons.resizeBitmap(context, bitmap, 128, 128),
                Icons.IconType.DevicePortIcon, port.getIconState());
        notifyDeviceChangeObservers(port.device, false);
    }

    public interface IDevicesSave {
        void devicesSave(DeviceCollection devices);
    }
}
