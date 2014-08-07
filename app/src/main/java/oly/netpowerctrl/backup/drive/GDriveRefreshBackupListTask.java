package oly.netpowerctrl.backup.drive;


import android.content.Context;
import android.os.AsyncTask;

import com.google.android.gms.cast.CastStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.MetadataBuffer;

import java.util.concurrent.TimeUnit;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;

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
            observer.showProgress(true, NetpowerctrlApplication.instance.getString(R.string.gDrive_refreshing_backup_list));
    }

    @Override
    protected void onProgressUpdate(String... values) {
        if (observer != null)
            observer.showProgress(true, values[0]);
    }

    @Override
    protected MetadataBuffer doInBackground(Void... params) {
        Context context = NetpowerctrlApplication.instance;

        // Request sync
        com.google.android.gms.common.api.Status resultRequestSync;
        resultRequestSync = Drive.DriveApi.requestSync(mClient).await(5, TimeUnit.SECONDS);
        if (!resultRequestSync.getStatus().isSuccess()) {
            if (resultRequestSync.getStatusCode() == CastStatusCodes.TIMEOUT) {
                errorString = context.getString(R.string.gDrive_error_timeout);
            } else
                errorString = context.getString(R.string.gDrive_error_retrieve_files,
                        resultRequestSync.getStatus().toString());
            // We failed, stop the task and return.
            return null;
        }

        // Enter dir
        DriveFolder appDataDir = GDrive.getAppFolder(mClient);

        // Get childs
        DriveApi.MetadataBufferResult result = appDataDir.listChildren(mClient).await(5, TimeUnit.SECONDS);
        if (!result.getStatus().isSuccess()) {
            if (resultRequestSync.getStatusCode() == CastStatusCodes.TIMEOUT) {
                errorString = context.getString(R.string.gDrive_error_timeout);
            } else
                errorString = context.getString(R.string.gDrive_error_retrieve_files, result.getStatus().toString());
            // We failed, stop the task and return.
            return null;
        }

        return result.getMetadataBuffer();
    }

    @Override
    protected void onPostExecute(MetadataBuffer metadataBuffer) {
        Context context = NetpowerctrlApplication.instance;
        if (metadataBuffer == null) {
            if (observer != null)
                observer.showProgress(false, errorString);
            return;
        }

        GDriveBackupsAdapter.clear();
        GDriveBackupsAdapter.append(metadataBuffer);

        if (observer != null)
            observer.showProgress(false, context.getString(R.string.gDriveConnected));
    }
}