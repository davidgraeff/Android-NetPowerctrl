package oly.netpowerctrl.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by david on 01.09.14.
 */
public interface onCollectionUpdated<COLLECTION, ITEM> {
    /**
     * Will be called by a collection if an item changed/got removed/got inserted.
     * Position will reflect the items position if changed or inserted and will indicate the
     * last position before the item got removed.
     *
     * @return Return false to be removed as listener for further events
     */
    boolean updated(@NonNull COLLECTION collection, @Nullable ITEM item, @NonNull ObserverUpdateActions action, int position);
}
