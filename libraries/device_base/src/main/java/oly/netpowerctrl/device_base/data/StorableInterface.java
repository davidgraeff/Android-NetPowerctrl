package oly.netpowerctrl.device_base.data;

import android.support.annotation.NonNull;
import android.util.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A storable object have to provide a unique id that is file system compatible and
 * have to be loadable and storable by a stream.
 */
public interface StorableInterface {
    public StorableDataType getDataType();

    public String getStorableName();

    public void load(@NonNull JsonReader reader) throws IOException, ClassNotFoundException;

    public void load(@NonNull InputStream input) throws IOException, ClassNotFoundException;

    public void save(@NonNull OutputStream output) throws IOException;

    // Describe this storable
    enum StorableDataType {
        BINARY, JSON
    }
}
