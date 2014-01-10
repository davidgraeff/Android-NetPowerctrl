package oly.netpowerctrl.shortcut;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;

import java.io.IOException;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.datastructure.SceneOutlet;
import oly.netpowerctrl.listadapter.CreateSceneOutletsAdapter;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.JSONHelper;
import oly.netpowerctrl.utils.ListItemMenu;

public class ShortcutCreatorActivity extends Activity implements ListItemMenu {
    public static final String CREATE_SCENE = "scenes";
    public static final String LOAD_SCENE = "load";
    public static final String RESULT_SCENE = "commands";
    private CreateSceneOutletsAdapter adpOutlets = null;
    private Switch show_mainWindow;
    private Switch enable_feedback;
    private EditText shortcutName;
    private final Context that = this;
    private Scene og;
    private boolean isLoaded = false;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (SharedPrefs.isDarkTheme(this)) {
            setTheme(R.style.Theme_CustomDarkTheme);
        } else {
            setTheme(R.style.Theme_CustomLightTheme);
        }

        setResult(RESULT_CANCELED, null);
        setContentView(R.layout.create_scene_activity);

        show_mainWindow = (Switch) findViewById(R.id.shortcut_show_mainwindow);
        enable_feedback = (Switch) findViewById(R.id.shortcut_enable_feedback);

        shortcutName = (EditText) findViewById(R.id.shortcut_name);
        enableDisableAcceptButton();

        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                loadContent();
            }
        }, 200);
    }

    private void loadContent() {

        shortcutName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                enableDisableAcceptButton();
            }
        });

        boolean isForGroups = false;
        Intent it = getIntent();
        if (it != null) {
            Bundle extra = it.getExtras();
            if (extra != null) {
                isForGroups = extra.getBoolean(CREATE_SCENE);

                String load_scene = extra.getString(LOAD_SCENE, null);
                if (load_scene != null) {
                    try {
                        og = Scene.fromJSON(JSONHelper.getReader(load_scene));
                    } catch (IOException ignored) {
                        finish();
                        return;
                    }
                    isLoaded = true;
                    adpOutlets = CreateSceneOutletsAdapter.createByOutletCommands(this, og.sceneOutlets);
                    shortcutName.setText(og.sceneName);
                }
            }
        }

        if (adpOutlets == null) {
            adpOutlets = CreateSceneOutletsAdapter.createByConfiguredDevices(this);
            og = new Scene();
        }
        adpOutlets.setListItemMenu(this);

        if (isForGroups) {
            show_mainWindow.setVisibility(View.GONE);
            enable_feedback.setVisibility(View.GONE);
            if (isLoaded)
                setTitle(R.string.title_scene_edit);
            else
                setTitle(R.string.title_scene);
        } else {
            setTitle(R.string.title_shortcut);
        }

        ListView lvOutletSelect = (ListView) findViewById(R.id.lvOutletSelect);
        lvOutletSelect.setAdapter(adpOutlets);

        findViewById(R.id.btnAcceptShortcut).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Generate list of checked items
                og.sceneOutlets = adpOutlets.getCheckedItems();
                //noinspection ConstantConditions
                og.sceneName = shortcutName.getText().toString();
                if (og.sceneName.isEmpty() || og.length() == 0)
                    return;
                Intent extra = Shortcuts.createShortcutExecutionIntent(that, og, show_mainWindow.isChecked(), enable_feedback.isChecked());
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
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.shortcut_create_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_switch_all_on:
                adpOutlets.switchAll(SceneOutlet.ON);
                return true;
            case R.id.menu_switch_all_off:
                adpOutlets.switchAll(SceneOutlet.OFF);
                return true;
            case R.id.menu_switch_all_toogle:
                adpOutlets.switchAll(SceneOutlet.TOGGLE);
                return true;
            case R.id.menu_switch_all_ignore:
                adpOutlets.switchAll(-1);
                return true;
            case R.id.menu_help:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setIcon(R.drawable.netpowerctrl);
                builder.setTitle(R.string.menu_help);
                builder.setMessage(R.string.shortcut_help);
                builder.create().show();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onMenuItemClicked(View v, int position) {
        og.sceneOutlets = adpOutlets.getCheckedItems();
        enableDisableAcceptButton();
    }

    private void enableDisableAcceptButton() {
        //noinspection ConstantConditions
        findViewById(R.id.btnAcceptShortcut).setEnabled((og != null) && (og.length() != 0) &&
                shortcutName.getText().length() > 0);

    }
}