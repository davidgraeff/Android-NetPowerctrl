package oly.netpowerctrl.backup.drive;


import android.os.AsyncTask;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Metadata;

import oly.netpowerctrl.application_state.RuntimeDataController;

class GDriveRemoveTask extends AsyncTask<Void, String, Boolean> {
    private static final String TAG = "GDriveCreateBackupTask";
    private final GoogleApiClient mClient;
    private final GDrive.GDriveConnectionState observer;
    private final DoneSuccess doneSuccess;
    private final Metadata item;

    public GDriveRemoveTask(GoogleApiClient client,
                            GDrive.GDriveConnectionState observer,
                            DoneSuccess doneSuccess,
                            Metadata item) {
        mClient = client;
        this.observer = observer;
        this.doneSuccess = doneSuccess;
        this.item = item;
    }

    @Override
    protected void onPreExecute() {
        if (observer != null)
            observer.showProgress(true, "Try removing...");
    }

    @Override
    protected void onProgressUpdate(String... values) {
        if (observer != null)
            observer.showProgress(true, values[0]);
    }

    @Override
    protected Boolean doInBackground(Void... params) {

        RuntimeDataController c = RuntimeDataController.getDataController();

//        DriveFile file = Drive.DriveApi.getFile(mClient, item.getDriveId());
//        DriveFolder.DriveFolderResult result = file.trash(mClient).await();
//        if (!result.getStatus().isSuccess()) {
//            // We failed, stop the task and return.
//            return false;
//        }

        return false;

//        return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (observer != null)
            observer.showProgress(false, "No delete support!");
        if (result) {
            if (observer != null)
                observer.showProgress(false, "Delete successful");
            if (doneSuccess != null)
                doneSuccess.done();
        }
    }

    public interface DoneSuccess {
        void done();
    }
}