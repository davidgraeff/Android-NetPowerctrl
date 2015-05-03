package oly.netpowerctrl.ioconnection.adapter;

import oly.netpowerctrl.devices.Credentials;

/**
 * Created by david on 25.04.15.
 */
public abstract class AdapterItem {
    protected final IOConnectionAdapter adapter;
    public String title;
    public String subtitle;
    public Credentials credentials;
    public boolean enabled = true;
    public boolean isConfigured;
    protected int last_known_position = 0;
    protected String deviceUID;
    protected String UID;

    protected AdapterItem(IOConnectionAdapter adapter) {
        this.adapter = adapter;
    }

    public String getDeviceUID() {
        return deviceUID;
    }

    public boolean matchesDeviceUID(String deviceUID) {
        return deviceUID.equals(deviceUID);
    }

    public boolean matchesUID(String UID) {
        return this.UID.equals(UID);
    }

    public abstract void destroy();
}
