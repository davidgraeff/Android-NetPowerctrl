package oly.netpowerctrl.main;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.ndeftools.Message;
import org.ndeftools.MimeRecord;
import org.ndeftools.Record;
import org.ndeftools.util.activity.NfcReaderActivity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.JSONHelper;
import oly.netpowerctrl.data.onDataQueryCompleted;
import oly.netpowerctrl.device_ports.DevicePort;
import oly.netpowerctrl.devices.Device;
import oly.netpowerctrl.listen_service.ListenService;
import oly.netpowerctrl.listen_service.onServiceReady;
import oly.netpowerctrl.network.DeviceQuery;
import oly.netpowerctrl.network.onDeviceObserverResult;
import oly.netpowerctrl.network.onExecutionFinished;
import oly.netpowerctrl.scenes.EditSceneActivity;
import oly.netpowerctrl.scenes.Scene;
import oly.netpowerctrl.utils.ShowToast;

public class ExecutionActivity extends NfcReaderActivity implements onDeviceObserverResult, onExecutionFinished {
    private Scene scene = null;
    private int scene_commands = 0;
    private int scene_executed_commands = 0;
    private boolean enable_feedback;
    private String scene_uuid;
//    private boolean updateWidget = false;

    @Override
    protected void onPause() {
        ListenService.stopUseService();
        super.onPause();
    }

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
    protected void onResume() {
        super.onResume();
        AppData.useAppData();
        ListenService.useService(getApplicationContext(), false, false);
        Intent it = getIntent();
        if (it == null) {
            finish();
            return;
        }

        setVisible(false);

        // Extract name group from intent extra
        final Bundle extra = it.getExtras();

        if (extra == null ||
                (!extra.containsKey(EditSceneActivity.RESULT_ACTION_UUID) &&
                        !extra.containsKey(EditSceneActivity.RESULT_SCENE_JSON) &&
                        !extra.containsKey(EditSceneActivity.RESULT_SCENE_UUID) &&
                        scene_uuid == null)) {
            //noinspection ConstantConditions
            Toast.makeText(this, getString(R.string.error_shortcut_not_valid), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Read data from intent
        final boolean show_mainwindow = extra.getBoolean("show_mainWindow", false);
        enable_feedback = extra.getBoolean("enable_feedback", true);
        final String action_uuid = extra.getString(EditSceneActivity.RESULT_ACTION_UUID);
        final int action_command = extra.getInt(EditSceneActivity.RESULT_ACTION_COMMAND);
        final String scene_json = extra.getString(EditSceneActivity.RESULT_SCENE_JSON);
        scene_uuid = extra.getString(EditSceneActivity.RESULT_SCENE_UUID, scene_uuid);

        // The application may have be started here, we have to wait for the service to be ready
        ListenService.observersServiceReady.register(new onServiceReady() {
            @Override
            public boolean onServiceReady(ListenService service) {
                AppData.observersDataQueryCompleted.register(new onDataQueryCompleted() {

                    @Override
                    public boolean onDataQueryFinished() {
                        serviceAndDataReady(action_uuid, action_command, scene_json);
                        return false;
                    }
                });
                return false;
            }

            @Override
            public void onServiceFinished() {
                finish();
            }
        });

        // Show main window
        if (show_mainwindow) {
            Intent mainIt = new Intent(this, MainActivity.class);
            startActivity(mainIt);
        }
    }

    void serviceAndDataReady(String action_uuid, int action_command, String scene_json) {
        // Execute single action (in contrast to scene)
        if (action_uuid != null) {
            executeSingleAction(action_uuid, action_command);
        } else {
            if (scene_uuid != null) {
                Log.e("serviceAndDataReady", UUID.fromString(scene_uuid).toString());
                scene = AppData.getInstance().sceneCollection.get(UUID.fromString(scene_uuid));
            } else if (scene_json != null) {
                // Extract scene from extra bundle
                try {
                    scene = new Scene();
                    scene.load(JSONHelper.getReader(scene_json));
                } catch (IOException | ClassNotFoundException ignored) {
                    scene = null;
                }
            }
            executeScene();
        }
    }

    @Override
    protected void onNfcStateChange(boolean enabled) {

    }

    void executeSingleAction(String port_uuid_string, final int command) {
        final UUID port_uuid = UUID.fromString(port_uuid_string);
        final DevicePort port = AppData.getInstance().findDevicePort(port_uuid);
        if (port == null) {
            Toast.makeText(this, getString(R.string.error_shortcut_not_valid), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        AppData.getInstance().execute(port, command, ExecutionActivity.this);
    }

    void executeScene() {
        if (scene == null) {
            Toast.makeText(this, getString(R.string.error_shortcut_not_valid), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // DeviceQuery for scene devices
        TreeSet<Device> devices = new TreeSet<>();
        scene_commands = scene.getDevices(devices);
        new DeviceQuery(this, ExecutionActivity.this, devices.iterator());

    }

    @Override
    public void onExecutionFinished(int commands) {
        scene_executed_commands += commands;
        if (scene_executed_commands >= scene_commands)
            finish();
    }

    @Override
    public void onObserverDeviceUpdated(Device di) {
    }

    @Override
    public void onObserverJobFinished(List<Device> timeout_devices) {
        for (Device di : timeout_devices) {
            Toast.makeText(this, getString(R.string.error_timeout_device, di.DeviceName), Toast.LENGTH_SHORT).show();
        }

        if (enable_feedback) {
            //noinspection ConstantConditions
            ShowToast.showToast(this,
                    this.getString(R.string.scene_executed, scene.sceneName), 800);
        }
        AppData.getInstance().execute(scene, this);
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