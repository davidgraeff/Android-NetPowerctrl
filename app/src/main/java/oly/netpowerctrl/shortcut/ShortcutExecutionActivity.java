package oly.netpowerctrl.shortcut;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import oly.netpowerctrl.datastructure.OutletCommandGroup;
import oly.netpowerctrl.main.NetpowerctrlActivity;
import oly.netpowerctrl.network.UDPSendToDevice;

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
        @SuppressWarnings("null")
        OutletCommandGroup g = OutletCommandGroup.fromString(extra.getString("commands"), this);
        if (g == null) {
            Toast.makeText(this, "Shortcut not valid!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (UDPSendToDevice.sendOutlet(this, g))
            setResult(RESULT_OK, null);

        if (extra.getBoolean("show_mainwindow")) {
            Intent mainIt = new Intent(this, NetpowerctrlActivity.class);
            startActivity(mainIt);
        }
        finish();
    }
}