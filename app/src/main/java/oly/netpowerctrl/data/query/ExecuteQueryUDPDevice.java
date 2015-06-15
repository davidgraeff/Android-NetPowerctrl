package oly.netpowerctrl.data.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import oly.netpowerctrl.credentials.Credentials;
import oly.netpowerctrl.ioconnection.DeviceIOConnections;
import oly.netpowerctrl.ioconnection.IOConnectionUDP;
import oly.netpowerctrl.network.UDPSend;

/**
 * Describes a credential, connection and udp data to send. This object is handed over to the DeviceQuery class.
 * The to-be-implemented finish() method is called after a timeout or if a response for the given device is received.
 */
public abstract class ExecuteQueryUDPDevice implements DeviceQueryInterface {
    final long timeoutInMS = 120; // maximum wait time before finish is called
    final long minimumTimeInMS = 100; // minimum wait time (for broadcast queries important)
    private final int[] repeatTimes = {80, 160};

    private final long startTime = System.currentTimeMillis();
    protected boolean mIsSuccess = false;
    protected IOConnectionUDP ioConnectionUDP;
    private boolean mIsDone = false;
    private Credentials credentials;
    private byte[] data;

    public ExecuteQueryUDPDevice(Credentials credentials, IOConnectionUDP ioConnectionUDP, byte[] data) {
        this.credentials = credentials;
        this.ioConnectionUDP = ioConnectionUDP;
        this.data = data;
    }

    public boolean isSuccess() {
        return mIsSuccess;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean removeContainedSuccess(Credentials credentials) {
        if (!credentials.deviceUID.equals(this.credentials.deviceUID)) return false;
        mIsSuccess = true;
        mIsDone = true;
        return true;
    }

    public boolean isEmpty() {
        return mIsDone;
    }

    @Override
    public int[] getRepeatTimes() {
        return repeatTimes;
    }

    @Override
    public List<Credentials> addAllToFailed() {
        mIsSuccess = false;
        mIsDone = true;
        List<Credentials> l = new ArrayList<>();
        l.add(credentials);
        return l;
    }

    @Override
    public Collection<Credentials> getCredentials() {
        List<Credentials> l = new ArrayList<>();
        l.add(credentials);
        return l;
    }

    @Override
    public boolean removeContainedFailed(Credentials credentials) {
        if (!credentials.deviceUID.equals(this.credentials.deviceUID)) return false;
        mIsSuccess = false;
        mIsDone = true;
        return true;
    }

    @Override
    public long computeMissingRuntimeUntilMinimum() {
        return minimumTimeInMS - (System.currentTimeMillis() - startTime);
    }

    @Override
    public boolean doAction(Credentials credentials, DeviceIOConnections deviceIOConnections) {
        UDPSend.sendMessage(ioConnectionUDP, data);
        return true;
    }

    @Override
    public long getTimeoutInMS() {
        return timeoutInMS;
    }
}
