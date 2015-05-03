package oly.netpowerctrl.utils;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Some collections like the ioconnection collection have different types of objects (with the same base class)
 * which needs a factory to initiate/load them. This interface have to be implemented for a factory
 * to be used by LoadStoreCollections.
 */
public interface FactoryInterface<ITEM> {
    ITEM newInstance(FileInputStream fileInputStream) throws IOException, ClassNotFoundException;
}
