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
import oly.netpowerctrl.pluginservice.onPluginsReady;
import oly.netpowerctrl.scenes.EditSceneActivity;
import oly.netpowerctrl.scenes.Scene;

/**
 * Will be started on NFC contact, homescreen scene execution, alarm timeout
 */
public class ExecutionActivity extends NfcReaderActivity implements onExecutionFinished {
    private String scene_uuid;

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

        if (!extra.containsKey(EditSceneActivity.RESULT_ACTION_UUID) &&
                !extra.containsKey(EditSceneActivity.RESULT_SCENE_JSON) &&
                !extra.containsKey(EditSceneActivity.RESULT_SCENE_UUID) && scene_uuid == null) {
            finish();
            return;
        }

        boolean show_mainwindow = extra.getBoolean("show_mainWindow", false);
        boolean enable_feedback = extra.getBoolean("enable_feedback", true);

        // wait for first data to be loaded
        AppData.observersDataQueryCompleted.resetDataQueryCompleted();
        AppData.observersDataQueryCompleted.register(new onDataQueryCompleted() {

            @Override
            public boolean onDataQueryFinished(boolean networkDevicesNotReachable) {
                final int action_command = extra.getInt(EditSceneActivity.RESULT_ACTION_COMMAND);

                // Read data from intent
                String action_uuid = extra.getString(EditSceneActivity.RESULT_ACTION_UUID);
                String scene_json = extra.getString(EditSceneActivity.RESULT_SCENE_JSON);
                scene_uuid = extra.getString(EditSceneActivity.RESULT_SCENE_UUID, scene_uuid);

                if (scene_json != null) {
                    try {
                        AppData.getInstance().execute(Scene.loadFromJson(JSONHelper.getReader(scene_json)), ExecutionActivity.this);
                    } catch (IOException | ClassNotFoundException ignored) {
                    }
                } else
                    executeSingleAction(action_uuid, action_command);

                return false;
            }
        });

        PluginService.observersPluginsReady.register(new onPluginsReady() {
            @Override
            public boolean onPluginsReady() {
                AppData.getInstance().refreshDeviceData(true);
                return false;
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

    void executeSingleAction(String executable_uid, final int command) {
        final Executable executable = AppData.getInstance().findExecutable(executable_uid);
        if (executable == null) {
            Toast.makeText(this, getString(R.string.error_shortcut_not_valid), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (executable instanceof DevicePort)
            AppData.getInstance().execute((DevicePort) executable, command, ExecutionActivity.this);
        else if (executable instanceof Scene) {
            AppData.getInstance().execute((Scene) executable, ExecutionActivity.this);
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
                        scene_uuid = new String(mimeRecord.getData(), "ASCII");
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