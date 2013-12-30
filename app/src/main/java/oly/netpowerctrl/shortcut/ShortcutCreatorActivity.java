package oly.netpowerctrl.shortcut;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.OutletCommandGroup;
import oly.netpowerctrl.listadapter.OutledListAdapter;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.MenuConfigureEvent;

public class ShortcutCreatorActivity extends Activity implements MenuConfigureEvent {
    ArrayList<DeviceInfo> alDevices;
    OutledListAdapter adpOutlets;
    ListView lvOutletSelect;
    Switch show_mainwindow;
    TextView shortcutName;
    boolean shortcutname_changed = false;
    final Context that = this;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED, null);
        setContentView(R.layout.shortcut_activity);

        alDevices = SharedPrefs.ReadConfiguredDevices(this);
        adpOutlets = new OutledListAdapter(this, alDevices);
        adpOutlets.setMenuConfigureEvent(this);
        show_mainwindow = (Switch) findViewById(R.id.shortcut_show_mainwindow);
        shortcutName = (TextView) findViewById(R.id.shortcut_name);
        // if the shortkey name has changed, we disallow automatically changes to the name
        shortcutName.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                shortcutname_changed = shortcutName.getText().length() != 0;
                return false;
            }
        });
        // Initial default name for a shortcut is the app name and current date and time

        boolean isForGroups = false;
        Intent it = getIntent();
        if (it != null) {
            Bundle extra = it.getExtras();
            if (extra != null) {
                isForGroups = extra.getBoolean("groups");
                OutletCommandGroup og = OutletCommandGroup.fromString(extra.getString("load", null), this);
                //TODO load scene
            }
        }

        if (isForGroups) {
            show_mainwindow.setVisibility(View.GONE);
            setTitle(R.string.title_scene);
        } else {
            setTitle(R.string.title_shortcut);
        }

        lvOutletSelect = (ListView) findViewById(R.id.lvOutletSelect);
        lvOutletSelect.setAdapter(adpOutlets);

        findViewById(R.id.btnAcceptShortcut).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Generate list of checked items
                OutletCommandGroup og = adpOutlets.getCheckedItems();
                og.groupname = shortcutName.getText().toString();
                if (og.groupname.isEmpty() || og.length() == 0)
                    return;
                Intent extra = Shortcuts.createShortcutExecutionIntent(that, og, show_mainwindow.isChecked());
                // Return result
                setResult(RESULT_OK, Shortcuts.createShortcut(that, extra, og.groupname));
                finish();
            }
        });

        findViewById(R.id.btnCancelShortcut).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public void onConfigure(View v, int position) {
        OutletCommandGroup og = adpOutlets.getCheckedItems();
        if (!shortcutname_changed) {
            if (og.length() > 3) {
                Calendar t = Calendar.getInstance();
                String defname = DateFormat.getMediumDateFormat(that).format(t.getTime()) + " - " + DateFormat.getTimeFormat(that).format(t.getTime());
                shortcutName.setText(getResources().getString(R.string.app_name) + " (" + og.length() + ") " + defname);
            } else
                shortcutName.setText(og.buildDetails(this));
        }
        findViewById(R.id.btnAcceptShortcut).setEnabled(og.length() != 0 && shortcutName.getText().length() != 0);
    }
}