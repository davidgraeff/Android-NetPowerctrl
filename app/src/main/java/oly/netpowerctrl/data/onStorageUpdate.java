package oly.netpowerctrl.data;

/**
 * Created by david on 01.09.14.
 */
public interface onStorageUpdate {
    void save(CollectionWithType collection, StorableInterface item);

    void remove(CollectionWithType collection, StorableInterface item);

    void clear(CollectionWithType collection);
}
