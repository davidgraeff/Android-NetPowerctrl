package oly.netpowerctrl.backup.drive;


import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Contents;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import oly.netpowerctrl.data.RuntimeDataController;
import oly.netpowerctrl.network.Utils;
import oly.netpowerctrl.utils.Icons;

class GDriveCreateBackupTask extends AsyncTask<Void, String, Boolean> {
    private static final String TAG = "GDriveCreateBackupTask";
    private final GoogleApiClient mClient;
    private final GDrive.GDriveConnectionState observer;
    private final BackupDoneSuccess backupDoneSuccess;
    private final Context context;

    public GDriveCreateBackupTask(Context context, GoogleApiClient client,
                                  GDrive.GDriveConnectionState observer,
                                  BackupDoneSuccess backupDoneSuccess) {
        this.context = context;
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
        RuntimeDataController c = RuntimeDataController.getDataController();

        // Enter dir
        DriveFolder PluginServiceDir = GDrive.getAppFolder(mClient);

        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle(Utils.getDeviceName() + " " + Utils.getDateTime(context)).build();
        DriveFolder.DriveFolderResult result = PluginServiceDir.createFolder(mClient, changeSet).await(3, TimeUnit.SECONDS);
        if (!result.getStatus().isSuccess()) {
            // We failed, stop the task and return.
            return false;
        }
        DriveFolder target = result.getDriveFolder();

        boolean s;
        s = createFile(c.sceneCollection.toJSON(), "scenes.json", target, "text/plain");
        s &= createFile(c.groupCollection.toJSON(), "groups.json", target, "text/plain");
        s &= createFile(c.deviceCollection.toJSON(), "devices.json", target, "text/plain");

        // Create icons folder
        changeSet = new MetadataChangeSet.Builder().setTitle("icons").build();
        result = target.createFolder(mClient, changeSet).await(3, TimeUnit.SECONDS);

        if (!result.getStatus().isSuccess()) {
            // We failed, stop the task and return.
            return false;
        }

        DriveFolder iconsFolder = result.getDriveFolder();

        Icons.IconFile[] icons = Icons.getAllIcons(context);
        for (Icons.IconFile f : icons) {
            try {
                // Open sub folder
                String relativePath = f.type.name() + f.state.name();
                DriveId subFolderID = GDrive.findChild(mClient, relativePath, iconsFolder);
                DriveFolder subFolder;
                if (subFolderID == null) { // not existing, create it
                    changeSet = new MetadataChangeSet.Builder().setTitle(relativePath).build();
                    result = iconsFolder.createFolder(mClient, changeSet).await(3, TimeUnit.SECONDS);

                    if (!result.getStatus().isSuccess()) {
                        // We failed, continue with the next file
                        Log.e(TAG, "Failed to create sub folder " + relativePath);
                        continue;
                    }
                    subFolder = result.getDriveFolder();
                } else
                    subFolder = Drive.DriveApi.getFolder(mClient, subFolderID);

                FileInputStream stream = new FileInputStream(f.file);
                createFile(stream, f.file.getName(), subFolder, f.file.getName().endsWith("png") ? "image/png" : "image/jpeg");
            } catch (FileNotFoundException ignored) {
            }
        }


        return s;
    }

    boolean createFile(String content, String filename, DriveFolder target, String mimetype) {
        return createFile(new ByteArrayInputStream(content.getBytes(Charset.defaultCharset())), filename, target, mimetype);
    }

    boolean createFile(InputStream input, String filename, DriveFolder target, String mimetype) {
        // New content
        DriveApi.ContentsResult contentsResult =
                Drive.DriveApi.newContents(mClient).await(3, TimeUnit.SECONDS);
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
                .setMimeType(mimetype).build();

        // Create the file
        DriveFolder.DriveFileResult fileResult = target.createFile(
                mClient, originalMetadata, originalContents).await(3, TimeUnit.SECONDS);
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

    public interface BackupDoneSuccess {
        void done();
    }
}