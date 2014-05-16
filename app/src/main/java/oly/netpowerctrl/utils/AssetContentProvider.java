package oly.netpowerctrl.utils;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Content provider for showing the applications images for picking
 */
public class AssetContentProvider extends ContentProvider {
    @Override
    public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
        AssetManager am = getContext().getAssets();
        String file_name = uri.getLastPathSegment();
        if (file_name == null)
            throw new FileNotFoundException();
        AssetFileDescriptor afd = null;
        try {
            afd = am.openFd(file_name);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return afd;//super.openAssetFile(uri, mode);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return "image/jpeg";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (projection == null) {
            projection = new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        }

        MatrixCursor c = new MatrixCursor(new String[]{"_id", "_data"});

        AssetManager am = getContext().getAssets();
        Bitmap[] list_of_icons = null;
        String[] list_of_icon_paths = null;
        final String assetSet = "scene_icons";
        try {
            list_of_icon_paths = am.list(assetSet);
            for (String filename : list_of_icon_paths) {
                c.addRow(new Object[]{filename, am.openFd(assetSet + "/" + filename)});
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}