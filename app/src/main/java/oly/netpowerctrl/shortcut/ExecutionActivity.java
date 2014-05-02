package oly.netpowerctrl.shortcut;

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
import oly.netpowerctrl.application_state.ServiceReady;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.DevicePort;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.main.NetpowerctrlActivity;
import oly.netpowerctrl.network.DeviceQuery;
import oly.netpowerctrl.network.DeviceQueryResult;
import oly.netpowerctrl.network.ExecutionFinished;
import oly.netpowerctrl.utils.JSONHelper;
import oly.netpowerctrl.utils.ShowToast;

public class ExecutionActivity extends Activity implements DeviceQueryResult, ExecutionFinished {
    private Scene scene = null;
    private int scene_commands = 0;
    private int scene_executed_commands = 0;
//    private boolean updateWidget = false;

    @Override
    protected void onDestroy() {
        NetpowerctrlApplication.instance.stopUseListener();
        super.onDestroy();
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NetpowerctrlApplication.instance.useListener();
        Intent it = getIntent();
        if (it == null) {
            finish();
            return;
        }

        setVisible(false);

        // Extract name group from intent extra
        Bundle extra = it.getExtras();
        assert extra != null;

        // Execute single action (in contrast to scene)
        if (extra.containsKey(EditShortcutActivity.RESULT_ACTION_UUID)) {
            executeSingleAction(extra);
        } else {
            executeScene(extra);

            // Show main window
            if (extra.getBoolean("show_mainWindow")) {
                Intent mainIt = new Intent(this, NetpowerctrlActivity.class);
                startActivity(mainIt);
            }

            if (extra.getBoolean("enable_feedback")) {
                //noinspection ConstantConditions
                ShowToast.showToast(this,
                        this.getString(R.string.scene_executed, scene.sceneName), 800);
            }
        }
    }

    void executeSingleAction(Bundle extra) {
        UUID port_uuid = UUID.fromString(extra.getString(EditShortcutActivity.RESULT_ACTION_UUID));
        final DevicePort port = NetpowerctrlApplication.getDataController().findDevicePort(port_uuid);
        if (port == null) {
            Toast.makeText(this, getString(R.string.error_shortcut_not_valid), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        final int command = extra.getInt(EditShortcutActivity.RESULT_ACTION_COMMAND);
        NetpowerctrlApplication.instance.registerServiceReadyObserver(new ServiceReady() {
            @Override
            public boolean onServiceReady() {
                NetpowerctrlApplication.getDataController().execute(port, command, ExecutionActivity.this);
                return false;
            }

            @Override
            public void onServiceFinished() {
                finish();
            }
        });
    }

    void executeScene(Bundle extra) {
        // Extract scene from extra bundle
        try {
            scene = Scene.fromJSON(JSONHelper.getReader(extra.getString(EditShortcutActivity.RESULT_SCENE)));
        } catch (IOException ignored) {
            scene = null;
        }
        if (scene == null) {
            Toast.makeText(this, getString(R.string.error_shortcut_not_valid), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // The application may have be started here, we have to wait for the service to be ready
        NetpowerctrlApplication.instance.registerServiceReadyObserver(new ServiceReady() {
            @Override
            public boolean onServiceReady() {
                // DeviceQuery for scene devices
                TreeSet<DeviceInfo> devices = new TreeSet<DeviceInfo>();
                scene_commands = scene.getDevices(devices);
                new DeviceQuery(ExecutionActivity.this, devices);
                return false;
            }

            @Override
            public void onServiceFinished() {
                finish();
            }
        });
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
    public void onDeviceQueryFinished(List<DeviceInfo> timeout_devices) {
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