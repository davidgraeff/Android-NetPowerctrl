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
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.onDataQueryCompleted;
import oly.netpowerctrl.device_base.data.JSONHelper;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.device_base.executables.Executable;
import oly.netpowerctrl.network.onExecutionFinished;
import oly.netpowerctrl.pluginservice.PluginService;
import oly.netpowerctrl.pluginservice.onServiceReady;
import oly.netpowerctrl.scenes.Scene;

/**
 * Will be started on NFC contact, homescreen scene execution, alarm timeout
 */
public class ExecutionActivity extends NfcReaderActivity implements onExecutionFinished {
    public static final String EXECUTE_ACTION_UUID = "action_uuid";
    public static final String EXECUTE_ACTION_COMMAND = "action_command";
    public static final String EXECUTE_SCENE_JSON = "scene";
    private String destination_uuid;

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
        boolean enable_feedback = extra.getBoolean("enable_feedback", true);

        // wait for first data to be loaded
        AppData.observersDataQueryCompleted.reset();
        AppData.observersDataQueryCompleted.register(new onDataQueryCompleted() {

            @Override
            public boolean onDataQueryFinished(AppData appData, boolean networkDevicesNotReachable) {
                final int action_command = extra.getInt(EXECUTE_ACTION_COMMAND);

                // Read data from intent
                destination_uuid = extra.getString(EXECUTE_ACTION_UUID, destination_uuid);
                String scene_json = extra.getString(EXECUTE_SCENE_JSON);
                if (scene_json != null) {
                    try {
                        appData.execute(Scene.loadFromJson(JSONHelper.getReader(scene_json)), ExecutionActivity.this);
                    } catch (IOException | ClassNotFoundException ignored) {
                    }
                } else
                    executeSingleAction(appData, destination_uuid, action_command);

                return false;
            }
        });

        PluginService.observersServiceReady.register(new onServiceReady() {
            @Override
            public boolean onServiceReady(PluginService service) {
                service.getAppData().refreshDeviceData(service, true);
                return false;
            }

            @Override
            public void onServiceFinished(PluginService service) {

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

    void executeSingleAction(AppData appData, String executable_uid, final int command) {
        final Executable executable = appData.findExecutable(executable_uid);
        if (executable == null) {
            Toast.makeText(this, getString(R.string.error_shortcut_not_valid), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (executable instanceof DevicePort)
            appData.execute((DevicePort) executable, command, ExecutionActivity.this);
        else if (executable instanceof Scene) {
            appData.execute((Scene) executable, ExecutionActivity.this);
        }
    }

    @Override
    public void onExecutionProgress(int success, int errors, int all) {
        if (success >= all)
            finish();
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