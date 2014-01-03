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

import java.util.Calendar;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.OutletCommandGroup;
import oly.netpowerctrl.listadapter.OutletListAdapter;
import oly.netpowerctrl.utils.ListItemMenu;

public class ShortcutCreatorActivity extends Activity implements ListItemMenu {
    public static final String CREATE_SCENE = "scenes";
    public static final String LOAD_SCENE = "load";
    private OutletListAdapter adpOutlets = null;
    private ListView lvOutletSelect;
    private Switch show_mainWindow;
    private TextView shortcutName;
    private boolean shortcutName_changed = false;
    private final Context that = this;
    private OutletCommandGroup og;
    private boolean isLoaded = false;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED, null);
        setContentView(R.layout.shortcut_activity);

        show_mainWindow = (Switch) findViewById(R.id.shortcut_show_mainwindow);
        shortcutName = (TextView) findViewById(R.id.shortcut_name);
        // if the shortKey name has changed, we disallow automatically changes to the name
        shortcutName.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                //noinspection ConstantConditions
                shortcutName_changed = shortcutName.getText().length() != 0;
                return false;
            }
        });
        // Initial default name for a shortcut is the app name and current date and time

        boolean isForGroups = false;
        Intent it = getIntent();
        if (it != null) {
            Bundle extra = it.getExtras();
            if (extra != null) {
                isForGroups = extra.getBoolean(CREATE_SCENE);
                og = OutletCommandGroup.fromString(extra.getString(LOAD_SCENE, null), this);
                if (og == null) {
                    finish();
                    return;
                }

                isLoaded = true;
                shortcutName_changed = true;
                adpOutlets = OutletListAdapter.createByOutletCommands(this, og.commands);
                shortcutName.setText(og.sceneName);
            }
        }

        if (adpOutlets == null) {
            adpOutlets = OutletListAdapter.createByConfiguredDevices(this);
            og = new OutletCommandGroup();
        }
        adpOutlets.setListItemMenu(this);

        if (isForGroups) {
            show_mainWindow.setVisibility(View.GONE);
            if (isLoaded)
                setTitle(R.string.title_scene_edit);
            else
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
                og.commands = adpOutlets.getCheckedItems();
                //noinspection ConstantConditions
                og.sceneName = shortcutName.getText().toString();
                if (og.sceneName.isEmpty() || og.length() == 0)
                    return;
                Intent extra = Shortcuts.createShortcutExecutionIntent(that, og, show_mainWindow.isChecked());
                // Return result
                setResult(RESULT_OK, Shortcuts.createShortcut(that, extra, og.sceneName));
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
    public void onMenuItemClicked(View v, int position) {
        og.commands = adpOutlets.getCheckedItems();
        if (!shortcutName_changed) {
            if (og.length() > 3) {
                Calendar t = Calendar.getInstance();
                String default_name = DateFormat.getMediumDateFormat(that).format(t.getTime()) + " - " + DateFormat.getTimeFormat(that).format(t.getTime());
                shortcutName.setText(getResources().getString(R.string.app_name) + " (" + og.length() + ") " + default_name);
            } else
                shortcutName.setText(og.buildDetails(this));
        }
        //noinspection ConstantConditions
        findViewById(R.id.btnAcceptShortcut).setEnabled(og.length() != 0 && shortcutName.getText().length() != 0);
    }
}