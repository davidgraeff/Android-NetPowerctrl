package oly.netpowerctrl.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.device_ports.DevicePort;

/**
 * Util for scenes
 */
public class Icons {

    private static final int PICK_IMAGE_BEFORE_KITKAT = 1;
    private static final int PICK_IMAGE_KITKAT = 2;
    private static WeakReference<Object> icon_callback_context_object;

    public static void animateView(final View view, final boolean in, final float max) {
        float c = view.getAlpha();
        if (c >= max && in || c == 0.0f && !in) {
            return;
        }

        if (view.getAnimation() != null && !view.getAnimation().hasEnded())
            return;

        AlphaAnimation animation1 = new AlphaAnimation(c, in ? max : 0.0f);
        animation1.setInterpolator(new AccelerateInterpolator());
        animation1.setDuration(500);
        animation1.setStartOffset(0);
        animation1.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setAlpha(in ? max : 0.0f);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        view.startAnimation(animation1);
    }

    private static BitmapDrawable getDrawableFromUri(Context context, Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                context.getContentResolver().openFileDescriptor(uri, "r");
        assert parcelFileDescriptor != null;
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return new BitmapDrawable(context.getResources(), image);
    }

    public static UUID uuidFromWidgetID(int widgetId) {
        return new UUID(0xABCD, (long) widgetId);
    }

    public static int getResIdForState(IconState state) {
        switch (state) {
            case StateOff:
                return R.drawable.stateoff;
            case StateOn:
                return R.drawable.stateon;
            case StateUnknown:
                return R.drawable.stateunknown;
            case StateToggle:
                return R.drawable.netpowerctrl;
        }
        return 0;
    }

    public static void saveIcon(Context context, Bitmap bitmap, UUID uuid, IconType iconType, IconState state) {
        @SuppressWarnings("ConstantConditions")
        String root = context.getExternalFilesDir(iconType.name() + state.name()).toString();
        File myDir = new File(root);

        String fileName = uuid.toString();
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
     * @param context
     * @param bm
     * @return
     */
    public static Bitmap resizeBitmap(Context context, Bitmap bm) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float size = context.getResources().getDimension(android.R.dimen.app_icon_size);
        Matrix matrix = new Matrix();
        matrix.postScale(size / width, size / height);
        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
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

    public static Drawable loadDrawable(Context context, UUID uuid, IconType iconType, IconState state, int default_resource) {
        Bitmap b = loadIcon(context, uuid, iconType, state, default_resource);
        return new BitmapDrawable(context.getResources(), b);
    }

    public static Bitmap loadIcon(Context context, UUID uuid, IconType iconType, IconState state, int default_resource) {
        @SuppressWarnings("ConstantConditions")
        String root = context.getExternalFilesDir(iconType.name() + state.name()).toString();
        File myDir = new File(root);

        File file = new File(myDir, uuid.toString() + ".png");
        if (!file.exists())
            file = new File(myDir, uuid.toString() + ".jpg");
        if (!file.exists()) {
            if (default_resource == 0)
                return null;
            return BitmapFactory.decodeResource(context.getResources(), default_resource);
        }
        return BitmapFactory.decodeFile(file.getAbsolutePath());
    }

    public static IconFile[] getAllIcons() {
        Context context = NetpowerctrlApplication.instance;

        List<IconFile> list = new ArrayList<>();
        for (Icons.IconType iconType : Icons.IconType.values()) {
            for (Icons.IconState state : Icons.IconState.values()) {
                //noinspection ConstantConditions
                String root = context.getExternalFilesDir(iconType.name() + state.name()).toString();
                File myDir = new File(root);
                for (File file : myDir.listFiles()) {
                    list.add(new IconFile(file, state, iconType));
                }
            }
        }

        return list.toArray(new IconFile[list.size()]);
    }

    public static void saveIcon(String fileName, IconType iconType, IconState state, InputStream input) {
        Context context = NetpowerctrlApplication.instance;
        @SuppressWarnings("ConstantConditions")
        String root = context.getExternalFilesDir(iconType.name() + state.name()).toString();
        File myDir = new File(root);

        try {
            byte[] buffer = new byte[4096]; // To hold file contents
            int bytes_read; // How many bytes in buffer
            FileOutputStream out = new FileOutputStream(new File(myDir, fileName));
            while ((bytes_read = input.read(buffer)) != -1)
                // Read until EOF
                out.write(buffer, 0, bytes_read); // write
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
            Drawable icon = Icons.loadDrawable(context, oi.uuid,
                    Icons.IconType.DevicePortIcon, oi.getIconState(), 0);
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
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.setType("image/*");
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        callback.startActivityForResult(intent, PICK_IMAGE_KITKAT);
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
        if (resultCode == Activity.RESULT_OK) {
            Uri selectedImage = imageReturnedIntent.getData();
            try {
                Bitmap b = Icons.getDrawableFromUri(context, selectedImage).getBitmap();
                callback.setIcon(icon_callback_context_object != null ? icon_callback_context_object.get() : null,
                        Icons.resizeBitmap(context, b, 128, 128));
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
        GroupIcon
    }


    public static enum IconState {
        StateOn,
        StateOff,
        StateUnknown, StateToggle
    }

    public static interface IconSelected {
        void setIcon(Object context_object, Bitmap bitmap);

        void startActivityForResult(Intent intent, int requestCode);
    }

    public static class IconFile {
        public final File file;
        public final IconState state;
        public final IconType type;

        public IconFile(File file, IconState state, IconType type) {
            this.file = file;
            this.state = state;
            this.type = type;
        }
    }
}
