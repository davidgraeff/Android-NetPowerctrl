package oly.netpowerctrl.pluginservice;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.network.onDeviceObserverResult;

/**
 * This base class is used by the device query and device-resend-command class.
 * It will be registered on the main application to receive device updates and
 * provide the result of a query/a sending action to the DeviceQueryResult object.
 */
public abstract class DeviceObserverBase extends Thread {
    protected static final int MSG_REPEAT = 1;
    protected static final int MSG_EXIT = 2;
    private static Queue<DeviceObserverBase> globalQueue = new ConcurrentLinkedQueue<>();

    protected final Context context;
    protected final List<Device> devices_to_observe = new ArrayList<>();
    protected final List<Device> timeout_devices = new ArrayList<>();
    protected final int repeatTimeout;
    protected int devices_to_obserse_size = 0;
    protected int repeatCountDown;
    protected RepeatHandler handler;
    private onDeviceObserverResult target;
    private AppData appData;

    protected DeviceObserverBase(Context context, onDeviceObserverResult target, int repeatTimeout, int repeatCount) {
        super("DeviceObserverBase");
        this.context = context;
        this.repeatTimeout = repeatTimeout;
        this.repeatCountDown = repeatCount;
        setDeviceQueryResult(target);
    }

    /**
     * Notify observers who are using the DeviceQuery class
     *
     * @param source Source device that has been updated
     */
    public static void notifyOfUpdatedDevice(Device source) {
        Iterator<DeviceObserverBase> it = globalQueue.iterator();
        while (it.hasNext()) {
            // Return true if the DeviceQuery object has finished its task.
            DeviceObserverBase deviceObserverBase = it.next();
            if (deviceObserverBase.notifyObservers(source)) {
                it.remove();
                deviceObserverBase.finishWithTimeouts();
            }
        }
    }

    public static void finishAll() {
        Iterator<DeviceObserverBase> it = globalQueue.iterator();
        while (it.hasNext()) {
            DeviceObserverBase deviceObserverBase = it.next();
            it.remove();
            deviceObserverBase.finishWithTimeouts();
        }
    }

