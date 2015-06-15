package oly.netpowerctrl.data.query;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import oly.netpowerctrl.App;
import oly.netpowerctrl.R;
import oly.netpowerctrl.credentials.Credentials;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.ioconnection.DeviceIOConnections;
import oly.netpowerctrl.network.ReachabilityStates;

/**
 * Use the DeviceQuery object in the main service, to query devices, query a single device or execute
 * a command on a device and observe responses. The latter is useful for network UDP execution, where
 * we do not have a response/ack in the protocol layer and may want to resend the command if no response
 * is observed.
 */
public class DeviceQuery {
    private static final int MSG_ACTION = 1;
    private static final int MSG_TIMEOUT = 2;
    private static final int MSG_RESPONSE = 3;
    private static final int MSG_ADD_OBSERVER = 4;
    private static final int MSG_RECHECK_MINIMUM_TIME = 5;
    private static final int MSG_EXIT = 6;
    @SuppressWarnings("unused")
    private static final String TAG = "DeviceQuery";
    private static int instances = 0;
    private final DataService dataService;
    private List<DeviceQueryInterface> deviceQueryInterfaceList = new ArrayList<>();
    private RepeatHandler handler;
    private MainHandler mainHandler = new MainHandler();
    private Thread thread = new Thread("DeviceObserverBase") {
        @Override
        public void run() {
            if (++instances != 1)
                throw new RuntimeException("Only one instance for DeviceQuery allowed!");

            Looper.prepare();
            handler = new RepeatHandler(Looper.myLooper());
            Looper.loop();
            --instances;
        }
    };

    public DeviceQuery(DataService dataService) {
        this.dataService = dataService;
        thread.start();
    }

    /**
     * Called from within this device observers thread to quit the looper and thread.
     * Will call finish in the gui thread.
     */
    public void finish() {
        if (thread.isAlive()) {
            handler.sendEmptyMessage(MSG_EXIT);
        }
        try {
            thread.join(2000);
            thread.interrupt();
        } catch (InterruptedException ignored) {
        }
    }

    protected void doAction(Credentials credentials, DeviceQueryInterface deviceQueryInterface) {
        DeviceIOConnections deviceIOConnections = dataService.connections.openDevice(credentials.deviceUID);

        // No connections for this device. Nothing to do.
        if (deviceIOConnections == null || deviceIOConnections.size() == 0) {
            // remove from list of devices to observe and notify observers
            deviceFailed(credentials);
            return;
        }

        if (!credentials.enabled) {
            // remove from list of devices to observe and notify observers
            deviceFailed(credentials);

            deviceIOConnections.setStatusMessage(dataService.getString(R.string.error_device_disabled));
            deviceIOConnections.setReachability(ReachabilityStates.NotReachable);
            dataService.connections.put(deviceIOConnections);
            return;
        }

        if (!deviceQueryInterface.doAction(credentials, deviceIOConnections)) {
            deviceFailed(credentials);
            dataService.connections.put(deviceIOConnections);
        }
    }

    /**
     * Return true if all devices responded and this DeviceQuery object
     * can be removed.
     */
    private void deviceSuccess(Credentials credentials) {
        if (Thread.currentThread() != thread) throw new RuntimeException();
        Log.w(TAG, "Response " + credentials.deviceName);
        handler.removeMessages(MSG_ACTION, credentials);
        for (Iterator<DeviceQueryInterface> iterator = deviceQueryInterfaceList.iterator(); iterator.hasNext(); ) {
            DeviceQueryInterface devicesObserver = iterator.next();
            if (!devicesObserver.removeContainedSuccess(credentials)) continue;
            checkObserverFinished(iterator, devicesObserver);
        }
    }

    private void checkObserverFinished(@NonNull Iterator<DeviceQueryInterface> iterator, @NonNull DeviceQueryInterface devicesObserver) {
        if (Thread.currentThread() != thread) throw new RuntimeException();
        if (!devicesObserver.isEmpty()) return;

        // Remove timeouts for this observer. We will now either finish or wait for the minimum time
        // to call this method again.
        handler.removeMessages(MSG_TIMEOUT, devicesObserver);

        // Check if the observer is active for at least the given minimum time. First calculate runtime of the observer.
        long missingRuntime = devicesObserver.computeMissingRuntimeUntilMinimum();
        // If not running long enough, send recheck message. It will call this method again later.
        if (missingRuntime > 0) {
            handler.sendMessageDelayed(handler.obtainMessage(MSG_RECHECK_MINIMUM_TIME, devicesObserver), missingRuntime);
            return;
        }

        for (Credentials credentials : devicesObserver.getCredentials())
            handler.removeMessages(MSG_ACTION, credentials);

        // Remove from observers list and
        iterator.remove();
        mainHandler.sendMessage(mainHandler.obtainMessage(0, devicesObserver));
    }

    private void deviceFailed(Credentials credentials) {
        Log.w(TAG, "Query failed " + credentials.getDeviceName());
        if (Thread.currentThread() != thread) throw new RuntimeException();
        handler.removeMessages(MSG_ACTION, credentials);
        for (Iterator<DeviceQueryInterface> iterator = deviceQueryInterfaceList.iterator(); iterator.hasNext(); ) {
            DeviceQueryInterface devicesObserver = iterator.next();
            if (!devicesObserver.removeContainedFailed(credentials)) continue;
            checkObserverFinished(iterator, devicesObserver);
        }
    }

