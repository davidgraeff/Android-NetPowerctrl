package oly.netpowerctrl.main;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import org.ndeftools.Message;
import org.ndeftools.MimeRecord;
import org.ndeftools.Record;
import org.ndeftools.util.activity.NfcReaderActivity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.data.onServiceReady;
import oly.netpowerctrl.data.query.onDataQueryCompleted;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.executables.ExecutableFabric;
import oly.netpowerctrl.network.onExecutionFinished;
import oly.netpowerctrl.utils.JSONHelper;

;

/**
 * Will be started on NFC contact, homescreen scene execution, alarm timeout
 */
public class ExecutionActivity extends NfcReaderActivity {
    public static final String EXECUTE_ACTION_UUID = "action_uuid";
    public static final String EXECUTE_ACTION_COMMAND = "action_command";
    public static final String EXECUTE_SCENE_JSON = "scene";
    private String destination_uuid;

    private onExecutionFinished executionFinished = new onExecutionFinished(0) {
        @Override
        public void onExecutionProgress() {
            if (success + errors >= expected)
                finish();
        }
    };

    @Override
    protected void onNfcFeatureNotFound() {

    }

    @Override
    protected void onNfcStateEnabled() {

    }

    @Override
    protected void onNfcStateDisabled() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setVisible(false);

        Intent it = getIntent();
        if (it == null) {
            finish();
            return;
        }

        // Extract name group from intent extra
        final Bundle extra = it.getExtras();
        if (extra == null) {
            Toast.makeText(this, getString(R.string.error_shortcut_not_valid), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!extra.containsKey(EXECUTE_ACTION_UUID) &&
                !extra.containsKey(EXECUTE_SCENE_JSON) && destination_uuid == null) {
            finish();
            return;
        }

        boolean show_mainwindow = extra.getBoolean("show_mainWindow", false);
        //boolean enable_feedback = extra.getBoolean("enable_feedback", true);

        // wait for first data to be loaded
        DataService.observersDataQueryCompleted.reset();
        DataService.observersDataQueryCompleted.register(new onDataQueryCompleted() {

            @Override
            public boolean onDataQueryFinished(DataService dataService) {
                final int action_command = extra.getInt(EXECUTE_ACTION_COMMAND);

                // Read data from intent
                destination_uuid = extra.getString(EXECUTE_ACTION_UUID, destination_uuid);
                String scene_json = extra.getString(EXECUTE_SCENE_JSON);

                if (scene_json != null) {
                    try {
                        Executable executable = new ExecutableFabric().newInstance(JSONHelper.getReader(scene_json));
                        executable.execute(dataService, action_command, executionFinished);
                    } catch (IOException | ClassNotFoundException ignored) {
                    }
                } else {
                    final Executable executable = dataService.executables.findByUID(destination_uuid);
                    executionFinished.addExpected(1);
                    if (executable == null) {
                        Toast.makeText(ExecutionActivity.this, getString(R.string.error_shortcut_not_valid), Toast.LENGTH_SHORT).show();
                        finish();
                    } else
                        executable.execute(dataService, action_command, executionFinished);
                }
                return false;
            }
        });

        DataService.observersServiceReady.register(new onServiceReady() {
            @Override
            public boolean onServiceReady(DataService service) {
                service.refreshDevices();
                return false;
            }

            @Override
            public void onServiceFinished(DataService service) {

            }
        });

        // Show main window
        if (show_mainwindow) {
            Intent mainIt = new Intent(this, MainActivity.class);
            startActivity(mainIt);
        }
    }

    @Override
    protected void onNfcStateChange(boolean enabled) {

    }

    @Override
    protected void readNdefMessage(Message message) {
        for (Record record : message) {
            if (record instanceof MimeRecord) {
                MimeRecord mimeRecord = (MimeRecord) record;
                if (mimeRecord.getMimeType() != null && mimeRecord.getMimeType().equals("application/oly.netpowerctrl")) {
                    try {
                        destination_uuid = new String(mimeRecord.getData(), "ASCII");
                    } catch (UnsupportedEncodingException ignored) {
                    }
                }
            }
        }
    }

    @Override
    protected void readEmptyNdefMessage() {

    }

    @Override
    protected void readNonNdefMessage() {

    }
}