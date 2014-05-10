package oly.netpowerctrl.transfer;


import android.os.AsyncTask;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Contents;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.MetadataChangeSet;

import java.io.IOException;
import java.io.OutputStream;

import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.application_state.RuntimeDataController;
import oly.netpowerctrl.network.Utils;

public class GDriveCreateBackupTask extends AsyncTask<Void, String, Boolean> {
    public interface BackupDoneSuccess {
        void done();
    }

    private GoogleApiClient mClient;
    private GDrive.GDriveConnectionState observer;
    private BackupDoneSuccess backupDoneSuccess;

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

        return s;
    }

    boolean createFile(String content, String filename, DriveFolder target) {
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
            os.write(content.getBytes());
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