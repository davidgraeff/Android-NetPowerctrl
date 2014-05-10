package oly.netpowerctrl.transfer;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.plus.Plus;

import oly.netpowerctrl.R;
import oly.netpowerctrl.preferences.SharedPrefs;

/**
 * Drive connection
 */
public class GDrive implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    /**
     * Request code for auto Google Play Services error resolution.
     */
    protected static final int REQUEST_CODE_RESOLUTION = 1001;
    private static final String TAG = "GooglePlayServicesActivity";
    private static final String KEY_IN_RESOLUTION = "is_in_resolution";
    GDriveConnectionState observer;

    private boolean error = false;
    private String errorMessage;
    /**
     * Google API client.
     */
    private GoogleApiClient mGoogleApiClient; // Google API service
    /**
     * Determines if the client is in a resolution state, and
     * waiting for resolution intent to return.
     */
    private boolean mIsInResolution;
    private Activity context;

    public boolean isError() {
        return error;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void getListOfBackups(GDriveBackupsAdapter GDriveBackupsAdapter) {
        if (!mGoogleApiClient.isConnected())
            return;

        new GDriveRefreshBackupListTask(mGoogleApiClient, observer, GDriveBackupsAdapter).execute();
    }

    public void deleteBackup(Metadata item) {
        Toast.makeText(context, R.string.gDrive_remove_not_supported, Toast.LENGTH_SHORT).show();
    }

    public void createNewBackup(GDriveCreateBackupTask.BackupDoneSuccess done) {
        new GDriveCreateBackupTask(mGoogleApiClient, observer, done).execute();
    }

    public void restoreBackup(DriveId drive_id) {
        new GDriveRestoreBackupTask(mGoogleApiClient, observer, drive_id).execute();
    }

    public void setObserver(GDriveConnectionState observer) {
        this.observer = observer;
    }

    /**
     * Called when the activity is starting. Restores the activity state.
     */
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mIsInResolution = savedInstanceState.getBoolean(KEY_IN_RESOLUTION, false);
        }
    }

    /**
     * Called when the Activity is made visible.
     * A connection to Play Services need to be initiated as
     * soon as the activity is visible. Registers {@code ConnectionCallbacks}
     * and {@code OnConnectionFailedListener} on the
     * activities itself.
     */
    public void onStart(Activity context) {
        error = false;
        if (!SharedPrefs.gDriveEnabled())
            return;

        this.context = context;
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(Drive.API)
                    .addApi(Plus.API)
                    .addScope(Drive.SCOPE_APPFOLDER)
                            // Optionally, add additional APIs and scopes if required.
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        if (status != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(status)) {
                GooglePlayServicesUtil.getErrorDialog(status, context,
                        REQUEST_CODE_RESOLUTION).show();
            } else {
                Toast.makeText(context, "This device is not supported.",
                        Toast.LENGTH_LONG).show();
            }
        } else if (!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();
    }

    /**
     * Called when activity gets invisible. Connection to Play Services needs to
     * be disconnected as soon as an activity is invisible.
     */
    public void onStop() {
        error = false;
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        if (observer != null)
            observer.gDriveConnected(false, false);
        context = null;
    }

    public void resetAccount() {
        Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
        onStop();
    }

    /**
     * Saves the resolution state.
     */
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_IN_RESOLUTION, mIsInResolution);
    }

    /**
     * Handles Google Play Services resolution callbacks.
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_RESOLUTION:
                if (resultCode == Activity.RESULT_CANCELED) {
                    mIsInResolution = false;
                    error = true;
                    errorMessage = "Login erforderlich";
                    if (observer != null)
                        observer.gDriveConnected(false, true);
                } else if (!mGoogleApiClient.isConnected())
                    retryConnecting();
                else if (observer != null)
                    observer.gDriveConnected(true, false);

                break;
        }
    }

    private void retryConnecting() {
        mIsInResolution = false;
        if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    /**
     * Called when {@code mGoogleApiClient} is connected.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
//        Log.w(TAG, "GoogleApiClient connected");
        error = false;
        if (observer != null)
            observer.gDriveConnected(true, false);
    }

    /**
     * Called when {@code mGoogleApiClient} connection is suspended.
     */
    @Override
    public void onConnectionSuspended(int cause) {
//        Log.w(TAG, "GoogleApiClient connection suspended");
        retryConnecting();
    }

    /**
     * Called when {@code mGoogleApiClient} is trying to connect but failed.
     * Handle {@code result.getResolution()} if there is a resolution
     * available.
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
//        Log.w(TAG, "GoogleApiClient connection failed: " + result.toString());

        error = true;

        if (context == null) {
            errorMessage = "Kontext nicht geladen";
            return;
        }

        if (!result.hasResolution()) {

            errorMessage = GooglePlayServicesUtil.getErrorString(result.getErrorCode());

            if (observer != null)
                observer.gDriveConnected(false, false);

            // Show a localized error dialog.
            GooglePlayServicesUtil.getErrorDialog(
                    result.getErrorCode(), context, 0, new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            retryConnecting();
                        }
                    }
            ).show();
            return;
        }
        // If there is an existing resolution error being displayed or a resolution
        // activity has started before, do nothing and wait for resolution
        // progress to be completed.
        if (mIsInResolution) {
            return;
        }
        mIsInResolution = true;

        errorMessage = GooglePlayServicesUtil.getErrorString(result.getErrorCode());
        try {
            result.startResolutionForResult(context, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
            retryConnecting();
        }
    }

    public boolean isConnected() {
        return mGoogleApiClient != null && mGoogleApiClient.isConnected();
    }

    public interface GDriveConnectionState {
        void gDriveConnected(boolean connected, boolean canceled);

        void showProgress(boolean inProgress, String text);
    }
}