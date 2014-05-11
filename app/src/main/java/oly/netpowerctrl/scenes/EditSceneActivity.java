package oly.netpowerctrl.scenes;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.devices.DeviceInfo;
import oly.netpowerctrl.devices.DevicePort;
import oly.netpowerctrl.devices.DevicePortsAvailableAdapter;
import oly.netpowerctrl.devices.DevicePortsCreateSceneAdapter;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.Icons;
import oly.netpowerctrl.utils.JSONHelper;
import oly.netpowerctrl.utils.ListItemMenu;
import oly.netpowerctrl.utils.Shortcuts;

/**
 * This activity is responsible for creating a "scene" either for the scene list
 * in the application or for a shortcut intent for the home-screen.
 */
public class EditSceneActivity extends Activity implements ListItemMenu, EditSceneFragmentReady, Icons.IconSelected {

    /**
     * We pass arguments to this activity via the intent extra bundle.
     * Define some constants here.
     */
    public static final String EDIT_SCENE_NOT_SHORTCUT = "scenes";
    public static final String LOAD_SCENE = "load";
    public static final String RESULT_SCENE = "scene";
    public static final String RESULT_ACTION_UUID = "action_uuid";
    public static final String RESULT_ACTION_COMMAND = "action_command";

    // UI widgets
    private Switch show_mainWindow;
    private Switch enable_feedback;
    private View btnDone;
    private DevicePortsAvailableAdapter adapter_available;
    private DevicePortsCreateSceneAdapter adapter_included;

    // Scene and flag variables
    private Scene scene;
    private boolean isLoaded = false;
    private boolean isSceneNotShortcut = false;

    // Scene Icon
    private Bitmap scene_icon;
    private EditSceneFragment fragment_included;
    private EditSceneFragment fragment_available;
    private boolean twoPane = false;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // set theme based on user preference
        if (SharedPrefs.isDarkTheme()) {
            setTheme(R.style.Theme_CustomDarkTheme);
        } else {
            setTheme(R.style.Theme_CustomLightTheme);
        }

        // Default result
        setResult(RESULT_CANCELED, null);

        reInitUI();

