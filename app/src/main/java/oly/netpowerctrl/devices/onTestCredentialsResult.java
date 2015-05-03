package oly.netpowerctrl.devices;

/**
 * Created by david on 05.09.14.
 */
public interface onTestCredentialsResult {
    void testFinished(TestStates state, Credentials credentials);
}
