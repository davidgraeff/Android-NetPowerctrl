package oly.netpowerctrl.utils;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Content provider for showing the applications images for picking
 */
public class AssetContentProvider extends ContentProvider implements ContentProvider.PipeDataWriter<InputStream> {
    @Override
    public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
        // Try to open an asset with the given name.
        try {
            InputStream is = getContext().getAssets().open(uri.getPath());
            // Start a new thread that pipes the stream data back to the caller.
            return new AssetFileDescriptor(
                    openPipeHelper(uri, null, null, is, this), 0,
                    AssetFileDescriptor.UNKNOWN_LENGTH);
        } catch (IOException e) {
            FileNotFoundException fnf = new FileNotFoundException("Unable to open " + uri);
            throw fnf;
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return "image/*";
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
        String[] list_of_icon_paths;
        final String assetSet = "widget_icons";
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

    @Override
    public void writeDataToPipe(ParcelFileDescriptor output, Uri uri, String mimeType,
                                Bundle opts, InputStream args) {
        // Transfer data from the asset to the pipe the client is reading.
        byte[] buffer = new byte[8192];
        int n;
        FileOutputStream fout = new FileOutputStream(output.getFileDescriptor());
        try {
            while ((n = args.read(buffer)) >= 0) {
                fout.write(buffer, 0, n);
            }
        } catch (IOException e) {
            Log.i("AssetContentProvider", "Failed transferring", e);
        } finally {
            try {
                args.close();
            } catch (IOException ignored) {
            }
            try {
                fout.close();
            } catch (IOException ignored) {
            }
        }
    }
}