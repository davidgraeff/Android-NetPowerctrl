package oly.netpowerctrl.utils;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A storable object have to provide a unique id that is file system compatible and
 * have to be loadable and storable by a stream.
 */
public interface IOInterface {
    // Return a unique id for the storage name
    String getUid();

    void load(@NonNull InputStream input) throws IOException, ClassNotFoundException;

    void save(@NonNull OutputStream output) throws IOException;

    boolean hasChanged();

    void resetChanged();
}
