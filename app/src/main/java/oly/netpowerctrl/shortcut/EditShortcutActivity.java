package oly.netpowerctrl.shortcut;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v13.app.FragmentPagerAdapter;
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
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.DevicePort;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.dynamicgid.DynamicGridView;
import oly.netpowerctrl.listadapter.DevicePortsAvailableAdapter;
import oly.netpowerctrl.listadapter.DevicePortsBaseAdapter;
import oly.netpowerctrl.listadapter.DevicePortsCreateSceneAdapter;
import oly.netpowerctrl.main.SceneEditFragment;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.Icons;
import oly.netpowerctrl.utils.JSONHelper;
import oly.netpowerctrl.utils.ListItemMenu;

/**
 * This activity is responsible for creating a "scene" either for the scene list
 * in the application or for a shortcut intent for the home-screen.
 */
public class EditShortcutActivity extends Activity implements ListItemMenu, OutletsManipulator, Icons.IconSelected {

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
    private DynamicGridView grid_available;
    private DynamicGridView grid_included;
    private DevicePortsAvailableAdapter adapter_available;
    private DevicePortsCreateSceneAdapter adapter_included;

    // Scene and flag variables
    private Scene scene;
    private boolean isLoaded = false;
    private boolean isSceneNotShortcut = false;

    // Scene Icon
    private Bitmap scene_icon;

    /**
     * This adapter is for the FragmentPager to show two OutletsFragments
     * (Available actions, scene included actions)
     */
    private class Available_Added_Adapter extends FragmentPagerAdapter {
        private Fragment[] frag;

        public Available_Added_Adapter(FragmentManager fm) {
            super(fm);

            SceneEditFragment f1 = new SceneEditFragment();
            f1.setData(EditShortcutActivity.this,
                    SceneEditFragment.MANIPULATOR_TAG_INCLUDED,
                    EditShortcutActivity.this);
            SceneEditFragment f2 = new SceneEditFragment();
            f2.setData(EditShortcutActivity.this,
                    SceneEditFragment.MANIPULATOR_TAG_AVAILABLE,
                    EditShortcutActivity.this);
            frag = new Fragment[]{f1, f2};
        }

        @Override
        public Fragment getItem(int i) {
            return frag[i];
        }

        @Override
        public int getCount() {
            return frag.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Zugewiesene Aktionen";
                case 1:
                    return "Verfügbar";
            }
            return "";
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // set theme based on user preference
        if (SharedPrefs.isDarkTheme()) {
            setTheme(R.style.Theme_CustomDarkTheme);
        } else {
            setTheme(R.style.Theme_CustomLightTheme);
        }

        // Default result and set content view, get references to widgets
        setResult(RESULT_CANCELED, null);
        setContentView(R.layout.create_scene_activity);
        show_mainWindow = (Switch) findViewById(R.id.shortcut_show_mainwindow);
        enable_feedback = (Switch) findViewById(R.id.shortcut_enable_feedback);

        //set the actionbar to use the custom view (can also be done with a style)
        ActionBar bar = getActionBar();
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

        // ViewPager init
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(new Available_Added_Adapter(getFragmentManager()));
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
                    adapter_included.loadByScene(scene);
                    //shortcutName.setText(scene.sceneName);
                }
            }
        }

        // Load current set of available outlets
        List<DeviceInfo> configuredDevices = NetpowerctrlApplication.getDataController().configuredDevices;
        adapter_available.update(configuredDevices);
        adapter_available.removeAll(adapter_included);

        if (scene == null) {
            scene = new Scene();
        }

        // Add click listener for the remove button on each included action
        adapter_included.setListItemMenu(this);

        // Add click listener to available gridGrid to move the clicked action
        // to the included gridView.
        grid_available.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                DevicePort oi = adapter_available.getItem(position);
                adapter_available.removeAt(position);
                adapter_included.addItem(oi, DevicePort.TOGGLE);
                adapter_included.notifyDataSetChanged();
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
        if (scene.sceneName.isEmpty() || scene.length() == 0)
            return;

        if (isSceneNotShortcut) {
            NetpowerctrlApplication.getDataController().scenes.setBitmap(this, scene, scene_icon);
            NetpowerctrlApplication.getDataController().scenes.addScene(scene);
        } else {
            Intent extra = Shortcuts.createShortcutExecutionIntent(EditShortcutActivity.this,
                    scene, show_mainWindow.isChecked(), enable_feedback.isChecked());
            // Return result
            Intent shortcut;
            if (scene_icon != null) {
                shortcut = Shortcuts.createShortcut(extra, scene.sceneName,
                        Icons.resizeBitmap(this, scene_icon));
            } else
                shortcut = Shortcuts.createShortcut(extra, scene.sceneName,
                        EditShortcutActivity.this);
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
                adapter_available.update(NetpowerctrlApplication.getDataController().configuredDevices);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void requestName(final Scene scene) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(this.getString(R.string.outlet_to_scene_title));
        alert.setMessage(this.getString(R.string.scene_set_name));

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
    public void sceneNameChanged() {
        getActionBar().setSubtitle(scene.sceneName.length() == 0 ? getString(R.string.scene_no_name) : scene.sceneName);
        invalidateOptionsMenu();
    }

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
        adapter_available.notifyDataSetChanged();
        adapter_included.removeAt(position);
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
    public void setManipulatorObjects(int tag, final DynamicGridView view, DevicePortsBaseAdapter adapter) {
        if (tag == SceneEditFragment.MANIPULATOR_TAG_AVAILABLE) {
            grid_available = view;
            adapter_available = (DevicePortsAvailableAdapter) adapter;
        } else if (tag == SceneEditFragment.MANIPULATOR_TAG_INCLUDED) {
            grid_included = view;
            adapter_included = (DevicePortsCreateSceneAdapter) adapter;
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
            }, 150);
        }
    }

}