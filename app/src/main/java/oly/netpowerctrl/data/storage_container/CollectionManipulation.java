package oly.netpowerctrl.data.storage_container;

import oly.netpowerctrl.utils.IOInterface;

/**
 * Created by david on 27.04.15.
 */
public interface CollectionManipulation<ITEM extends IOInterface> {
    CollectionStorage<ITEM> getStorage();
}
