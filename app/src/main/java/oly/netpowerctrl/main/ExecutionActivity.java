package oly.netpowerctrl.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.application_state.NetpowerctrlService;
import oly.netpowerctrl.application_state.RuntimeStateChanged;
import oly.netpowerctrl.application_state.ServiceReady;
import oly.netpowerctrl.devices.DeviceInfo;
import oly.netpowerctrl.devices.DevicePort;
import oly.netpowerctrl.network.DeviceObserverResult;
import oly.netpowerctrl.network.DeviceQuery;
import oly.netpowerctrl.network.ExecutionFinished;
import oly.netpowerctrl.scenes.EditSceneActivity;
import oly.netpowerctrl.scenes.Scene;
import oly.netpowerctrl.utils.JSONHelper;
import oly.netpowerctrl.utils.ShowToast;

public class ExecutionActivity extends Activity implements DeviceObserverResult, ExecutionFinished {
    private Scene scene = null;
    private int scene_commands = 0;
    private int scene_executed_commands = 0;
    private boolean enable_feedback;
//    private boolean updateWidget = false;

    @Override
    protected void onDestroy() {
        NetpowerctrlService.stopUseListener();
        super.onDestroy();
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NetpowerctrlService.useListener();
        Intent it = getIntent();
        if (it == null) {
            finish();
            return;
        }

        setVisible(false);

        // Extract name group from intent extra
        final Bundle extra = it.getExtras();

        if (extra == null) {
            //noinspection ConstantConditions
            Toast.makeText(this, getString(R.string.error_shortcut_not_valid), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // The application may have be started here, we have to wait for the service to be ready
        NetpowerctrlService.registerServiceReadyObserver(new ServiceReady() {
            @Override
            public boolean onServiceReady(NetpowerctrlService service) {
                // Execute single action (in contrast to scene)
                if (extra.containsKey(EditSceneActivity.RESULT_ACTION_UUID)) {
                    executeSingleAction(extra.getString(EditSceneActivity.RESULT_ACTION_UUID),
                            extra.getInt(EditSceneActivity.RESULT_ACTION_COMMAND));
                } else {
                    enable_feedback = extra.getBoolean("enable_feedback");
                    executeScene(extra.getString(EditSceneActivity.RESULT_SCENE));
                }
                return false;
            }

            @Override
            public void onServiceFinished() {
                finish();
            }
        });

        // Show main window
        if (extra.getBoolean("show_mainWindow")) {
            Intent mainIt = new Intent(this, MainActivity.class);
            startActivity(mainIt);
        }
    }

    void executeSingleAction(String port_uuid_string, final int command) {
        final UUID port_uuid = UUID.fromString(port_uuid_string);
        final DevicePort port = NetpowerctrlApplication.getDataController().findDevicePort(port_uuid);
        if (port == null) {
            Toast.makeText(this, getString(R.string.error_shortcut_not_valid), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        NetpowerctrlApplication.getDataController().registerStateChanged(new RuntimeStateChanged() {
            @Override
            public boolean onDataLoaded() {
                return true;
            }

            @Override
            public boolean onDataQueryFinished() {
                NetpowerctrlApplication.getDataController().execute(port, command, ExecutionActivity.this);
                return false;
            }
        });
    }

    void executeScene(String scene_string) {
        // Extract scene from extra bundle
        try {
            scene = Scene.fromJSON(JSONHelper.getReader(scene_string));
        } catch (IOException ignored) {
            scene = null;
        }
        if (scene == null) {
            Toast.makeText(this, getString(R.string.error_shortcut_not_valid), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // DeviceQuery for scene devices
        TreeSet<DeviceInfo> devices = new TreeSet<>();
        scene_commands = scene.getDevices(devices);
        new DeviceQuery(ExecutionActivity.this, devices);

    }

    @Override
    public void onExecutionFinished(int commands) {
        scene_executed_commands += commands;
        if (scene_executed_commands >= scene_commands)
            finish();
    }

    @Override
    public void onDeviceUpdated(DeviceInfo di) {
    }

    @Override
    public void onObserverJobFinished(List<DeviceInfo> timeout_devices) {
        if (enable_feedback) {
            //noinspection ConstantConditions
            ShowToast.showToast(this,
                    this.getString(R.string.scene_executed, scene.sceneName), 800);
        }
        NetpowerctrlApplication.getDataController().execute(scene, this);
    }

    @Override
    public void onDeviceError(DeviceInfo di) {
        Toast.makeText(this, getString(R.string.error_nopass, di.DeviceName), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeviceTimeout(DeviceInfo di) {
        Toast.makeText(this, getString(R.string.error_timeout_device, di.DeviceName), Toast.LENGTH_SHORT).show();
    }
}