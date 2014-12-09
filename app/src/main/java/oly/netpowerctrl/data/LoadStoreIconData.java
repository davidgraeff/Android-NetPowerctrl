package oly.netpowerctrl.data;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.util.TypedValue;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.device_base.executables.ExecutableType;
import oly.netpowerctrl.main.App;

/**
 * Util for scenes
 */
public class LoadStoreIconData {

    private static final int PICK_IMAGE_BEFORE_KITKAT = 10;
    private static final int PICK_IMAGE_KITKAT = 11;
    private static final int INITIAL_ICON_CACHE_CAPACITY = 60;
    public static LruCache<String, CacheEntry> iconCache = new LruCache<>(INITIAL_ICON_CACHE_CAPACITY);
    public static String defaultFallbackIconSet = "";
    public static IconDeferredLoadingThread iconLoadingThread;
    public static IconCacheClearedObserver iconCacheClearedObserver = new IconCacheClearedObserver();
    private static WeakReference<Object> icon_callback_context_object;

    /**
     * Decode drawable from an URI. This is used after a file picker activity returned.
     *
     * @param context A context
     * @param uri     An uri
     * @return Returns a drawable
     * @throws IOException
     */
    private static BitmapDrawable getDrawableFromUri(Context context, Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                context.getContentResolver().openFileDescriptor(uri, "r");
        assert parcelFileDescriptor != null;
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return new BitmapDrawable(context.getResources(), image);
    }