    /**
     * Called in the main thread to finish up this object. At this point we should already
     * be removed or never added to globalQueue.
     */
    private void finishThread() {
        for (Device device : devices_to_observe) {
            device.lockDevice();
            device.setStatusMessageAllConnections(context.getString(R.string.error_timeout_device, ""));
            device.releaseDevice();
            timeout_devices.add(device);
        }

        App.getMainThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                for (Device device : timeout_devices) {
                    appData.updateDevice(device, false);
                }
                if (target != null)
                    target.onObserverJobFinished(DeviceObserverBase.this);
            }
        });

        Looper.myLooper().quit();
    }

    public boolean isAllTimedOut() {
        return devices_to_obserse_size == timeout_devices.size() && timeout_devices.size() > 0;
    }


    public List<Device> timedOutDevices() {
        return timeout_devices;
    }

    /**
     * Override this to be informed if the threads main is entered.
     *
     * @return Return false if you do not want DeviceObserverBase schedule
     * doAction(..) for you.
     * You have to call handler.sendEmptyMessage(MSG_REPEAT) yourself.
     * <p/>
     * If you want the thread to exit, return false and call finishWithTimeouts().
     */
    protected boolean runStarted() {
        return true;
    }

    @Override
    public void run() {
        Looper.prepare();
        handler = new RepeatHandler(Looper.myLooper());
        boolean callRepeat = runStarted();

        if (!isEmpty()) {
            globalQueue.add(this);
            if (callRepeat) handler.sendEmptyMessage(MSG_REPEAT);
        } else
            handler.sendEmptyMessage(MSG_EXIT);
        Looper.loop();
    }

    /**
     * Basic implementation. Just call doAction for every added device.
     */
    protected void repeat() {
        if (repeatCountDown < 0) {
            finishWithTimeouts();
            return;
        }

        --repeatCountDown;

        List<Device> deviceList = new ArrayList<>(devices_to_observe);
        for (Device device : deviceList) {
            doAction(device, repeatCountDown);
        }

        handler.sendEmptyMessageDelayed(MSG_REPEAT, repeatTimeout);
    }

    public void setDeviceQueryResult(onDeviceObserverResult target) {
        this.target = target;
    }

    /**
     * Will be called after startQuery. If broadcast flag is set, the device parameter will be null.
     * You may reset the broadcast flag while in doAction.
     * If no broadcast is set, doAction will be called for every added device.
     * After a given timeout without a device response, doAction will be called again.
     *
     * @param device           The device, that is still in devices_to_observe (no response so far) or null
     *                         if broadcast flag is set.
     * @param remainingRepeats Amount of repeats remaining.
     */
    protected abstract void doAction(@Nullable Device device, int remainingRepeats);

    /**
     * Return true if all devices responded and this DeviceQuery object
     * have to be removed.
     *
     * @param device The DeviceInfo object all observes should be notified of.
     */
    private boolean notifyObservers(Device device) {
        if (device.getUniqueDeviceID() == null)
            throw new RuntimeException("Fresh device without unique id not allowed!");

        Iterator<Device> it = devices_to_observe.iterator();
        while (it.hasNext()) {
            Device device_to_observe = it.next();
            // Special case: No unique id. IPs are compared instead
            // and the unique id is copied from the network device.
            if (device_to_observe.getUniqueDeviceID() == null) {
                if (device_to_observe.hasAddress(device.getHostnameIPs(false), false)) {
                    device_to_observe.lockDevice();
                    device_to_observe.setUniqueDeviceID(device.getUniqueDeviceID());
                    device_to_observe.releaseDevice();
                    device_to_observe.clearStatusMessageAllConnections();
                    it.remove();
                    break;
                }
            } else if (device_to_observe.equalsByUniqueID(device)) {
                it.remove();
                Log.w("Query", "found " + device.getDeviceName() + ", left: " + String.valueOf(devices_to_observe.size()));
                break;
            }
        }
        return devices_to_observe.isEmpty();
    }

    protected void notifyObserversInternal(Device device_from_observed_devices) {
        Iterator<Device> it = devices_to_observe.iterator();
        while (it.hasNext()) {
            Device device = it.next();
            if (device == device_from_observed_devices) {
                it.remove();
                break;
            }
        }
    }

    public void clearDevicesToObserve() {
        devices_to_observe.clear();
        timeout_devices.clear();
        devices_to_obserse_size = 0;
    }

    protected boolean isEmpty() {
        return devices_to_observe.isEmpty();
    }

    public boolean addDevice(AppData appData, final Device device) {
        this.appData = appData;
        if (!device.isEnabled()) {
            device.lockDevice();
            device.setStatusMessageAllConnections(context.getString(R.string.error_device_disabled));
            device.releaseDevice();
            timeout_devices.add(device);
            appData.updateDevice(device, false);
            return false;
        }

        // We explicitly allow devices without connections (udp/http). Those connections
        // may be added later by a broadcast response. Out of the same reason we allow
        // devices without a unique id (MAC address) if only the hostname is known.
        // But we do not allow a device without a unique id and without a hostname.
        if (device.getUniqueDeviceID() == null && device.hasNoConnections()) {
            device.lockDevice();
            device.setStatusMessageAllConnections(context.getString(R.string.error_device_incomplete));
            device.releaseDevice();
            timeout_devices.add(device);
            appData.updateDevice(device, false);
            return false;
        }

        ++devices_to_obserse_size;
        devices_to_observe.add(device);
        return true;
    }

    /**
     * Called right before this object is removed from the Application list
     * of DeviceQueries because the listener service has been shutdown. All
     * remaining device queries of this object have to timeout now.
     */
    public void finishWithTimeouts() {
        handler.removeMessages(MSG_REPEAT);
        Iterator<DeviceObserverBase> it = globalQueue.iterator();
        while (it.hasNext()) {
            DeviceObserverBase deviceObserverBase = it.next();
            if (deviceObserverBase == this) {
                it.remove();
                break;
            }
        }

        if (isAlive())
            handler.sendEmptyMessage(MSG_EXIT);
        else if (devices_to_observe.size() > 0)
            throw new RuntimeException("DeviceObserverBase not alive but devices left!");
    }


    protected class RepeatHandler extends Handler {
        RepeatHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REPEAT: {
                    repeat();
                    break;
                }
                case MSG_EXIT: {
                    finishThread();
                    break;
                }
            }
        }
    }
}
