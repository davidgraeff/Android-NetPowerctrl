package oly.netpowerctrl.ioconnection;

import oly.netpowerctrl.credentials.Credentials;
import oly.netpowerctrl.data.AbstractBasePlugin;

/**
 * Asynchronous callback used if {@link AbstractBasePlugin#addNewIOConnection(Credentials, android.app.Activity)} is called.
 */
public interface onNewIOConnection {
    void newIOConnection(IOConnection ioConnection);
}
