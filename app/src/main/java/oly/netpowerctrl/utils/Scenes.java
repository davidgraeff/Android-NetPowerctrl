package oly.netpowerctrl.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.EditText;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.datastructure.SceneOutlet;
import oly.netpowerctrl.listadapter.OutletsExecuteAdapter;
import oly.netpowerctrl.main.NetpowerctrlActivity;

/**
 * Util for scenes
 */
public class Scenes {
    public static void createScene(final Context context, final OutletsExecuteAdapter adapter) {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);

        alert.setTitle(context.getString(R.string.outlet_to_scene_title));
        alert.setMessage(context.getString(R.string.outlet_to_scene_message));

        final EditText input = new EditText(alert.getContext());
        input.setText("");
        alert.setView(input);

        alert.setPositiveButton(context.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Scene scene = new Scene();
                scene.sceneName = input.getText().toString();
                if (scene.sceneName.trim().isEmpty())
                    return;
                for (int i = 0; i < adapter.getCount(); ++i) {
                    scene.add(SceneOutlet.fromOutletInfo(adapter.getItem(i), true));
                }
                NetpowerctrlActivity.instance.getScenesAdapter().addScene(context, scene);
            }
        });

        alert.setNegativeButton(context.getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        alert.show();
    }

    public interface SceneNameChanged {
        void sceneNameChanged();
    }

    public static void requestName(Context context, final Scene scene, final SceneNameChanged callback) {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);

        alert.setTitle(context.getString(R.string.outlet_to_scene_title));
        alert.setMessage(context.getString(R.string.scene_set_name));

        final EditText input = new EditText(alert.getContext());
        input.setText(scene.sceneName);
        alert.setView(input);

        alert.setPositiveButton(context.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String name = input.getText().toString();
                if (name.trim().isEmpty())
                    return;
                scene.sceneName = name;
                callback.sceneNameChanged();
            }
        });

        alert.setNegativeButton(context.getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        alert.show();
    }

    public static BitmapDrawable getDrawableFromUri(Context context, Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                context.getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return new BitmapDrawable(context.getResources(), image);
    }

    public static void saveIcon(Context context, Scene scene, Bitmap bitmap) {
        String root = context.getExternalFilesDir("scene_icons").toString();
        File myDir = new File(root);

        String fname = scene.uuid.toString();
        File file;

        file = new File(myDir, fname + ".jpg");
        if (file.exists()) file.delete();
        file = new File(myDir, fname + ".png");
        if (file.exists()) file.delete();

        if (bitmap == null) {
            return;

        }
        if (bitmap.hasAlpha()) {
            fname += ".png";
        } else {
            fname += ".jpg";
        }

        try {
            FileOutputStream out = new FileOutputStream(new File(myDir, fname));
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

    public static Bitmap getResizedBitmapIconSize(Context context, Bitmap bm) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        int size = (int) context.getResources().getDimension(android.R.dimen.app_icon_size);
        Matrix matrix = new Matrix();
        matrix.postScale(size / width, size / height);
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }

    public static Bitmap getResizedBitmap(Context context, Bitmap bm, int newHeightDP, int newWidthDP) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        DisplayMetrics m = context.getResources().getDisplayMetrics();
        float scaleWidth = (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newWidthDP, m)) / width;
        float scaleHeight = (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newHeightDP, m)) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }

    public static Bitmap loadIcon(Context context, Scene scene) {
        String root = context.getExternalFilesDir("scene_icons").toString();
        File myDir = new File(root);

        File file = new File(myDir, scene.uuid.toString() + ".png");
        if (!file.exists())
            file = new File(myDir, scene.uuid.toString() + ".jpg");
        if (!file.exists()) return null;
        return BitmapFactory.decodeFile(file.getAbsolutePath());
    }
}
