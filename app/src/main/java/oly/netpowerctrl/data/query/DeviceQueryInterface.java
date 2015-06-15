package oly.netpowerctrl.data.query;

import java.util.Collection;
import java.util.List;

import oly.netpowerctrl.credentials.Credentials;
import oly.netpowerctrl.ioconnection.DeviceIOConnections;

/**
 * Created by david on 08.06.15.
 */
public interface DeviceQueryInterface {
    boolean removeContainedSuccess(Credentials credentials);

    boolean removeContainedFailed(Credentials credentials);

    long computeMissingRuntimeUntilMinimum();

    void finish();

    long getTimeoutInMS();

    /**
     * @return Return true if the Query is valid. An invalid query could be a specific devices query,
     * where no actual devices have been added.
     */
    boolean isValid();

    boolean isEmpty();

    /**
     * @return Return a list of times (e.g. 20, 100, 200) for repeating the request if no response
     * could be observed in the given time.
     */
    int[] getRepeatTimes();

    List<Credentials> addAllToFailed();

    Collection<Credentials> getCredentials();

    boolean doAction(Credentials credentials, DeviceIOConnections deviceIOConnections);
}
