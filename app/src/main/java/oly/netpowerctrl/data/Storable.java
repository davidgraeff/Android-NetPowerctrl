package oly.netpowerctrl.data;

import android.util.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A storable object have to provide a unique id that is file system compatible and
 * have to be loadable and storable by a stream.
 */
public interface Storable {
    public StorableDataType getDataType();

    ;

    public String getStorableName();

    public void load(JsonReader reader) throws IOException, ClassNotFoundException;

    public void load(InputStream input) throws IOException, ClassNotFoundException;

    public void save(OutputStream output) throws IOException;

    // Describe this storable
    enum StorableDataType {
        BINARY, JSON
    }
}
