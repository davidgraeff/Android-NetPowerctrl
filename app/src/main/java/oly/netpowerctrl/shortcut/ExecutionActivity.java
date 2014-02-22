package oly.netpowerctrl.shortcut;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.DevicePort;
import oly.netpowerctrl.datastructure.Executor;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.main.NetpowerctrlActivity;
import oly.netpowerctrl.network.DeviceQuery;
import oly.netpowerctrl.network.DeviceUpdateStateOrTimeout;
import oly.netpowerctrl.network.NetpowerctrlService;
import oly.netpowerctrl.network.ServiceReady;
import oly.netpowerctrl.utils.JSONHelper;
import oly.netpowerctrl.utils.ShowToast;

public class ExecutionActivity extends Activity implements DeviceUpdateStateOrTimeout {
    private Scene scene = null;
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

        // Extract command group from intent extra
        Bundle extra = it.getExtras();
        assert extra != null;

        // Execute single action (in contrast to scene)
        if (extra.containsKey(EditShortcutActivity.RESULT_ACTION_UUID)) {
            UUID port_uuid = UUID.fromString(extra.getString(EditShortcutActivity.RESULT_ACTION_UUID));
            DevicePort port = NetpowerctrlApplication.getDataController().findDevicePort(port_uuid);
            if (port == null) {
                Toast.makeText(this, getString(R.string.error_shortcut_not_valid), Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            Executor.execute(port, extra.getInt(EditShortcutActivity.RESULT_ACTION_COMMAND));
            finish();
            return;
        }

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

        setVisible(false);

        // The application may have be started here, we have to wait for the service to be ready
        NetpowerctrlApplication.instance.registerServiceReadyObserver(new ServiceReady() {
            @Override
            public boolean onServiceReady(NetpowerctrlService mDiscoverService) {
                // DeviceQuery for scene devices
                new DeviceQuery(ExecutionActivity.this, scene.getDevices());
                return false;
            }
        });

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

    @Override
    public void onDeviceUpdated(DeviceInfo di) {
    }

    @Override
    public void onDeviceQueryFinished(List<DeviceInfo> timeout_devices) {
        Executor.execute(scene);
        finish();
    }

    @Override
    public void onDeviceTimeout(DeviceInfo di) {
        Toast.makeText(this, getString(R.string.error_timeout_device, di.DeviceName), Toast.LENGTH_SHORT).show();
    }
}