    private void devicesTimeout(DeviceQueryInterface devicesObserver) {
        if (Thread.currentThread() != thread) throw new RuntimeException();
        handler.removeMessages(MSG_TIMEOUT, devicesObserver);

        List<Credentials> credentialsList = devicesObserver.addAllToFailed();

        for (Credentials credentials : credentialsList) {
            DeviceIOConnections l = dataService.connections.openDevice(credentials.deviceUID);
            if (l != null) {
                l.setStatusMessage(App.getAppString(R.string.device_timeout));
                l.setReachability(ReachabilityStates.NotReachable);
                dataService.connections.put(l);
            }
        }

        for (Iterator<DeviceQueryInterface> iterator = deviceQueryInterfaceList.iterator(); iterator.hasNext(); ) {
            if (iterator.next() == devicesObserver) {
                checkObserverFinished(iterator, devicesObserver);
                break;
            }
        }
    }

    /**
     * Call this if a device responded. This is usually called by the IOConnectionCollection after
     * a device send a packet to us. This method will do nothing if no DeviceQueryInterface is registered before.
     *
     * @param credentials The device that responded
     */
    public void deviceResponded(Credentials credentials) {
        if (deviceQueryInterfaceList.isEmpty()) return;
        handler.sendMessage(handler.obtainMessage(DeviceQuery.MSG_RESPONSE, credentials));
    }

    /**
     * Register a deviceObserver and request a response from all your given devices.
     * You will get notified if the request is finished and the given minimum waiting time is over
     * and either all devices responded or after a given timeout.
     *
     * This method will do nothing if the given observer is not a broadcast observer and also
     * has no devices to observe.
     *
     * @param deviceQueryInterface The observer
     */
    public void addDeviceObserver(DeviceQueryInterface deviceQueryInterface) {

        if (!deviceQueryInterface.isValid()) {
            deviceQueryInterface.finish();
            return;
        }
        handler.sendMessage(handler.obtainMessage(DeviceQuery.MSG_ADD_OBSERVER, deviceQueryInterface));
    }

    /**
     * Main Handler. The only purpose is to call the DeviceQueryInterface callback in the gui thread.
     */
    private static class MainHandler extends Handler {
        MainHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            DeviceQueryInterface deviceQueryInterface = (DeviceQueryInterface) msg.obj;
            deviceQueryInterface.finish();
        }
    }

    class CredentialsAndQuery {
        DeviceQueryInterface deviceQueryInterface;
        Credentials credentials;

        public CredentialsAndQuery(DeviceQueryInterface deviceQueryInterface, Credentials credentials) {
            this.deviceQueryInterface = deviceQueryInterface;
            this.credentials = credentials;
        }
    }

    /**
     * The thread handler of this DeviceQuery thread. All communication from outside is handled
     * by this handler.
     *
     * @see #addDeviceObserver(DeviceQueryInterface)
     * @see #deviceResponded(Credentials)
     */
    public class RepeatHandler extends Handler {
        RepeatHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ADD_OBSERVER: {
                    DeviceQueryInterface devicesObserver = (DeviceQueryInterface) msg.obj;
                    deviceQueryInterfaceList.add(devicesObserver);
                    // Add timeout
                    sendMessageDelayed(obtainMessage(MSG_TIMEOUT, devicesObserver), devicesObserver.getTimeoutInMS());
                    int[] repeatTimes = devicesObserver.getRepeatTimes();
                    // Add devices
                    for (Credentials credentials : devicesObserver.getCredentials()) {
                        removeMessages(MSG_ACTION, credentials);
                        for (int repeatTime : repeatTimes)
                            sendMessageDelayed(obtainMessage(MSG_ACTION, new CredentialsAndQuery(devicesObserver, credentials)), repeatTime);
                    }
                    break;
                }
                case MSG_ACTION: {
                    CredentialsAndQuery credentialsAndQuery = (CredentialsAndQuery) msg.obj;
                    doAction(credentialsAndQuery.credentials, credentialsAndQuery.deviceQueryInterface);
                    break;
                }
                case MSG_TIMEOUT: {
                    DeviceQueryInterface devicesObserver = (DeviceQueryInterface) msg.obj;
                    devicesTimeout(devicesObserver);
                    break;
                }
                case MSG_RECHECK_MINIMUM_TIME: {
                    DeviceQueryInterface devicesObserver = (DeviceQueryInterface) msg.obj;
                    for (Iterator<DeviceQueryInterface> iterator = deviceQueryInterfaceList.iterator(); iterator.hasNext(); ) {
                        if (iterator.next() == devicesObserver) {
                            checkObserverFinished(iterator, devicesObserver);
                            break;
                        }
                    }
                    break;
                }
                case MSG_RESPONSE: {
                    Credentials credentials = (Credentials) msg.obj;
                    deviceSuccess(credentials);
                    break;
                }
                case MSG_EXIT: {
                    //noinspection ConstantConditions
                    Looper.myLooper().quit();
                    break;
                }
            }
        }
    }
}
