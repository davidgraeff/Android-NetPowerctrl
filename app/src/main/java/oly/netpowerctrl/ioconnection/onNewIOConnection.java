package oly.netpowerctrl.ioconnection;

import oly.netpowerctrl.data.AbstractBasePlugin;
import oly.netpowerctrl.devices.Credentials;

/**
 * Asynchronous callback used if {@link AbstractBasePlugin#addNewIOConnection(Credentials, onNewIOConnection)} is called.
 */
public interface onNewIOConnection {
    void newIOConnection(IOConnection ioConnection);
}