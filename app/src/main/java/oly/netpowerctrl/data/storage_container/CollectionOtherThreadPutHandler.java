package oly.netpowerctrl.data.storage_container;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.lang.ref.WeakReference;

/**
 * Created by david on 28.04.15.
 */
public class CollectionOtherThreadPutHandler<ITEM> extends Handler {
    public final WeakReference<CollectionOtherThreadPut<ITEM>> weakReference;

    public CollectionOtherThreadPutHandler(WeakReference<CollectionOtherThreadPut<ITEM>> weakReference) {
        super(Looper.getMainLooper());
        this.weakReference = weakReference;
    }

    @Override
    public void handleMessage(Message msg) {
        CollectionOtherThreadPut<ITEM> collectionOtherThreadPut = weakReference.get();
        if (collectionOtherThreadPut == null) {
            return;
        }
        collectionOtherThreadPut.put((ITEM) msg.obj);
    }
}
