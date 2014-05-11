package oly.netpowerctrl.transfer;


import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Contents;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filter;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;

import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.application_state.RuntimeDataController;
import oly.netpowerctrl.network.Utils;
import oly.netpowerctrl.utils.Icons;

class GDriveCreateBackupTask extends AsyncTask<Void, String, Boolean> {
    private static final String TAG = "GDriveCreateBackupTask";

    public interface BackupDoneSuccess {
        void done();
    }

    private final GoogleApiClient mClient;
    private final GDrive.GDriveConnectionState observer;
    private final BackupDoneSuccess backupDoneSuccess;

    public GDriveCreateBackupTask(GoogleApiClient client,
                                  GDrive.GDriveConnectionState observer,
                                  BackupDoneSuccess backupDoneSuccess) {
        mClient = client;
        this.observer = observer;
        this.backupDoneSuccess = backupDoneSuccess;
    }

    @Override
    protected void onPreExecute() {
        if (observer != null)
            observer.showProgress(true, "Creating backup...");
    }

    @Override
    protected void onProgressUpdate(String... values) {
        if (observer != null)
            observer.showProgress(true, values[0]);
    }

    @Override
    protected Boolean doInBackground(Void... params) {

        RuntimeDataController c = NetpowerctrlApplication.getDataController();

        // Create folder
        DriveFolder appDataDir = Drive.DriveApi.getAppFolder(mClient);
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle(Utils.getDeviceName() + " " + Utils.getDateTime()).build();
        DriveFolder.DriveFolderResult result = appDataDir.createFolder(mClient, changeSet).await();
        if (!result.getStatus().isSuccess()) {
            // We failed, stop the task and return.
            return false;
        }
        DriveFolder target = result.getDriveFolder();

        boolean s;
        s = createFile(c.sceneCollection.toJSON(), "scenes.json", target);
        s &= createFile(c.groupCollection.toJSON(), "groups.json", target);
        s &= createFile(c.deviceCollection.toJSON(), "devices.json", target);

        // Create icons folder
        changeSet = new MetadataChangeSet.Builder().setTitle("icons").build();
        result = target.createFolder(mClient, changeSet).await();

        if (!result.getStatus().isSuccess()) {
            // We failed, stop the task and return.
            return false;
        }

        DriveFolder iconsFolder = result.getDriveFolder();

        Icons.IconFile[] icons = Icons.getAllIcons();
        for (Icons.IconFile f : icons) {
            try {
                // Open sub folder
                String relativePath = f.type.name() + f.state.name();
                DriveId subFolderID = findAll(relativePath, iconsFolder);
                DriveFolder subFolder;
                if (subFolderID == null) { // not existing, create it
                    changeSet = new MetadataChangeSet.Builder().setTitle(relativePath).build();
                    result = iconsFolder.createFolder(mClient, changeSet).await();

                    if (!result.getStatus().isSuccess()) {
                        // We failed, continue with the next file
                        Log.e(TAG, "Failed to create sub folder " + relativePath);
                        continue;
                    }
                    subFolder = result.getDriveFolder();
                } else
                    subFolder = Drive.DriveApi.getFolder(mClient, subFolderID);

                FileInputStream stream = new FileInputStream(f.file);
                createFile(stream, f.file.getName(), subFolder);
            } catch (FileNotFoundException ignored) {
            }
        }


        return s;
    }

    DriveId findAll(String title, DriveFolder baseFolder) {
        ArrayList<Filter> fltrs = new ArrayList<>();
        fltrs.add(Filters.eq(SearchableField.TRASHED, false));
        if (title != null) fltrs.add(Filters.eq(SearchableField.TITLE, title));
        Query qry = new Query.Builder().addFilter(Filters.and(fltrs)).build();
        DriveApi.MetadataBufferResult rslt = (baseFolder == null) ?
                Drive.DriveApi.query(mClient, qry).await() :
                baseFolder.queryChildren(mClient, qry).await();
        if (rslt.getStatus().isSuccess()) {
            MetadataBuffer mdb = null;
            try {
                mdb = rslt.getMetadataBuffer();
                if (mdb != null) {
                    for (Metadata md : mdb) {
                        if (md == null) continue;
                        return md.getDriveId();      // here is the "Drive ID"
                    }
                }
            } finally {
                if (mdb != null) mdb.close();
            }
        }
        return null;
    }

    boolean createFile(String content, String filename, DriveFolder target) {
        return createFile(new ByteArrayInputStream(content.getBytes(Charset.defaultCharset())), filename, target);
    }

    boolean createFile(InputStream input, String filename, DriveFolder target) {
        // New content
        DriveApi.ContentsResult contentsResult =
                Drive.DriveApi.newContents(mClient).await();
        if (!contentsResult.getStatus().isSuccess()) {
            // We failed, stop the task and return.
            return false;
        }

        // Write content
        Contents originalContents = contentsResult.getContents();
        OutputStream os = originalContents.getOutputStream();
        try {
            int read;
            byte[] bytes = new byte[1024];

            while ((read = input.read(bytes)) != -1) {
                os.write(bytes, 0, read);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        // Create the metadata
        MetadataChangeSet originalMetadata = new MetadataChangeSet.Builder()
                .setTitle(filename)
                .setMimeType("text/plain").build();

        // Create the file
        DriveFolder.DriveFileResult fileResult = target.createFile(
                mClient, originalMetadata, originalContents).await();
        return fileResult.getStatus().isSuccess();
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (result) {
            if (observer != null)
                observer.showProgress(false, "Backup created");
            if (backupDoneSuccess != null)
                backupDoneSuccess.done();
        } else {
            if (observer != null)
                observer.showProgress(false, "Backup failed");
        }
    }
}