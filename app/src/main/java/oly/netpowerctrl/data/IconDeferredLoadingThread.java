package oly.netpowerctrl.data;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.lang.ref.WeakReference;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Load icons for the ViewHolder of DevicePortsBaseAdapter in a separate thread
 */
public class IconDeferredLoadingThread extends Thread {
    private final LinkedBlockingQueue<IconItem> q = new LinkedBlockingQueue<>();
    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            IconItem j = (IconItem) msg.obj;
            IconLoaded viewHolder = j.target.get();
            if (viewHolder == null) return;
            viewHolder.setDrawable(j.drawable, j.position);
        }
    };

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
                j.drawable = LoadStoreIconData.loadDrawable(j.context, j.uuid, j.state, null);
                handler.obtainMessage(j.position, j).sendToTarget();
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
     * An IconItem consists of all parameters necessary to call "Icons.loadBitmap", the target
     * ViewHolder and the bitmap index.
     */
    public static class IconItem {
        private final String uuid;
        private final LoadStoreIconData.IconState state;
        private final WeakReference<IconLoaded> target;
        private final int position;
        private final Context context;
        private Drawable drawable;

        public IconItem(Context context, String uuid, LoadStoreIconData.IconState state,
                        IconLoaded target, int position) {
            this.context = context;
            this.uuid = uuid;
            this.state = state;
            this.target = new WeakReference<>(target);
            this.position = position;
        }
    }
}
