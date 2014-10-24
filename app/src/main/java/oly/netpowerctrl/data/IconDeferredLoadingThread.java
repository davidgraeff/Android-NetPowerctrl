package oly.netpowerctrl.data;

import android.content.Context;
import android.graphics.drawable.Drawable;

import java.lang.ref.WeakReference;
import java.util.concurrent.LinkedBlockingQueue;

import oly.netpowerctrl.main.App;

/**
 * Load icons for the ViewHolder of DevicePortsBaseAdapter in a separate thread
 */
public class IconDeferredLoadingThread extends Thread {
    private final LinkedBlockingQueue<IconItem> q = new LinkedBlockingQueue<>();

    public IconDeferredLoadingThread() {
        super("IconDeferredLoadingThread");
    }

    public void loadIcon(IconItem job) {
        q.add(job);
    }

    @Override
    public void run() {
        while (true) {
            try {
                IconItem j = q.take();
                j.setFinalBitmap(LoadStoreIconData.loadDrawable(j.context, j.uuid,
                        j.iconType, j.state));
            } catch (InterruptedException e) {
                q.clear();
                return;
            }
        }
    }

    public interface IconLoaded {
        void setDrawable(Drawable bitmap, int position);
    }

    /**
     * An IconItem consists of all parameters necessary to call "Icons.loadIcon", the target
     * ViewHolder and the bitmap index.
     */
    public static class IconItem {
        private final String uuid;
        private final LoadStoreIconData.IconType iconType;
        private final LoadStoreIconData.IconState state;
        private final WeakReference<IconLoaded> target;
        private final int position;
        private final Context context;

        public IconItem(Context context, String uuid, LoadStoreIconData.IconType iconType, LoadStoreIconData.IconState state,
                        IconLoaded target, int position) {
            this.context = context;
            this.uuid = uuid;
            this.iconType = iconType;
            this.state = state;
            this.target = new WeakReference<>(target);
            this.position = position;
        }

        public void setFinalBitmap(final Drawable drawable) {
            App.getMainThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    IconLoaded viewHolder = target.get();
                    if (viewHolder == null)
                        return;
                    viewHolder.setDrawable(drawable, position);
                }
            });
        }

    }
}
