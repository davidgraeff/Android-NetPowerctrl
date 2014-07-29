package oly.netpowerctrl.devices;

import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;

/**
 * Created by david on 28.07.14.
 */
public interface DeviceConnection {
    public void toJSON(JsonWriter writer) throws IOException;

    public boolean fromJSON(JsonReader reader) throws IOException, ClassNotFoundException;

    public boolean isReachable();

    public String getNotReachableReason();

    void setNotReachable(String not_reachable_reason);

    public void setReachable();

    public int getListenPort();

    public int getDestinationPort();

    public String getDestinationHost();

    String getProtocol();

    Device getDevice();

    String getString();
}
