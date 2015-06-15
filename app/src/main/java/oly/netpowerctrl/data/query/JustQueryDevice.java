package oly.netpowerctrl.data.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import oly.netpowerctrl.App;
import oly.netpowerctrl.R;
import oly.netpowerctrl.credentials.Credentials;
import oly.netpowerctrl.data.AbstractBasePlugin;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.ioconnection.DeviceIOConnections;
import oly.netpowerctrl.ioconnection.IOConnection;
import oly.netpowerctrl.network.ReachabilityStates;
import oly.netpowerctrl.utils.Logging;

/**
 * Describes a set of credentials and a callback. This object is handed over to the DeviceQuery class.
 * The callback is called after a timeout or if all given devices responded.
 */
public class JustQueryDevice implements DeviceQueryInterface {
    final long timeoutInMS = 2500; // maximum wait time before finish is called
    final long minimumTimeInMS = 500; // minimum wait time (for broadcast queries important)
    private final int[] repeatTimes = {10, 100, 200, 300};

    private final boolean broadcast;
    private final Map<String, Credentials> credentialsList = new TreeMap<>();
    private final onDevicesObserverFinished callback;
    private final long startTime = System.currentTimeMillis();
    private List<Credentials> success = new ArrayList<>();
    private List<Credentials> failed = new ArrayList<>();
    private int attempt = 0;

    public JustQueryDevice(Collection<Credentials> credentialsList, onDevicesObserverFinished callback, boolean newDevicesBroadcast) {
        this.broadcast = false;
        this.callback = callback;
        for (Credentials c : credentialsList)
            this.credentialsList.put(c.deviceUID, c);
        if (newDevicesBroadcast)
            Logging.getInstance().logEnergy("Suche alle Geräte\n");
        else
            Logging.getInstance().logEnergy("Suche bekannte Geräte\n");
    }

    public JustQueryDevice(Credentials credentials, onDevicesObserverFinished callback) {
        this.callback = callback;
        broadcast = false;
        credentialsList.put(credentials.deviceUID, credentials);
        Logging.getInstance().logEnergy("Suche Gerät " + credentials.getDeviceName() + "\n");
    }

    public boolean isAllTimedOut() {
        return success.isEmpty();
    }

    public List<Credentials> timedOutDevices() {
        return failed;
    }

    @Override
    public boolean isValid() {
        return broadcast || !credentialsList.isEmpty();
    }

    @Override
    public boolean removeContainedSuccess(Credentials credentials) {
        if (!credentialsList.containsKey(credentials.deviceUID)) return false;
        credentialsList.remove(credentials.deviceUID);
        success.add(credentials);
        return true;
    }

    public boolean isEmpty() {
        return credentialsList.isEmpty();
    }

    @Override
    public int[] getRepeatTimes() {
        return repeatTimes;
    }

    @Override
    public List<Credentials> addAllToFailed() {
        failed.addAll(credentialsList.values());
        credentialsList.clear();
        return failed;
    }

    @Override
    public Collection<Credentials> getCredentials() {
        return credentialsList.values();
    }

    @Override
    public boolean doAction(Credentials credentials, DeviceIOConnections deviceIOConnections) {
        AbstractBasePlugin abstractBasePlugin = credentials.getPlugin();
        // First try to find the not assigned plugin
        if (abstractBasePlugin == null) {
            abstractBasePlugin = DataService.getService().getPlugin(credentials.pluginID);
            credentials.setPlugin(abstractBasePlugin);
        }

        if (abstractBasePlugin == null) {
            // remove from list of devices to observe and notify observers
            deviceIOConnections.setStatusMessage(App.getAppString(R.string.error_plugin_not_installed));
            deviceIOConnections.setReachability(ReachabilityStates.NotReachable);
            return false;
        }

        if (!abstractBasePlugin.isStarted()) {
            deviceIOConnections.setReachability(ReachabilityStates.NotReachable);
            deviceIOConnections.setStatusMessage(App.getAppString(R.string.device_energysave_mode));
            return false;
        }

        if (!deviceIOConnections.lookupIP()) {
            return false;
        }

        IOConnection connection = deviceIOConnections.getConnectionByPositionModulo(attempt++);

        abstractBasePlugin.requestData(connection);
        return true;
    }

    @Override
    public boolean removeContainedFailed(Credentials credentials) {
        if (!credentialsList.containsKey(credentials.deviceUID)) return false;
        credentialsList.remove(credentials.deviceUID);
        failed.add(credentials);
        return true;
    }

    @Override
    public long computeMissingRuntimeUntilMinimum() {
        return minimumTimeInMS - (System.currentTimeMillis() - startTime);
    }


    @Override
    public void finish() {
        callback.onObserverJobFinished(this);
    }


    @Override
    public long getTimeoutInMS() {
        return timeoutInMS;
    }

    public interface onDevicesObserverFinished {
        /**
         * Implement this to get notified when the operation finished.
         */
        void onObserverJobFinished(JustQueryDevice justQueryDevice);
    }
}