    public static String uuidFromDefaultWidget() {
        return new UUID(0xABCE, 0).toString();
    }

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
            if (old_dir.exists())
                old_dir.renameTo(dir);
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
        SharedPrefs.getInstance().setDefaultFallbackIconSet(new_theme);
        iconCache.evictAll();
        iconCacheClearedObserver.onIconCacheCleared();
    }

    @NonNull
    public static File getFilename(@NonNull Context context,
                                   @NonNull String uuid, IconState state) {
        return new File(getImageDirectory(context, state), uuid + ".png");
    }

    public static File saveTempIcon(Context context, @Nullable Bitmap bitmap) {
        if (bitmap == null)
            return null;

        File file;
        try {
            file = File.createTempFile("scene_image", "bitmap", context.getCacheDir());
            file.deleteOnExit();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not create temp file!");
        }

        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return file;
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

    /**
     * Resize a bitmap to the size of an app icon
     *
     * @param context The context
     * @param bitmap  The bitmap
     * @return A bitmap in the size of an app icon
     */
    public static Bitmap resizeBitmap(Context context, Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float size = context.getResources().getDimension(android.R.dimen.app_icon_size);
        Matrix matrix = new Matrix();
        matrix.postScale(size / width, size / height);
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false);
    }

    public static Bitmap resizeBitmap(Context context, Bitmap bm, int newHeightDP, int newWidthDP) {
        if (bm == null)
            return null;
        int width = bm.getWidth();
        int height = bm.getHeight();
        DisplayMetrics m = context.getResources().getDisplayMetrics();
        float scaleWidth = (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newWidthDP, m)) / width;
        float scaleHeight = (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newHeightDP, m)) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
    }

    public static Drawable loadDrawable(Context context, String uniqueID,
                                        IconState state, @Nullable oly.netpowerctrl.utils.MutableBoolean isDefault) {
        Bitmap b = loadBitmap(context, uniqueID, state, isDefault);
        if (b == null)
            return null;
        return new BitmapDrawable(context.getResources(), b);
    }

    public static Bitmap loadBackgroundBitmap() {
        File file = new File(App.instance.getFilesDir(), "image_bg");
        if (!file.mkdirs() && !file.isDirectory())
            throw new RuntimeException("Could not create image dir!");
        file = new File(file, "bg.jpg");

        Bitmap b = null;
        if (file.exists()) {
            b = BitmapFactory.decodeFile(file.getAbsolutePath());
        }

        if (b == null)
            try {
                b = BitmapFactory.decodeStream(App.instance.getAssets().open("backgrounds/bg.jpg"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        return b;
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

    public static Bitmap loadBitmap(Context context, String uniqueID,
                                    IconState state, @Nullable oly.netpowerctrl.utils.MutableBoolean isDefault) {

        // Get from Cache
        String hashKey_original = uniqueID + String.valueOf(state.ordinal());
        CacheEntry cacheEntry = iconCache.get(hashKey_original);
        if (cacheEntry != null) {
            if (isDefault != null)
                isDefault.value = cacheEntry.isDefault;
            return cacheEntry.bitmap;
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
                iconCache.put(hashKey_original, new CacheEntry(bitmap, false));
        }

        // Load from default theme set
        if (bitmap == null) {
            if (isDefault != null)
                isDefault.value = true;
            try {
                // Try cache first
                String hashKey = String.valueOf(state.ordinal());
                cacheEntry = iconCache.get(hashKey);
                if (cacheEntry != null) {
                    // Cache entry for default found: Add cache entry for original request
                    iconCache.put(hashKey_original, new CacheEntry(cacheEntry.bitmap, true));
                    return cacheEntry.bitmap;
                }

                // Load default icon, save in cache
                bitmap = loadDefaultBitmap(context, state, defaultFallbackIconSet);
                iconCache.put(hashKey, new CacheEntry(bitmap, true));

                // Add cache entry for original request
                if (!hashKey_original.equals(hashKey))
                    iconCache.put(hashKey_original, new CacheEntry(bitmap, true));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return bitmap;
    }

    public static LoadStoreIconData.IconState getIconState(DevicePort devicePort) {
        LoadStoreIconData.IconState t = LoadStoreIconData.IconState.StateOff;
        if (devicePort.getCurrentValue() != devicePort.min_value &&
                (devicePort.getType() == ExecutableType.TypeToggle ||
                        devicePort.getType() == ExecutableType.TypeRangedValue))
            t = LoadStoreIconData.IconState.StateOn;
        return t;
    }

//    public static IconFile[] getAllIcons(Context context) {
//
//        List<IconFile> list = new ArrayList<>();
//        for (LoadStoreIconData.IconState state : LoadStoreIconData.IconState.values()) {
//            //noinspection ConstantConditions
//            File myDir = getImageDirectory(context, state);
//            for (File file : myDir.listFiles()) {
//                list.add(new IconFile(file, state));
//            }
//        }
//
//        return list.toArray(new IconFile[list.size()]);
//    }

    public static void show_select_icon_dialog(final Context context, String assetSet,
                                               final IconSelected callback, final Object callback_context_object) {
        AssetManager assetMgr = context.getAssets();
        Bitmap[] list_of_icons = null;

        ArrayAdapterWithIcons adapter = new ArrayAdapterWithIcons(context,
                android.R.layout.select_dialog_item,
                android.R.id.text1, new ArrayList<ArrayAdapterWithIcons.Item>());
        adapter.items.add(new ArrayAdapterWithIcons.Item(context.getString(R.string.dialog_icon_default), null));
        adapter.items.add(new ArrayAdapterWithIcons.Item(context.getString(R.string.dialog_icon_select), null));

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            String[] list_of_icon_paths = null;
            try {
                list_of_icon_paths = assetMgr.list(assetSet);
                list_of_icons = new Bitmap[list_of_icon_paths.length];
                int c = 0;
                for (String filename : list_of_icon_paths) {
                    list_of_icons[c] = BitmapFactory.decodeStream(assetMgr.open(assetSet + "/" + filename));
                    ++c;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (list_of_icons != null)
                for (int i = 0; i < list_of_icons.length; ++i) {
                    adapter.items.add(new ArrayAdapterWithIcons.Item(list_of_icon_paths[i],
                            new BitmapDrawable(context.getResources(), list_of_icons[i])));
                }
        }

        final Bitmap[] list_of_icons_dialog = list_of_icons;
        AlertDialog.Builder select_icon_dialog = new AlertDialog.Builder(context);
        select_icon_dialog.setTitle(context.getString(R.string.dialog_icon_title));
        if (callback_context_object instanceof DevicePort) {
            DevicePort oi = (DevicePort) callback_context_object;
            Drawable icon = LoadStoreIconData.loadDrawable(context, oi.getUid(), getIconState(oi), null);
            select_icon_dialog.setIcon(icon);
        }
        select_icon_dialog.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (i == 0) {
                    callback.setIcon(callback_context_object, null);
                } else if (i == 1) {
                    if (callback_context_object != null)
                        icon_callback_context_object = new WeakReference<>(callback_context_object);
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        callback.startActivityForResult(Intent.createChooser(intent,
                                        context.getResources().getString(R.string.dialog_icon_select)),
                                PICK_IMAGE_BEFORE_KITKAT
                        );
                    } else {
                        try {
                            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                            intent.setType("image/*");
                            intent.addCategory(Intent.CATEGORY_OPENABLE);
                            callback.startActivityForResult(intent, PICK_IMAGE_KITKAT);
                        } catch (ActivityNotFoundException ignored) {
                            Toast.makeText(context, "Cannot open file chooser", Toast.LENGTH_SHORT).show();
                        }
                    }

                } else {
                    assert list_of_icons_dialog != null;
                    callback.setIcon(callback_context_object, list_of_icons_dialog[i - 2]);
                }
                dialogInterface.dismiss();
            }
        });
        select_icon_dialog.create().show();
    }

    @SuppressLint("NewApi")
    public static void activityCheckForPickedImage(final Context context,
                                                   final IconSelected callback,
                                                   int requestCode, int resultCode,
                                                   Intent imageReturnedIntent) {
        if (resultCode == Activity.RESULT_OK && (requestCode == PICK_IMAGE_KITKAT || requestCode == PICK_IMAGE_BEFORE_KITKAT)) {
            Uri selectedImage = imageReturnedIntent.getData();
            try {
                Bitmap b = LoadStoreIconData.getDrawableFromUri(context, selectedImage).getBitmap();
                callback.setIcon(icon_callback_context_object != null ? icon_callback_context_object.get() : null,
                        LoadStoreIconData.resizeBitmap(context, b, 128, 128));
                icon_callback_context_object = null;
            } catch (IOException e) {
                Toast.makeText(context, context.getString(R.string.error_icon),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static void clearIconCache() {
        iconCache.evictAll();
        iconCacheClearedObserver.onIconCacheCleared();
    }

    public static enum IconState {
        StateOn,
        StateOff,
        StateUnknown,
        OnlyOneState,
        StateNotApplicable // State does not apply e.g. for a background
    }

    public static interface IconSelected {
        void setIcon(Object context_object, Bitmap bitmap);

        void startActivityForResult(Intent intent, int requestCode);
    }

    public static class CacheEntry {
        Bitmap bitmap;
        boolean isDefault;

        public CacheEntry(Bitmap bitmap, boolean isDefault) {
            this.bitmap = bitmap;
            this.isDefault = isDefault;
        }
    }

//    public static class IconFile implements StorableInterface {
//        public final File file;
//        public final IconState state;
//        public final UUID uuid;
//        public final String extension;
//
//        public IconFile(File file, IconState state) {
//            this.file = file;
//            this.uuid = UUID.fromString(file.getName().replace(".jpg", "").replace(".png", ""));
//            String fullPath = file.getAbsolutePath();
//            int dot = fullPath.lastIndexOf(".");
//            this.extension = fullPath.substring(dot + 1);
//            this.state = state;
//        }
//
//        @Override
//        public String getStorableName() {
//            return state.name() + File.pathSeparator + uuid.toString() + "." + extension;
//        }
//
//        @Override
//        public void load(@NonNull InputStream input) {
//
//        }
//
//        @Override
//        public void save(@NonNull OutputStream output) throws IOException {
//            FileInputStream f = new FileInputStream(file);
//            Streams.copy(f, output);
//        }
//    }
}
