package oly.netpowerctrl.devices;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AbstractBasePlugin;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.ioconnection.DeviceIOConnections;
import oly.netpowerctrl.ioconnection.IOConnection;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.network.ReachabilityStates;

/**
 * Use the static sendQuery and sendBroadcastQuery methods to issue a query to one
 * or all scenes. If you want to issue a query and get notified on the result or get a
 * timeout if no reaction can be received within 1.2s, create a DeviceQuery object with
 * all devices to query.
 */
public class DeviceQuery {
    private static final int MSG_REQUEST = 1;
    private static final int MSG_TIMEOUT = 2;
    private static final int MSG_RESPONSE = 3;
    private static final int MSG_ADD_OBSERVER = 4;
    private static final int MSG_RECHECK_MINIMUM_TIME = 5;
    @SuppressWarnings("unused")
    private static final String TAG = "DeviceQuery";
    private static int instances = 0;
    private final DataService dataService;
    private List<DevicesObserver> devicesObserverList = new ArrayList<>();
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
        }
    };

    public DeviceQuery(DataService dataService) {
        this.dataService = dataService;
        thread.start();
    }

    /**
     * Called from within this device observers thread to quit the looper and thread.
     * Will call onObserverJobFinished in the gui thread.
     */
    public void finish() {
        Looper.myLooper().quit();
    }

    protected void doAction(Credentials credentials, int attempt) {
        AbstractBasePlugin abstractBasePlugin = credentials.getPlugin();
        // First try to find the not assigned plugin
        if (abstractBasePlugin == null) {
            abstractBasePlugin = DataService.getService().getPlugin(credentials.pluginID);
            credentials.setPlugin(abstractBasePlugin);
        }

        DeviceIOConnections l = dataService.connections.openDevice(credentials.deviceUID);

        // No connections for this device. Nothing to do.
        if (l == null) {
            // remove from list of devices to observe and notify observers
            deviceFailed(credentials);
            return;
        }

        if (!credentials.enabled) {
            // remove from list of devices to observe and notify observers
            deviceFailed(credentials);

            l.setStatusMessage(dataService.getString(R.string.error_device_disabled));
            l.setReachability(ReachabilityStates.NotReachable);
            dataService.connections.put(l);
            return;
        }

        if (abstractBasePlugin == null) {
            // remove from list of devices to observe and notify observers
            deviceFailed(credentials);

            l.setStatusMessage(App.getAppString(R.string.error_plugin_not_installed));
            l.setReachability(ReachabilityStates.NotReachable);
            dataService.connections.put(l);
            return;
        }

        if (!abstractBasePlugin.isStarted()) {
            // remove from list of devices to observe and notify observers
            deviceFailed(credentials);

            l.setReachability(ReachabilityStates.NotReachable);
            l.setStatusMessage(App.getAppString(R.string.device_energysave_mode));
            dataService.connections.put(l);
            return;
        }

        if (!l.lookupIP()) {
            // remove from list of devices to observe and notify observers
            deviceFailed(credentials);

            dataService.connections.put(l);
        }

        IOConnection connection = l.getConnectionByPositionModulo(attempt);
        abstractBasePlugin.requestData(connection);

//        for (Iterator<IOConnection> iterator = l.iterator(); iterator.hasNext(); ) {
//            IOConnection connection = iterator.next();
//            abstractBasePlugin.requestData(connection);
//            //if (connection.reachableState() == ReachabilityStates.Reachable) break;
//        }
    }

    /**
     * Return true if all devices responded and this DeviceQuery object
     * can be removed.
     */
    private void deviceSuccess(Credentials credentials) {
        if (Thread.currentThread() != thread) throw new RuntimeException();
        Log.w(TAG, "Response " + credentials.deviceName);
        handler.removeMessages(MSG_REQUEST, credentials);
        for (Iterator<DevicesObserver> iterator = devicesObserverList.iterator(); iterator.hasNext(); ) {
            DevicesObserver devicesObserver = iterator.next();
            if (!devicesObserver.credentialsList.containsKey(credentials.deviceUID)) continue;
            devicesObserver.credentialsList.remove(credentials.deviceUID);
            devicesObserver.success.add(credentials);
            checkObserverFinished(iterator, devicesObserver);
        }
    }

    private void checkObserverFinished(@NonNull Iterator<DevicesObserver> iterator, @NonNull DevicesObserver devicesObserver) {
        if (Thread.currentThread() != thread) throw new RuntimeException();
        if (!devicesObserver.credentialsList.isEmpty()) return;

        // Remove timeouts for this observer. We will now either finish or wait for the minimum time
        // to call this method again.
        handler.removeMessages(MSG_TIMEOUT, devicesObserver);

        // Check if the observer is active for at least the given minimum time. First calculate runtime of the observer.
        long runtimeMS = System.currentTimeMillis() - devicesObserver.startTime;
        // If not running long enough, send recheck message. It will call this method again later.
        if (devicesObserver.minimumTimeInMS > runtimeMS) {
            handler.sendMessageDelayed(handler.obtainMessage(MSG_RECHECK_MINIMUM_TIME, devicesObserver), devicesObserver.minimumTimeInMS - runtimeMS);
            return;
        }

        iterator.remove();
        mainHandler.sendMessage(mainHandler.obtainMessage(0, devicesObserver));
    }

    private void deviceFailed(Credentials credentials) {
        Log.w(TAG, "Query failed " + credentials.getDeviceName());
        if (Thread.currentThread() != thread) throw new RuntimeException();
        handler.removeMessages(MSG_REQUEST, credentials);
        for (Iterator<DevicesObserver> iterator = devicesObserverList.iterator(); iterator.hasNext(); ) {
            DevicesObserver devicesObserver = iterator.next();
            if (!devicesObserver.credentialsList.containsKey(credentials.deviceUID)) continue;
            devicesObserver.credentialsList.remove(credentials.deviceUID);
            devicesObserver.failed.add(credentials);
            checkObserverFinished(iterator, devicesObserver);
        }
    }

    private void devicesTimeout(DevicesObserver devicesObserver) {
        if (Thread.currentThread() != thread) throw new RuntimeException();
        handler.removeMessages(MSG_TIMEOUT, devicesObserver);

        for (Iterator<Map.Entry<String, Credentials>> iterator = devicesObserver.credentialsList.entrySet().iterator(); iterator.hasNext(); ) {
            Credentials credentials = iterator.next().getValue();
            iterator.remove();
            devicesObserver.failed.add(credentials);
            DeviceIOConnections l = dataService.connections.openDevice(credentials.deviceUID);
            if (l != null) {
                l.setStatusMessage(App.getAppString(R.string.device_timeout));
                l.setReachability(ReachabilityStates.NotReachable);
                dataService.connections.put(l);
            }
        }

        for (Iterator<DevicesObserver> iterator = devicesObserverList.iterator(); iterator.hasNext(); ) {
            if (iterator.next() == devicesObserver) {
                checkObserverFinished(iterator, devicesObserver);
                break;
            }
        }
    }

    /**
     * Call this if a device responded. This is usually called by the IOConnectionCollection after
     * a device send a packet to us. This method will do nothing if no DevicesObserver is registered before.
     *
     * @param credentials The device that responded
     */
    public void deviceResponded(Credentials credentials) {
        if (devicesObserverList.isEmpty()) return;
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
     * @param devicesObserver The observer
     */
    public void addDeviceObserver(DevicesObserver devicesObserver) {
        if (devicesObserver.addAllExisting)
            for (Credentials c : dataService.credentials.getItems().values()) {
                devicesObserver.credentialsList.put(c.deviceUID, c);
            }

        if (!devicesObserver.broadcast && devicesObserver.credentialsList.isEmpty()) {
            devicesObserver.callback.onObserverJobFinished(devicesObserver);
            return;
        }
        handler.sendMessage(handler.obtainMessage(DeviceQuery.MSG_ADD_OBSERVER, devicesObserver));
    }

    /**
     * Main Handler. The only purpose is to call the DevicesObserver callback in the gui thread.
     */
    private static class MainHandler extends Handler {
        MainHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            DevicesObserver devicesObserver = (DevicesObserver) msg.obj;
            devicesObserver.callback.onObserverJobFinished(devicesObserver);
        }
    }

    /**
     * The thread handler of this DeviceQuery thread. All communication from outside is handled
     * by this handler.
     *
     * @see #addDeviceObserver(DevicesObserver)
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
                    DevicesObserver devicesObserver = (DevicesObserver) msg.obj;
                    devicesObserverList.add(devicesObserver);
                    // Add timeout
                    sendMessageDelayed(obtainMessage(MSG_TIMEOUT, devicesObserver), devicesObserver.timeoutInMS);
                    // Add devices
                    for (Credentials credentials : devicesObserver.credentialsList.values()) {
                        removeMessages(MSG_REQUEST, credentials);
                        sendMessageDelayed(obtainMessage(MSG_REQUEST, 0, 0, credentials), 10);
                        sendMessageDelayed(obtainMessage(MSG_REQUEST, 1, 0, credentials), 100);
                        sendMessageDelayed(obtainMessage(MSG_REQUEST, 2, 0, credentials), 200);
                        sendMessageDelayed(obtainMessage(MSG_REQUEST, 3, 0, credentials), 300);
                    }
                    break;
                }
                case MSG_REQUEST: {
                    doAction((Credentials) msg.obj, msg.arg1);
                    break;
                }
                case MSG_TIMEOUT: {
                    DevicesObserver devicesObserver = (DevicesObserver) msg.obj;
                    devicesTimeout(devicesObserver);
                    break;
                }
                case MSG_RECHECK_MINIMUM_TIME: {
                    DevicesObserver devicesObserver = (DevicesObserver) msg.obj;
                    for (Iterator<DevicesObserver> iterator = devicesObserverList.iterator(); iterator.hasNext(); ) {
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
            }
        }
    }
}
