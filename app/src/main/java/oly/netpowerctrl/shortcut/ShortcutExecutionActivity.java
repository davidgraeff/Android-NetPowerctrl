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
    private ShortcutExecutionActivity that = this;
    private boolean listener_started = false;
    private Collection<DeviceCommand> deviceCommands;
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
        Scene g = null;
        try {
            g = Scene.fromJSON(JSONHelper.getReader(extra.getString("commands")));
        } catch (IOException ignored) {
            g = null;
        }
        if (g == null) {
            Toast.makeText(this, getString(R.string.error_shortcut_not_valid), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        updateWidget = extra.containsKey("widgetId");
        if (updateWidget) {
            widgetId = extra.getInt("widgetId");
        }

        // Convert to device sceneOutlets
        deviceCommands = DeviceCommand.fromOutletCommandGroup(g);

        // Start listener and Update device state
        listener_started = true;
        NetpowerctrlApplication.instance.startListener(false);
        new DeviceQuery(this, this, g.getDevices(), false, true);

        setResult(RESULT_OK);

        // Show main window
        if (extra.getBoolean("show_mainWindow")) {
            Intent mainIt = new Intent(this, NetpowerctrlActivity.class);
            startActivity(mainIt);
        }

        if (extra.getBoolean("enable_feedback")) {
            //noinspection ConstantConditions
            Toast.makeText(this,
                    this.getString(R.string.scene_executed, g.sceneName), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeviceUpdated(DeviceInfo di) {
        Iterator<DeviceCommand> it = deviceCommands.iterator();
        while (it.hasNext()) {
            DeviceCommand c = it.next();
            if (c.device.equals(di)) {
                di.updateByDeviceCommand(c);
                DeviceSend.sendAllOutlets(this, c, true);
                it.remove();
            }

        }
    }

    @Override
    public void onDeviceQueryFinished(int timeout_devices) {
        if (updateWidget)
            WidgetUpdateService.updateWidgetWithoutDataFetch(widgetId, this);
        finish();
    }

    @Override
    public void onDeviceTimeout(DeviceInfo di) {
        Toast.makeText(this, "Execution: Timeout for device " + di.DeviceName, Toast.LENGTH_SHORT).show();
        finish();
    }
}