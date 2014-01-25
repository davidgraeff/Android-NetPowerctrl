package oly.netpowerctrl.shortcut;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anelservice.DeviceQuery;
import oly.netpowerctrl.anelservice.DeviceSend;
import oly.netpowerctrl.anelservice.DeviceUpdateStateOrTimeout;
import oly.netpowerctrl.anelservice.NetpowerctrlService;
import oly.netpowerctrl.anelservice.ServiceReady;
import oly.netpowerctrl.datastructure.DeviceCommand;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.main.NetpowerctrlActivity;
import oly.netpowerctrl.main.NetpowerctrlApplication;
import oly.netpowerctrl.utils.JSONHelper;
import oly.netpowerctrl.utils.ShowToast;

public class ShortcutExecutionActivity extends Activity implements DeviceUpdateStateOrTimeout {
    private Scene scene;
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
        setResult(RESULT_CANCELED);
        Intent it = getIntent();
        if (it == null) {
            finish();
            return;
        }

        // Extract command group from intent extra
        Bundle extra = it.getExtras();
        assert extra != null;
        try {
            scene = Scene.fromJSON(JSONHelper.getReader(extra.getString("commands")));
        } catch (IOException ignored) {
            scene = null;
        }
        if (scene == null) {
            Toast.makeText(this, getString(R.string.error_shortcut_not_valid), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        NetpowerctrlApplication.instance.registerServiceReadyObserver(new ServiceReady() {
            @Override
            public boolean onServiceReady(NetpowerctrlService mDiscoverService) {
                // Suspend widget updates for two update requests
                // (The first one is a refresh-value-request response, the
                // other one is for the automatic new-values broadcast of the anel devices)
                NetpowerctrlApplication.suspendWidgetUpdate = 2;
                new DeviceQuery(ShortcutExecutionActivity.this, scene.getDevices());
                return false;
            }
        });

        setResult(RESULT_OK);

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
        Collection<DeviceCommand> deviceCommands = DeviceCommand.fromOutletCommandGroup(scene);
        Iterator<DeviceCommand> it = deviceCommands.iterator();
        while (it.hasNext()) {
            DeviceCommand c = it.next();
            if (!c.device.reachable)
                continue;
            c.device.updateByDeviceCommand(c);
            DeviceSend.instance().sendOutlets(c, true);
            //it.remove();
        }
        finish();
    }

    @Override
    public void onDeviceTimeout(DeviceInfo di) {
        Toast.makeText(this, getString(R.string.error_timeout_device, di.DeviceName), Toast.LENGTH_SHORT).show();
    }
}