package oly.netpowerctrl.transfer;


import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.MetadataBuffer;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;

public class GDriveRefreshBackupListTask extends AsyncTask<Void, String, MetadataBuffer> {
    private GoogleApiClient mClient;
    private GDrive.GDriveConnectionState observer;
    private GDriveBackupsAdapter GDriveBackupsAdapter;

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
            observer.showProgress(true, "Refreshing backup list...");
    }

    @Override
    protected void onProgressUpdate(String... values) {
        if (observer != null)
            observer.showProgress(true, values[0]);
    }

    @Override
    protected MetadataBuffer doInBackground(Void... params) {
        Drive.DriveApi.requestSync(mClient).await();

        DriveFolder appDataDir = Drive.DriveApi.getAppFolder(mClient);
        DriveApi.MetadataBufferResult result = appDataDir.listChildren(mClient).await();
        if (!result.getStatus().isSuccess()) {
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
                observer.showProgress(false, context.getString(R.string.neighbours_error_retrieve_files));
            return;
        }

        Log.w("debug", "folders:" + String.valueOf(metadataBuffer.getCount()));
        GDriveBackupsAdapter.clear();
        GDriveBackupsAdapter.append(metadataBuffer);

        if (observer != null)
            observer.showProgress(false, context.getString(R.string.gDriveConnected));
    }
}