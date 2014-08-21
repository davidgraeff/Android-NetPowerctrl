package oly.netpowerctrl.utils;

/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;
import android.provider.DocumentsProvider;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import oly.netpowerctrl.R;

/**
 * Manages documents and exposes them to the Android system for sharing.
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class AssetDocumentProvider extends DocumentsProvider implements ContentProvider.PipeDataWriter<InputStream> {
    private static final String TAG = AssetDocumentProvider.class.getSimpleName();

    // Use these as the default columns to return information about a root if no specific
    // columns are requested in a query.
    private static final String[] DEFAULT_ROOT_PROJECTION = new String[]{
            Root.COLUMN_ROOT_ID,
            Root.COLUMN_MIME_TYPES,
            Root.COLUMN_FLAGS,
            Root.COLUMN_ICON,
            Root.COLUMN_TITLE,
            Root.COLUMN_SUMMARY,
            Root.COLUMN_DOCUMENT_ID
    };

    // Use these as the default columns to return information about a document if no specific
    // columns are requested in a query.
    private static final String[] DEFAULT_DOCUMENT_PROJECTION = new String[]{
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_FLAGS,
            Document.COLUMN_SIZE
    };

    private static final String ROOT = "root";
    private static final String assetSet = "widget_icons";

    // A file object at the root of the file hierarchy.  Depending on your implementation, the root
    // does not need to be an existing file system directory.  For example, a tag-based document
    // provider might return a directory containing all tags, represented as child directories.
//    private File mBaseDir;

    /**
     * @param projection the requested root column projection
     * @return either the requested root column projection, or the default projection if the
     * requested projection is null.
     */
    private static String[] resolveRootProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_ROOT_PROJECTION;
    }

    private static String[] resolveDocumentProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION;
    }

    /**
     * Get the MIME data type of a document, given its filename.
     *
     * @param name the filename of the document
     * @return the MIME data type of a document
     */
    private static String getTypeForName(String name) {
        final int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0) {
            final String extension = name.substring(lastDot + 1);
            final String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mime != null) {
                return mime;
            }
        }
        return "application/octet-stream";
    }

    @Override
    public boolean onCreate() {
        Log.w(TAG, "onCreate");

//        mBaseDir = getContext().getFilesDir();

        return true;
    }
    // END_INCLUDE(query_document)

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        Log.w(TAG, "queryRoots");

        // Create a cursor with either the requested fields, or the default projection.  This
        // cursor is returned to the Android system picker UI and used to display all roots from
        // this provider.
        final MatrixCursor result = new MatrixCursor(resolveRootProjection(projection));

        createRowRoot(result);
        return result;
    }

    private void createRowRoot(MatrixCursor result) {
        // It's possible to have multiple roots (e.g. for multiple accounts in the same app) -
        // just add multiple cursor rows.
        // Construct one row for a root called "MyCloud".
        final MatrixCursor.RowBuilder row = result.newRow();

        row.add(Root.COLUMN_ROOT_ID, ROOT);
        row.add(Root.COLUMN_SUMMARY, getContext().getString(R.string.doc_provider_summary));

        // FLAG_SUPPORTS_CREATE means at least one directory under the root supports creating
        // documents.  FLAG_SUPPORTS_RECENTS means your application's most recently used
        // documents will show up in the "Recents" category.  FLAG_SUPPORTS_SEARCH allows users
        // to search all documents the application shares.
        //row.add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_SEARCH);

        // COLUMN_TITLE is the root title (e.g. what will be displayed to identify your provider).
        row.add(Root.COLUMN_TITLE, getContext().getString(R.string.app_name));

        // This document id must be unique within this provider and consistent across time.  The
        // system picker UI may save it and refer to it later.
        row.add(Root.COLUMN_DOCUMENT_ID, ROOT);

        // The child MIME types are used to filter the roots and only present to the user roots
        // that contain the desired type somewhere in their file hierarchy.
        row.add(Root.COLUMN_MIME_TYPES, "image/*");
        row.add(Root.COLUMN_ICON, R.drawable.netpowerctrl);
    }

    // END_INCLUDE(query_child_documents)

    // BEGIN_INCLUDE(query_document)
    @Override
    public Cursor queryDocument(String documentId, String[] projection)
            throws FileNotFoundException {
        Log.w(TAG, "queryDocument " + documentId);

        // Create a cursor with the requested projection, or the default projection.
        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        if (!documentId.equals(ROOT)) {
            try {
                createRowAssetFile(result, documentId, getContext().getAssets());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else
            createRowRoot(result);
        return result;
    }

    // BEGIN_INCLUDE(query_child_documents)
    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection,
                                      String sortOrder) throws FileNotFoundException {
        Log.w(TAG, "queryChildDocuments, parentDocumentId: " +
                parentDocumentId +
                " sortOrder: " +
                sortOrder);

        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        if (parentDocumentId.equals(ROOT)) {
            AssetManager am = getContext().getAssets();
            String[] list_of_icon_paths;
            try {
                list_of_icon_paths = am.list(assetSet);
                for (String filename : list_of_icon_paths) {
                    createRowAssetFile(result, filename, am);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


        }

        return result;
    }

    private void createRowAssetFile(MatrixCursor result, String filename, AssetManager am) throws IOException {
        int flags = 0;
        final String mimeType = getTypeForName(filename);

        if (mimeType.startsWith("image/")) {
            // Allow the image to be represented by a thumbnail rather than an icon
            flags |= Document.FLAG_SUPPORTS_THUMBNAIL;
        }

        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, filename);
        row.add(Document.COLUMN_DISPLAY_NAME, filename);
        row.add(Document.COLUMN_MIME_TYPE, mimeType);
        row.add(Document.COLUMN_SIZE, am.openFd(assetSet + "/" + filename).getLength());
        row.add(Document.COLUMN_FLAGS, flags);

        // Add a custom icon
        row.add(Document.COLUMN_ICON, R.drawable.netpowerctrl);
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(String documentId, Point sizeHint,
                                                     CancellationSignal signal)
            throws FileNotFoundException {
        Log.w(TAG, "openDocumentThumbnail");

        if (documentId.equals(ROOT))
            return null;

        AssetManager am = getContext().getAssets();
        try {
            return am.openFd(assetSet + "/" + documentId);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    // BEGIN_INCLUDE(open_document)
    @Override
    public ParcelFileDescriptor openDocument(final String documentId, final String mode,
                                             CancellationSignal signal)
            throws FileNotFoundException {
        Log.w(TAG, "openDocument, mode: " + mode);

        if (documentId.equals(ROOT))
            return null;

        AssetManager am = getContext().getAssets();
        try {
            //return am.openFd(assetSet + "/" + documentId).getParcelFileDescriptor();
            InputStream is = getContext().getAssets().open(assetSet + "/" + documentId);
            // Start a new thread that pipes the stream data back to the caller.
            return new AssetFileDescriptor(
                    openPipeHelper(null, null, null, is, this), 0,
                    AssetFileDescriptor.UNKNOWN_LENGTH).getParcelFileDescriptor();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
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