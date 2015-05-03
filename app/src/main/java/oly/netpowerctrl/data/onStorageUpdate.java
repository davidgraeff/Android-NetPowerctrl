package oly.netpowerctrl.data;

import oly.netpowerctrl.data.storage_container.CollectionStorage;
import oly.netpowerctrl.utils.IOInterface;

/**
 * Created by david on 01.09.14.
 */
public interface onStorageUpdate {
    void save(CollectionStorage collection, IOInterface item);

    void remove(CollectionStorage collection, IOInterface item);

    void clear(CollectionStorage collection);
}
