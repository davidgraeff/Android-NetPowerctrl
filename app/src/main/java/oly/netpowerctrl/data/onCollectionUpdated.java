package oly.netpowerctrl.data;

/**
 * Created by david on 01.09.14.
 */
public interface onCollectionUpdated<COLLECTION, ITEM> {
    // Return false to be removed as listener for further events
    boolean updated(COLLECTION collection, ITEM item, ObserverUpdateActions action);
}
