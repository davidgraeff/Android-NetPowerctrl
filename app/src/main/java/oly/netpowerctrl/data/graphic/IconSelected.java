package oly.netpowerctrl.data.graphic;

import android.content.Intent;
import android.graphics.Bitmap;

/**
 * Created by david on 15.05.15.
 */
public interface IconSelected {
    void setIcon(Object context_object, Bitmap bitmap);

    void startActivityForResult(Intent intent, int requestCode);
}
