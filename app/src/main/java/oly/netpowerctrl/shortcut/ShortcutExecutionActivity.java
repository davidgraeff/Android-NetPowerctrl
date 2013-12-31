package oly.netpowerctrl.shortcut;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anelservice.DeviceSend;
import oly.netpowerctrl.datastructure.OutletCommandGroup;
import oly.netpowerctrl.main.NetpowerctrlActivity;

public class ShortcutExecutionActivity extends Activity {
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent it = getIntent();
        if (it == null) {
            finish();
            return;
        }

        Bundle extra = it.getExtras();
        //TODO get all devices from application, force update before sending
        assert extra != null;
        OutletCommandGroup g = OutletCommandGroup.fromString(extra.getString("commands"), this);
        if (g == null) {
            Toast.makeText(this, getString(R.string.error_shortcut_not_valid), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        DeviceSend.sendOutlet(this, g);
        setResult(RESULT_OK, null);

        Toast.makeText(this, extra.getBoolean("show_mainWindow") ? "show main" : "not show main", Toast.LENGTH_SHORT).show();
        if (extra.getBoolean("show_mainWindow")) {
            Intent mainIt = new Intent(this, NetpowerctrlActivity.class);
            startActivity(mainIt);
        }
        finish();
    }
}