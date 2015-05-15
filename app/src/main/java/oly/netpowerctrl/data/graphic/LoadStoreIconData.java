package oly.netpowerctrl.data.graphic;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.LruCache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import oly.netpowerctrl.R;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.executables.ExecutableType;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.preferences.SharedPrefs;

/**
 * Util for scenes
 */
public class LoadStoreIconData {
    private static final int INITIAL_ICON_CACHE_CAPACITY = 60;
    public static LruCache<String, DrawableCacheEntry> iconCache = new LruCache<>(INITIAL_ICON_CACHE_CAPACITY);
    public static String defaultFallbackIconSet = null;
    public static IconDeferredLoadingThread iconLoadingThread;
    public static IconCacheClearedObserver iconCacheClearedObserver = new IconCacheClearedObserver();


    public static File getImageDirectory(Context context, IconState state) {
        File dir = new File(context.getFilesDir(), "images");
        dir = new File(dir, state.name());
        if (!dir.mkdirs() && !dir.isDirectory())
            throw new RuntimeException("Could not create image dir!");
        return dir;
    }

    public static void onCreate(Context context) {
        { // Legacy dir support
            File dir = new File(App.instance.getFilesDir(), "images");
            File old_dir = App.instance.getDir("images", 0);
            if (!dir.exists() && old_dir.exists())
                if (!old_dir.renameTo(dir)) {
                    throw new RuntimeException("Legacy file renaming failed!");
                } else if (old_dir.exists()) {
                    if (!old_dir.delete())
                        throw new RuntimeException("Legacy directory delete failed!");
                }
        }
        defaultFallbackIconSet = SharedPrefs.getDefaultFallbackIconSet(context);
        iconLoadingThread = new IconDeferredLoadingThread();
        iconLoadingThread.start();
    }

    public static void onDestroy() {
        if (iconLoadingThread != null)
            iconLoadingThread.interrupt();
        iconLoadingThread = null;
    }

    public static void setDefaultFallbackIconSet(String new_theme) {
        defaultFallbackIconSet = new_theme;
        iconCache.evictAll();
        SharedPrefs.getInstance().setDefaultFallbackIconSet(new_theme);
        iconCacheClearedObserver.onIconCacheCleared();
    }

    @NonNull
    public static File getFilename(@NonNull Context context,
                                   @NonNull String uuid, IconState state) {
        return new File(getImageDirectory(context, state), uuid + ".png");
    }

    public static void saveIcon(@NonNull Context context, @Nullable Bitmap bitmap,
                                @NonNull String uuid, IconState state) {
        File file = getFilename(context, uuid, state);
        if (file.exists()) //noinspection ResultOfMethodCallIgnored
            file.delete();

        String hashKey = uuid + String.valueOf(state.ordinal());
        iconCache.remove(hashKey);

        if (bitmap == null) {
            return;
        }

        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static IconState getIconState(Executable executable) {
        IconState t = IconState.StateOff;
        if (executable.getCurrentValue() != executable.getMinimumValue() &&
                (executable.getType() == ExecutableType.TypeToggle ||
                        executable.getType() == ExecutableType.TypeRangedValue))
            t = IconState.StateOn;
        return t;
    }

    public static Drawable loadDrawable(Executable executable,
                                        IconState state, @Nullable oly.netpowerctrl.utils.MutableBoolean isDefault) {
        Bitmap b = loadBitmap(App.instance, executable, state, isDefault);
        if (b == null)
            return null;
        return new BitmapDrawable(App.instance.getResources(), b);
    }

    public static Drawable loadBackgroundBitmap() {
        File file = new File(App.instance.getFilesDir(), "image_bg");
        if (!file.mkdirs() && !file.isDirectory())
            throw new RuntimeException("Could not create image dir!");
        file = new File(file, "bg.jpg");

        Bitmap b = null;
        if (file.exists())
            b = BitmapFactory.decodeFile(file.getAbsolutePath());

        if (b == null) {
            WallpaperManager w = WallpaperManager.getInstance(App.instance);
            return w.getDrawable();
        }

        return new BitmapDrawable(App.instance.getResources(), b);
    }

    public static void saveBackground(Bitmap bitmap) {
        File file = new File(App.instance.getFilesDir(), "image_bg");
        if (!file.mkdirs() && !file.isDirectory())
            throw new RuntimeException("Could not create image dir!");
        file = new File(file, "bg.jpg");

        if (file.exists()) //noinspection ResultOfMethodCallIgnored
            file.delete();

        if (bitmap == null) {
            return;
        }

        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Bitmap loadDefaultBitmap(Context context, IconState state, String iconTheme) throws IOException {
        switch (state) {
            case StateOff:
                return BitmapFactory.decodeStream(context.getAssets().open("widget_icons/off_" + iconTheme + ".png"));
            case OnlyOneState:
                return BitmapFactory.decodeResource(context.getResources(), R.drawable.netpowerctrl);
            case StateOn:
                return BitmapFactory.decodeStream(context.getAssets().open("widget_icons/on_" + iconTheme + ".png"));
            case StateUnknown:
                return BitmapFactory.decodeStream(context.getAssets().open("widget_icons/unknown_" + iconTheme + ".png"));
        }
        return null;
    }

    public static Bitmap loadBitmap(Context context, @NonNull Executable executable,
                                    IconState state, @Nullable oly.netpowerctrl.utils.MutableBoolean isDefault) {

        // Get from Cache
        String uniqueID = executable.getUid();
        String hashKey_original = uniqueID + String.valueOf(state.ordinal());
        DrawableCacheEntry drawableCacheEntry = iconCache.get(hashKey_original);
        if (drawableCacheEntry != null) {
            if (isDefault != null)
                isDefault.value = drawableCacheEntry.isDefault;
            return drawableCacheEntry.bitmap;
        }

        // Load from file
        Bitmap bitmap = null;
        File file = getFilename(context, uniqueID, state);
        if (file.exists()) {
            bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (bitmap == null)
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            else
                iconCache.put(hashKey_original, new DrawableCacheEntry(bitmap, false));
        }

        // Load from default theme set
        if (bitmap == null) {
            if (isDefault != null)
                isDefault.value = true;
            try {
                // Try cache first
                String hashKey = String.valueOf(state.ordinal());
                drawableCacheEntry = iconCache.get(hashKey);
                if (drawableCacheEntry != null) {
                    // Cache entry for default found: Add cache entry for original request
                    iconCache.put(hashKey_original, new DrawableCacheEntry(drawableCacheEntry.bitmap, true));
                    return drawableCacheEntry.bitmap;
                }

                if (defaultFallbackIconSet == null)
                    throw new RuntimeException();

                // Load default icon, save in cache
                bitmap = loadDefaultBitmap(context, state, defaultFallbackIconSet);
                iconCache.put(hashKey, new DrawableCacheEntry(bitmap, true));

                // Add cache entry for original request
                if (!hashKey_original.equals(hashKey))
                    iconCache.put(hashKey_original, new DrawableCacheEntry(bitmap, true));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return bitmap;
    }

    public static void clearIconCache() {
        iconCache.evictAll();
        iconCacheClearedObserver.onIconCacheCleared();
    }
}
