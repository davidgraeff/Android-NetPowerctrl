package oly.netpowerctrl.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import oly.netpowerctrl.utils.Logging;

/**
 * Describes a set of credentials and a callback. This object is handed over to the DeviceQuery class.
 * The callback is called after a timeout or if all given devices responded.
 */
public class DevicesObserver {
    public final boolean broadcast;
    public final Map<String, Credentials> credentialsList = new TreeMap<>();
    final long timeoutInMS = 2500; // maximum wait time before onObserverJobFinished is called
    final long minimumTimeInMS = 500; // mimimum waiit time (for broadcast queries important)
    final long deviceQualifiedAsOnlineTimeInMS = 5000;
    final onDevicesObserverFinished callback;
    final long startTime = System.currentTimeMillis();
    public boolean addAllExisting = false;
    public List<Credentials> success = new ArrayList<>();
    public List<Credentials> failed = new ArrayList<>();

    public DevicesObserver(Collection<Credentials> credentialsList, onDevicesObserverFinished callback) {
        this.broadcast = false;
        this.callback = callback;
        for (Credentials c : credentialsList)
            this.credentialsList.put(c.deviceUID, c);
        Logging.getInstance().logEnergy("Suche bekannte Geräte\n");
    }

    public DevicesObserver(Credentials credentials, onDevicesObserverFinished callback) {
        this.callback = callback;
        broadcast = false;
        credentialsList.put(credentials.deviceUID, credentials);
        Logging.getInstance().logEnergy("Suche Gerät " + credentials.getDeviceName() + "\n");
    }

    public DevicesObserver(onDevicesObserverFinished callback) {
        this.callback = callback;
        broadcast = true;
        addAllExisting = true;
        Logging.getInstance().logEnergy("Suche alle Geräte\n");
    }

    public boolean isAllTimedOut() {
        return success.isEmpty();
    }

    public List<Credentials> timedOutDevices() {
        return failed;
    }

    public interface onDevicesObserverFinished {
        /**
         * Implement this to get notified when the operation finished.
         */
        void onObserverJobFinished(DevicesObserver devicesObserver);
    }
}
