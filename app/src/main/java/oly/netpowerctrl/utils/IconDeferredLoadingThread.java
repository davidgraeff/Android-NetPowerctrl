package oly.netpowerctrl.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;

import java.lang.ref.WeakReference;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import oly.netpowerctrl.application_state.NetpowerctrlApplication;

/**
 * Load icons for the ViewHolder of DevicePortsBaseAdapter in a separate thread
 */
public class IconDeferredLoadingThread extends Thread {
    public interface IconLoaded {
        void setDrawable(Drawable bitmap, int position);
    }

    private final LinkedBlockingQueue<IconItem> q = new LinkedBlockingQueue<IconItem>();

    public void loadIcon(IconItem job) {
        q.add(job);
    }

    @Override
    public void run() {
        while (true) {
            try {
                IconItem j = q.take();
                j.setFinalBitmap(Icons.loadDrawable(j.context, j.uuid,
                        j.iconType, j.state, j.default_resource));
            } catch (InterruptedException e) {
                q.clear();
                return;
            }
        }
    }

    /**
     * An IconItem consists of all parameters necessary to call "Icons.loadIcon", the target
     * ViewHolder and the bitmap index.
     */
    public static class IconItem {
        private final UUID uuid;
        private final Icons.IconType iconType;
        private final Icons.IconState state;
        private final int default_resource;
        private final WeakReference<IconLoaded> target;
        private final int position;
        private final Context context;

        public IconItem(Context context, UUID uuid, Icons.IconType iconType, Icons.IconState state,
                        int default_resource, IconLoaded target, int position) {
            this.context = context;
            this.uuid = uuid;
            this.iconType = iconType;
            this.state = state;
            this.default_resource = default_resource;
            this.target = new WeakReference<IconLoaded>(target);
            this.position = position;
        }

        public void setFinalBitmap(final Drawable drawable) {
            NetpowerctrlApplication.getMainThreadHandler().post(new Runnable() {
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
