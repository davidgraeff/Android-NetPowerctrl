package oly.netpowerctrl.data.graphic;

import android.graphics.Bitmap;

/**
 * Created by david on 15.05.15.
 */
public class DrawableCacheEntry {
    Bitmap bitmap;
    boolean isDefault;

    public DrawableCacheEntry(Bitmap bitmap, boolean isDefault) {
        this.bitmap = bitmap;
        this.isDefault = isDefault;
    }
}
