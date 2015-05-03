package oly.netpowerctrl.backup.drive;


import android.os.AsyncTask;

import com.google.android.gms.cast.CastStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.MetadataBuffer;

import java.util.concurrent.TimeUnit;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.NetpowerctrlApplication;

class GDriveRefreshBackupListTask extends AsyncTask<Void, String, MetadataBuffer> {
    private final GoogleApiClient mClient;
    private final GDrive.GDriveConnectionState observer;
    private final GDriveBackupsAdapter GDriveBackupsAdapter;
    private String errorString;

    public GDriveRefreshBackupListTask(GoogleApiClient client,
                                       GDrive.GDriveConnectionState observer,
                                       GDriveBackupsAdapter GDriveBackupsAdapter) {
        mClient = client;
        this.observer = observer;
        this.GDriveBackupsAdapter = GDriveBackupsAdapter;
    }

    @Override
    protected void onPreExecute() {
        if (observer != null)
            observer.showProgress(true, NetpowerctrlApplication.getAppString(R.string.gDrive_refreshing_backup_list));
    }

    @Override
    protected void onProgressUpdate(String... values) {
        if (observer != null)
            observer.showProgress(true, values[0]);
    }

    @Override
    protected MetadataBuffer doInBackground(Void... params) {
        // Check connection
        if (!mClient.isConnected()) {
            errorString = NetpowerctrlApplication.getAppString(R.string.gDrive_error_lost_connection);
            return null;
        }

        try {
            // Request sync
            com.google.android.gms.common.api.Status resultRequestSync;
            resultRequestSync = Drive.DriveApi.requestSync(mClient).await(5, TimeUnit.SECONDS);
            if (!resultRequestSync.getStatus().isSuccess()) {
                if (resultRequestSync.getStatusCode() == CastStatusCodes.TIMEOUT) {
                    errorString = NetpowerctrlApplication.getAppString(R.string.gDrive_error_timeout);
                } else
                    errorString = NetpowerctrlApplication.getAppString(R.string.gDrive_error_retrieve_files,
                            resultRequestSync.getStatus().toString());
                // We failed, stop the task and return.
                return null;
            }

            // Enter dir
            DriveFolder PluginServiceDir = GDrive.getAppFolder(mClient);

            // Get childs
            DriveApi.MetadataBufferResult result = PluginServiceDir.listChildren(mClient).await(5, TimeUnit.SECONDS);
            if (!result.getStatus().isSuccess()) {
                if (resultRequestSync.getStatusCode() == CastStatusCodes.TIMEOUT) {
                    errorString = NetpowerctrlApplication.getAppString(R.string.gDrive_error_timeout);
                } else
                    errorString = NetpowerctrlApplication.getAppString(R.string.gDrive_error_retrieve_files, result.getStatus().toString());
                // We failed, stop the task and return.
                return null;
            }

            return result.getMetadataBuffer();
        } catch (Exception e) {
            errorString = e.getMessage();
        }

        return null;
    }

    @Override
    protected void onPostExecute(MetadataBuffer metadataBuffer) {
        if (metadataBuffer == null) {
            if (observer != null)
                observer.showProgress(false, errorString);
            return;
        }

        GDriveBackupsAdapter.clear();
        GDriveBackupsAdapter.append(metadataBuffer);

        if (observer != null)
            observer.showProgress(false, NetpowerctrlApplication.getAppString(R.string.gDriveConnected));
    }
}