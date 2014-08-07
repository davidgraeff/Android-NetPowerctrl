package oly.netpowerctrl.devices;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.device_ports.DevicePort;
import oly.netpowerctrl.network.DeviceUpdate;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.Icons;
import oly.netpowerctrl.utils.JSONHelper;

/**
 * Contains DeviceInfos. Used for NFC and backup transfers
 */
public class DeviceCollection {
    private final WeakHashMap<DeviceUpdate, Boolean> observersConfiguredDevice = new WeakHashMap<>();
    public List<Device> devices = new ArrayList<>();
    private IDevicesSave storage;

    public static DeviceCollection fromDevices(List<Device> devices, IDevicesSave storage) {
        DeviceCollection dc = new DeviceCollection();
        dc.storage = storage;
        dc.devices = devices;
        return dc;
    }

    /**
     * @param reader     A json reader
     * @param tryToMerge If you merge the data instead of replacing them the process will be slower.
     * @throws IOException
     * @throws IllegalStateException
     */
    public void fromJSON(JsonReader reader, boolean tryToMerge)
            throws IOException, IllegalStateException {

        if (reader == null)
            return;

        if (!tryToMerge)
            devices = new ArrayList<>();

        reader.beginArray();
        while (reader.hasNext()) {
            try {
                Device di = Device.fromJSON(reader);
                if (!tryToMerge)
                    devices.add(di);
                else {
                    add(di);
                }
            } catch (ClassNotFoundException e) {
                // If we read a device description, where we do not support that device type,
                // we just ignore that device and go on. Nevertheless print a backtrace.
                e.printStackTrace();
            }
        }
        reader.endArray();
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

    void toJSON(JsonWriter writer) throws IOException {
        writer.beginArray();
        for (Device di : devices) {
            di.toJSON(writer);
        }
        writer.endArray();
    }

    @SuppressWarnings("unused")
    public void registerDeviceObserver(DeviceUpdate o) {
        observersConfiguredDevice.put(o, true);
    }

    @SuppressWarnings("unused")
    public void unregisterDeviceObserver(DeviceUpdate o) {
        observersConfiguredDevice.remove(o);
    }

    void notifyDeviceObservers(Device di, boolean willBeRemoved) {
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
    public boolean add(Device device) {
        // Determine next free devicePort position.
        // This position is only for the listView to let all devicePorts of this new device
        // be at the end of the list.
        int highestPosition = 0;
        int nextPos = 0;
        for (Device local_device : devices) {
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
                save();
                notifyDeviceObservers(device, false);
                return true;
            }
        }

        devices.add(device);
        notifyDeviceObservers(device, false);
        save();
        return false;
    }

    public void removeAll() {
        devices.clear();
        notifyDeviceObservers(null, true);
        save();
    }

    public void save() {
        if (storage != null)
            storage.devicesSave(this);
    }

    public void remove(Device device) {
        int position = devices.indexOf(device);
        if (position == -1)
            return;
        device.configured = false;
        devices.remove(position);
        notifyDeviceObservers(device, true);
        save();
    }

    public void updateNotReachable(Device device) {
        notifyDeviceObservers(device, false);

        if (SharedPrefs.notifyDeviceNotReachable()) {
            long current_time = System.currentTimeMillis();
            Context context = NetpowerctrlApplication.instance;
            Toast.makeText(context,
                    context.getString(R.string.error_setting_outlet, device.DeviceName,
                            (int) ((current_time - device.getUpdatedTime()) / 1000)),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    public Device update(Device newValues_device) {
        for (Device existing_device : devices) {
            if (!newValues_device.equalsByUniqueID(existing_device))
                continue;

            if (existing_device.copyValuesFromUpdated(newValues_device)) {
                notifyDeviceObservers(existing_device, false);
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

        Icons.saveIcon(context, Icons.resizeBitmap(context, bitmap, 128, 128), port.uuid,
                Icons.IconType.DevicePortIcon, port.getIconState());
        notifyDeviceObservers(port.device, false);
    }

    public void setStorage(IDevicesSave storage) {
        this.storage = storage;
    }

    public interface IDevicesSave {
        void devicesSave(DeviceCollection devices);
    }
}
