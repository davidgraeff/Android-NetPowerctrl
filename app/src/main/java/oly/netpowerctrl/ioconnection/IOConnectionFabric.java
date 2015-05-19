package oly.netpowerctrl.ioconnection;

import android.util.JsonReader;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import oly.netpowerctrl.ioconnection.adapter.IOConnectionIP;
import oly.netpowerctrl.utils.FactoryInterface;

/**
 * Return an IOConnection by providing a json reader or FileInputStream.
 */
public class IOConnectionFabric implements FactoryInterface<IOConnection> {
    @Override
    public IOConnection newInstance(FileInputStream fileInputStream) throws IOException, ClassNotFoundException {
        JsonReader reader = new JsonReader(new InputStreamReader(fileInputStream));
        reader.beginObject();
        if (!reader.hasNext()) {
            return null;
        }

        String name = reader.nextName();
        if (!name.equals("connection_type")) {
            Log.e("DeviceConnection", "Expected connection_type first! " + name);
            reader.endObject();
            return null;
        }

        name = reader.nextString();

        IOConnection ioConnection;

        switch (name) {
            case IOConnectionUDP.PROTOCOL:
                ioConnection = new IOConnectionUDP();
                break;
            case IOConnectionHTTP.PROTOCOL:
                ioConnection = new IOConnectionHTTP();
                break;
            case IOConnectionIP.PROTOCOL:
                ioConnection = new IOConnectionIP();
                break;
            default:
                throw new ClassNotFoundException("Unexpected connection_type: " + name);
        }

        ioConnection.load(reader, true);

        return ioConnection;
    }
}
