package oly.netpowerctrl.backup.drive;


import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Contents;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.application_state.RuntimeDataController;
import oly.netpowerctrl.utils.Icons;
import oly.netpowerctrl.utils.JSONHelper;

class GDriveRestoreBackupTask extends AsyncTask<Void, String, Boolean> {
    public interface BackupDoneSuccess {
        void done();
    }

    private final GoogleApiClient mClient;
    private final GDrive.GDriveConnectionState observer;
    private final DriveId driveId;

    private String scenes;
    private String groups;
    private String devices;

    /**
     * @param client   The Google API client
     * @param observer The message observer
     * @param driveId  The driveID of the folder with the backup
     */
    public GDriveRestoreBackupTask(GoogleApiClient client,
                                   GDrive.GDriveConnectionState observer,
                                   DriveId driveId) {
        mClient = client;
        this.observer = observer;
        this.driveId = driveId;
    }

    @Override
    protected void onPreExecute() {
        if (observer != null)
            observer.showProgress(true, "Restoring from backup...");
    }

    @Override
    protected void onProgressUpdate(String... values) {
        if (observer != null)
            observer.showProgress(true, values[0]);
    }

    void readRelativeDir(Icons.IconType iconType, Icons.IconState state, MetadataBuffer children) {
        for (int i = 0; i < children.getCount(); ++i) {
            Metadata d = children.get(i);
            if (!d.isFolder()) {
                DriveFile file = Drive.DriveApi.getFile(mClient, d.getDriveId());
                DriveApi.ContentsResult contentsResult =
                        file.openContents(mClient, DriveFile.MODE_READ_ONLY, null).await();
                if (!contentsResult.getStatus().isSuccess()) {
                    continue;
                }
                Contents r = contentsResult.getContents();
                Icons.saveIcon(d.getTitle(), iconType, state, r.getInputStream());
            }
        }
        children.close();
    }

    void readIconsDir(MetadataBuffer children) {
        for (int i = 0; i < children.getCount(); ++i) {
            Metadata d = children.get(i);
            if (!d.isFolder())
                continue;

            boolean found = false;
            for (Icons.IconType iconType : Icons.IconType.values()) {
                for (Icons.IconState state : Icons.IconState.values()) {
                    String relativePath = iconType.name() + state.name();
                    if (d.getTitle().equals(relativePath)) {
                        DriveFolder iconsDir = Drive.DriveApi.getFolder(mClient, d.getDriveId());
                        DriveApi.MetadataBufferResult resultIconsDir = iconsDir.listChildren(mClient).await();
                        if (!resultIconsDir.getStatus().isSuccess()) {
                            continue;
                        }
                        readRelativeDir(iconType, state, resultIconsDir.getMetadataBuffer());
                        found = true;
                        break;
                    }
                }
                if (found)
                    break;
            }
        }
        children.close();
    }

    @Override
    protected Boolean doInBackground(Void... params) {

        RuntimeDataController c = NetpowerctrlApplication.getDataController();

        // Create folder
        DriveFolder backupDir = Drive.DriveApi.getFolder(mClient, driveId);
        DriveApi.MetadataBufferResult result = backupDir.listChildren(mClient).await();
        if (!result.getStatus().isSuccess()) {
            // We failed, stop the task and return.
            return false;
        }

        MetadataBuffer b = null;
        try {
            b = result.getMetadataBuffer();
            for (int i = 0; i < b.getCount(); ++i) {
                Metadata d = b.get(i);
                if (d.getTitle().equals("scenes.json")) {
                    scenes = readFile(d);
                } else if (d.getTitle().equals("groups.json")) {
                    groups = readFile(d);
                } else if (d.getTitle().equals("devices.json")) {
                    devices = readFile(d);
                } else if (d.isFolder() && d.getTitle().equals("icons")) {
                    DriveFolder iconsDir = Drive.DriveApi.getFolder(mClient, d.getDriveId());
                    DriveApi.MetadataBufferResult resultIconsDir = iconsDir.listChildren(mClient).await();
                    if (!resultIconsDir.getStatus().isSuccess()) {
                        // We failed, stop the task and return.
                        continue;
                    }
                    readIconsDir(resultIconsDir.getMetadataBuffer());
                }
            }
        } finally {
            if (b != null) b.close();
        }

        return true;
    }

    private String readFile(Metadata d) {
        DriveFile file = Drive.DriveApi.getFile(mClient, d.getDriveId());
        DriveApi.ContentsResult contentsResult =
                file.openContents(mClient, DriveFile.MODE_READ_ONLY, null).await();
        if (!contentsResult.getStatus().isSuccess()) {
            return null;
        }
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(contentsResult.getContents().getInputStream()));
        StringBuilder builder = new StringBuilder();
        String line;
        String contents = null;
        try {
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            contents = builder.toString();
        } catch (IOException e) {
            Log.e("GDriveRestoreBackupTask", "IOException while reading from the stream", e);
        }

        file.discardContents(mClient, contentsResult.getContents()).await();
        return contents;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (result && scenes != null && groups != null && devices != null) {
            if (observer != null)
                observer.showProgress(false, "Backup restored");
            RuntimeDataController d = NetpowerctrlApplication.getDataController();
            try {
                d.deviceCollection.fromJSON(JSONHelper.getReader(devices), false);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                d.sceneCollection.fromJSON(JSONHelper.getReader(scenes), false);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                d.groupCollection.fromJSON(JSONHelper.getReader(groups), false);
            } catch (IOException e) {
                e.printStackTrace();
            }
            d.notifyStateReloaded();
        } else {
            if (observer != null)
                observer.showProgress(false, "Backup restoring failed");
        }
    }
}