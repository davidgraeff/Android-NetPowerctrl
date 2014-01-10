package oly.netpowerctrl.shortcut;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anelservice.DeviceQuery;
import oly.netpowerctrl.anelservice.DeviceSend;
import oly.netpowerctrl.anelservice.DeviceUpdateStateOrTimeout;
import oly.netpowerctrl.datastructure.DeviceCommand;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.main.NetpowerctrlActivity;
import oly.netpowerctrl.main.NetpowerctrlApplication;
import oly.netpowerctrl.utils.JSONHelper;
import oly.netpowerctrl.widget.WidgetUpdateService;

public class ShortcutExecutionActivity extends Activity implements DeviceUpdateStateOrTimeout {
    private boolean listener_started = false;
    private Scene scene;
    private boolean updateWidget = false;
    private int widgetId = 0;

    @Override
    protected void onDestroy() {
        if (listener_started)
            NetpowerctrlApplication.instance.stopListener();
        super.onDestroy();
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        updateWidget = extra.containsKey("widgetId");
        if (updateWidget) {
            widgetId = extra.getInt("widgetId");
        }

        // Convert to device sceneOutlets

        // Start listener and Update device state
        listener_started = true;
        NetpowerctrlApplication.instance.startListener(false);
        new DeviceQuery(this, this, scene.getDevices(), false, true);

        setResult(RESULT_OK);

        // Show main window
        if (extra.getBoolean("show_mainWindow")) {
            Intent mainIt = new Intent(this, NetpowerctrlActivity.class);
            startActivity(mainIt);
        }

        if (extra.getBoolean("enable_feedback")) {
            //noinspection ConstantConditions
            Toast.makeText(this,
                    this.getString(R.string.scene_executed, scene.sceneName), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeviceUpdated(DeviceInfo di) {

    }

    @Override
    public void onDeviceQueryFinished(int timeout_devices) {
        Collection<DeviceCommand> deviceCommands = DeviceCommand.fromOutletCommandGroup(scene);
        Iterator<DeviceCommand> it = deviceCommands.iterator();
        while (it.hasNext()) {
            DeviceCommand c = it.next();
            if (!c.device.reachable)
                continue;
            c.device.updateByDeviceCommand(c);
            DeviceSend.instance().sendOutlets(c, true);
            it.remove();
        }

        if (updateWidget)
            WidgetUpdateService.updateWidgetWithoutDataFetch(widgetId, this);
        finish();
    }

    @Override
    public void onDeviceTimeout(DeviceInfo di) {
        Toast.makeText(this, getString(R.string.error_timeout_device, di.DeviceName), Toast.LENGTH_SHORT).show();
    }
}