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
import android.util.DisplayMetrics;
import android.util.JsonReader;
import android.util.TypedValue;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.device_ports.DevicePort;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.utils.Streams;

/**
 * Util for scenes
 */
public class LoadStoreIconData {

    private static final int PICK_IMAGE_BEFORE_KITKAT = 10;
    private static final int PICK_IMAGE_KITKAT = 11;
    public static String defaultFallbackIconSet = "";
    private static WeakReference<Object> icon_callback_context_object;

    /**
     * Decode drawable from an URI. This is used after a file picker activity returned.
     *
     * @param context
     * @param uri
     * @return
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

    public static String uuidForBackground() {
        return new UUID(0xABCD, 0xABCD).toString();
    }

    public static String uuidFromWidgetID(int widgetId) {
        return new UUID(0xABCD, (long) widgetId).toString();
    }

    public static String uuidFromDefaultWidget() {
        return new UUID(0xABCE, 0).toString();
    }

    public static File getImageDirectory(Context context, IconType iconType, IconState state) {
        File dir = new File(context.getFilesDir(), "images");
        return new File(dir, iconType.name() + state.name());
    }

    public static void init(Context context) {
        { // Legacy dir support
            File dir = new File(App.instance.getFilesDir(), "images");
            File old_dir = App.instance.getDir("images", 0);
            if (old_dir.exists())
                old_dir.renameTo(dir);
        }
        defaultFallbackIconSet = SharedPrefs.getDefaultFallbackIconSet(context);
    }

    public static void saveIcon(Context context, Bitmap bitmap, String uuid, IconType iconType, IconState state) {
        File myDir = getImageDirectory(context, iconType, state);
        //noinspection ResultOfMethodCallIgnored
        myDir.mkdirs();

        String fileName = uuid;
        File file;

        file = new File(myDir, fileName + ".jpg");
        if (file.exists()) //noinspection ResultOfMethodCallIgnored
            file.delete();
        file = new File(myDir, fileName + ".png");
        if (file.exists()) //noinspection ResultOfMethodCallIgnored
            file.delete();

        if (bitmap == null) {
            return;

        }
        if (bitmap.hasAlpha()) {
            fileName += ".png";
        } else {
            fileName += ".jpg";
        }

        try {
            FileOutputStream out = new FileOutputStream(new File(myDir, fileName));
            if (bitmap.hasAlpha())
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            else
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
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

    public static Drawable loadDrawable(Context context, String uniqueID, IconType iconType, IconState state) {
        Bitmap b = loadIcon(context, uniqueID, iconType, state);
        if (b == null)
            return null;
        return new BitmapDrawable(context.getResources(), b);
    }

    public static Bitmap loadIcon(Context context, String uniqueID, IconType iconType, IconState state) {
        File myDir = getImageDirectory(context, iconType, state);

        File file = new File(myDir, uniqueID + ".png");
        if (!file.exists())
            file = new File(myDir, uniqueID + ".jpg");
        if (!file.exists()) {
            try {
                switch (state) {
                    case StateOff:
                        return BitmapFactory.decodeStream(context.getAssets().open("widget_icons/off_" + defaultFallbackIconSet + ".png"));
                    case OnlyOneState:
                        return BitmapFactory.decodeResource(context.getResources(), R.drawable.netpowerctrl);
                    case StateOn:
                        return BitmapFactory.decodeStream(context.getAssets().open("widget_icons/on_" + defaultFallbackIconSet + ".png"));
                    case StateUnknown:
                        return BitmapFactory.decodeStream(context.getAssets().open("widget_icons/unknown_" + defaultFallbackIconSet + ".png"));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
        return BitmapFactory.decodeFile(file.getAbsolutePath());
    }

    public static IconFile[] getAllIcons(Context context) {

        List<IconFile> list = new ArrayList<>();
        for (LoadStoreIconData.IconType iconType : LoadStoreIconData.IconType.values()) {
            for (LoadStoreIconData.IconState state : LoadStoreIconData.IconState.values()) {
                //noinspection ConstantConditions
                File myDir = getImageDirectory(context, iconType, state);
                for (File file : myDir.listFiles()) {
                    list.add(new IconFile(file, state, iconType));
                }
            }
        }

        return list.toArray(new IconFile[list.size()]);
    }

//    public static void saveIcon(Context context, String fileName, IconType iconType, IconState state, InputStream input) {
//        @SuppressWarnings("ConstantConditions")
//        File myDir = getImageDirectory(context, iconType, state);
//
//        try {
//            byte[] buffer = new byte[4096]; // To hold file contents
//            int bytes_read; // How many bytes in buffer
//            FileOutputStream out = new FileOutputStream(new File(myDir, fileName));
//            while ((bytes_read = input.read(buffer)) != -1)
//                // Read until EOF
//                out.write(buffer, 0, bytes_read); // write
//            out.flush();
//            out.close();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
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
            Drawable icon = LoadStoreIconData.loadDrawable(context, oi.getUid(),
                    LoadStoreIconData.IconType.DevicePortIcon, oi.getIconState());
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


    public static enum IconType {
        SceneIcon,
        WidgetIcon,
        DevicePortIcon,
        GroupIcon,
        BackgroundImage
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

    public static class IconFile implements StorableInterface {
        public final File file;
        public final IconState state;
        public final IconType type;
        public final UUID uuid;
        public final String extension;

        public IconFile(File file, IconState state, IconType type) {
            this.file = file;
            this.uuid = UUID.fromString(file.getName().replace(".jpg", "").replace(".png", ""));
            String fullPath = file.getAbsolutePath();
            int dot = fullPath.lastIndexOf(".");
            this.extension = fullPath.substring(dot + 1);
            this.state = state;
            this.type = type;
        }

        @Override
        public String getStorableName() {
            return type.name() + state.name() + File.pathSeparator + uuid.toString() + "." + extension;
        }

        @Override
        public void load(JsonReader reader) throws IOException, ClassNotFoundException {

        }

        @Override
        public void load(InputStream input) {

        }

        @Override
        public void save(OutputStream output) throws IOException {
            FileInputStream f = new FileInputStream(file);
            Streams.copy(f, output);
        }

        @Override
        public StorableDataType getDataType() {
            return StorableDataType.BINARY;
        }
    }
}