        //set the actionbar to use the custom view (can also be done with a style)
        ActionBar bar = getActionBar();
        assert bar != null;
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_CUSTOM);
        bar.setHomeButtonEnabled(true);
        bar.setCustomView(R.layout.create_scene_done);

        btnDone = findViewById(R.id.action_mode_save_button);
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                save_and_close();
            }
        });
        View btnCancel = findViewById(R.id.action_mode_close_button);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void reInitUI() {
        // set content view, get references to widgets
        setContentView(R.layout.activity_create_scene);
        show_mainWindow = (Switch) findViewById(R.id.shortcut_show_mainwindow);
        enable_feedback = (Switch) findViewById(R.id.shortcut_enable_feedback);

        FragmentManager m = getFragmentManager();

        // Show the included and available list for the scene. Both are fragments. Either in a
        // viewPager (small width) or both visible at the same time.
        // If there is a viewPager, use that. Otherwise we show both fragments next to each other.
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        if (pager != null) {
            twoPane = false;
            EditScenePagerAdapter pagerAdapter = new EditScenePagerAdapter(m);
            fragment_included = pagerAdapter.getFragmentIncluded();
            fragment_available = pagerAdapter.getFragmentAvailable();
            pager.setAdapter(pagerAdapter);
        } else {
            twoPane = true;
            fragment_included = (EditSceneFragment) m.findFragmentById(R.id.scene_edit_fragment1);
            fragment_available = (EditSceneFragment) m.findFragmentById(R.id.scene_edit_fragment2);
        }

        // Assign data to the fragments
        fragment_included.setData(this,
                EditSceneFragment.TYPE_INCLUDED,
                this);
        fragment_available.setData(this,
                EditSceneFragment.TYPE_AVAILABLE,
                this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            reInitUI();
        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            reInitUI();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        NetpowerctrlApplication.instance.useListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        NetpowerctrlApplication.instance.stopUseListener();
    }

    private void loadContent() {
        Intent it = getIntent();
        if (it != null) {
            Bundle extra = it.getExtras();
            if (extra != null) {
                isSceneNotShortcut = extra.getBoolean(EDIT_SCENE_NOT_SHORTCUT);

                String load_scene = extra.getString(LOAD_SCENE, null);
                if (load_scene != null) {
                    try {
                        scene = Scene.fromJSON(JSONHelper.getReader(load_scene));
                    } catch (IOException ignored) {
                        finish();
                        return;
                    }
                    isLoaded = true;
                    adapter_included.loadItemsOfScene(scene);
                    adapter_included.setMasterOfScene(scene);
                    adapter_included.notifyDataSetChanged();
                    //shortcutName.setText(scene.sceneName);
                }
            }
        }

        // Load current set of available outlets
        List<DeviceInfo> configuredDevices = NetpowerctrlApplication.getDataController().deviceCollection.devices;
        adapter_available.update(configuredDevices);
        adapter_available.removeAll(adapter_included);

        if (scene == null) {
            scene = new Scene();
        }

        // Add click listener for the remove button on each included action
        adapter_included.setListItemMenu(this);

        // Add click listener to available gridGrid to move the clicked action
        // to the included gridView.
        fragment_available.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                DevicePort oi = adapter_available.getItem(position);
                fragment_available.dismissItem(position);
                adapter_included.addItem(oi, DevicePort.TOGGLE);
                fragment_included.notifyDataSetChanged();
                invalidateOptionsMenu();
            }
        });

        if (isSceneNotShortcut) {
            if (isLoaded) {
                setTitle(R.string.title_scene_edit);
                setIcon(null, Icons.loadIcon(this, scene.uuid, Icons.IconType.SceneIcon, Icons.IconState.StateUnknown, 0));
            } else
                setTitle(R.string.title_scene);
        } else {
            show_mainWindow.setVisibility(View.VISIBLE);
            enable_feedback.setVisibility(View.VISIBLE);
            setTitle(R.string.title_shortcut);
        }

        sceneNameChanged();

        fragment_available.checkEmpty(twoPane);
        fragment_included.checkEmpty(twoPane);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.shortcut_create_menu, menu);
        return true;
    }

    private void save_and_close() {
        // Generate list of checked items
        scene.sceneItems = adapter_included.getScene();
        scene.setMaster(adapter_included.getMaster());
        if (scene.sceneName.isEmpty() || scene.length() == 0)
            return;

        if (isSceneNotShortcut) {
            NetpowerctrlApplication.getDataController().sceneCollection.setBitmap(this, scene, scene_icon);
            NetpowerctrlApplication.getDataController().sceneCollection.add(scene);
        } else {
            Intent extra = Shortcuts.createShortcutExecutionIntent(EditSceneActivity.this,
                    scene, show_mainWindow.isChecked(), enable_feedback.isChecked());
            // Return result
            Intent shortcut;
            if (scene_icon != null) {
                shortcut = Shortcuts.createShortcut(extra, scene.sceneName,
                        Icons.resizeBitmap(this, scene_icon));
            } else
                shortcut = Shortcuts.createShortcut(extra, scene.sceneName,
                        EditSceneActivity.this);
            setResult(RESULT_OK, shortcut);
        }
        finish();
    }

    /*
     * ActionBar icon clicked
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
            case R.id.menu_icon:
                Icons.show_select_icon_dialog(this, "scene_icons", this, null);
                return true;
            case R.id.menu_name:
                requestName(scene);
                return true;
            case R.id.menu_switch_all_on:
                adapter_included.switchAllOn();
                return true;
            case R.id.menu_switch_all_off:
                adapter_included.switchAllOff();
                return true;
            case R.id.menu_switch_all_toogle:
                adapter_included.toggleAll();
                return true;
            case R.id.menu_switch_all_ignore:
                adapter_included.clear();
                adapter_available.update(NetpowerctrlApplication.getDataController().deviceCollection.devices);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void requestName(final Scene scene) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(this.getString(R.string.scene_rename));

        final EditText input = new EditText(alert.getContext());
        input.setText(scene.sceneName);
        alert.setView(input);

        alert.setPositiveButton(this.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                @SuppressWarnings("ConstantConditions")
                String name = input.getText().toString();
                if (name.trim().isEmpty())
                    return;
                scene.sceneName = name;
                sceneNameChanged();
            }
        });

        alert.setNegativeButton(this.getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        alert.show();
    }

    /**
     * Called by the widget/object/dialog that is responsible for asking the
     * user for a new scene name after a name has been chosen.
     */
    void sceneNameChanged() {
        //noinspection ConstantConditions
        getActionBar().setSubtitle(scene.sceneName.length() == 0 ? getString(R.string.scene_no_name) : scene.sceneName);
        invalidateOptionsMenu();
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void setIcon(Object context_object, Bitmap o) {
        scene_icon = o;
        if (o == null) {
            getActionBar().setIcon(getApplicationInfo().icon);
        } else
            getActionBar().setIcon(new BitmapDrawable(getResources(), o));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        Icons.activityCheckForPickedImage(this, this, requestCode, resultCode, imageReturnedIntent);
    }

    /**
     * This is called when the user clicks on remove-action on an included action of a scene.
     *
     * @param v
     * @param position
     */
    @Override
    public void onMenuItemClicked(View v, int position) {
        adapter_available.addItem(adapter_included.getItem(position), DevicePort.TOGGLE);
        fragment_available.notifyDataSetChanged();
        fragment_included.dismissItem(position);
        invalidateOptionsMenu();
    }

    /* Called whenever we call invalidateOptionsMenu(). Enables or disables the
     * "done" action bar button. */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (scene == null)
            return super.onPrepareOptionsMenu(menu);

        boolean en = (adapter_included.getCount() != 0) && scene.sceneName.length() > 0;
        btnDone.setVisibility(en ? View.VISIBLE : View.GONE);
        TextView text = (TextView) findViewById(R.id.hintText);
        if (adapter_included.getCount() == 0)
            text.setText(getString(R.string.error_scene_no_actions));
        else if (scene.sceneName.length() == 0) {
            text.setText(getString(R.string.error_scene_no_name));
        }
        text.setVisibility(en ? View.GONE : View.VISIBLE);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void sceneEditFragmentReady(final EditSceneFragment fragment) {
        final int type = fragment.getType();
        if (type == EditSceneFragment.TYPE_AVAILABLE) {
            adapter_available = (DevicePortsAvailableAdapter) fragment.getAdapter();
        } else if (type == EditSceneFragment.TYPE_INCLUDED) {
            adapter_included = (DevicePortsCreateSceneAdapter) fragment.getAdapter();
        }

        // When both adapters and gridViews are available, we
        // can start loading the content after a short delay.
        if (adapter_available != null && adapter_included != null) {
            Handler h = new Handler();
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadContent();
                }
            }, 50);
        }
    }

    /**
     * Called by one of the fragments if animations are activated and on_swipe_to_dismiss has been
     * used (an entry has been swiped away). Because swiping is only enabled on the included adapter
     * for now, we do not check the fragment argument.
     *
     * @param fragment
     * @param position
     */
    @Override
    public void entryDismiss(EditSceneFragment fragment, int position) {
        // It's the same as if the user clicked the remove button.
        onMenuItemClicked(null, position);
    }

}