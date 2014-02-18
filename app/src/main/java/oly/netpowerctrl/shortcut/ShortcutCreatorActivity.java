package oly.netpowerctrl.shortcut;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.datastructure.OutletInfo;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.dynamicgid.DynamicGridView;
import oly.netpowerctrl.listadapter.OutletsCreateSceneAdapter;
import oly.netpowerctrl.main.NetpowerctrlActivity;
import oly.netpowerctrl.main.OutletsSceneEditFragment;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.ArrayAdapterWithIcons;
import oly.netpowerctrl.utils.JSONHelper;
import oly.netpowerctrl.utils.ListItemMenu;
import oly.netpowerctrl.utils.Scenes;

/**
 * This activity is responsible for creating a "scene" either for the scene list
 * in the application or for a shortcut intent for the home-screen.
 */
public class ShortcutCreatorActivity extends Activity implements ListItemMenu,
        Scenes.SceneNameChanged, OutletsManipulator {

    /**
     * We pass arguments to this activity via the intent extra bundle.
     * Define some constants here.
     */
    public static final String CREATE_SCENE = "scenes";
    public static final String LOAD_SCENE = "load";
    public static final String RESULT_SCENE = "commands";

    // UI widgets
    private Switch show_mainWindow;
    private Switch enable_feedback;
    private View btnDone;
    private DynamicGridView grid_available;
    private DynamicGridView grid_included;
    private OutletsCreateSceneAdapter adapter_available;
    private OutletsCreateSceneAdapter adapter_included;

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

            frag = new Fragment[]{
                    new OutletsSceneEditFragment(ShortcutCreatorActivity.this,
                            OutletsSceneEditFragment.MANIPULATOR_TAG_INCLUDED,
                            ShortcutCreatorActivity.this),
                    new OutletsSceneEditFragment(ShortcutCreatorActivity.this,
                            OutletsSceneEditFragment.MANIPULATOR_TAG_AVAILABLE,
                            ShortcutCreatorActivity.this)};
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
        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_CUSTOM);

        getActionBar().setCustomView(R.layout.create_scene_done);
        btnDone = (LinearLayout) findViewById(R.id.action_mode_close_button);
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                save_and_close();
            }
        });

        // ViewPager init
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(new Available_Added_Adapter(getFragmentManager()));
    }

    private void loadContent() {
        Intent it = getIntent();
        if (it != null) {
            Bundle extra = it.getExtras();
            if (extra != null) {
                isSceneNotShortcut = extra.getBoolean(CREATE_SCENE);

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
        adapter_available.update(NetpowerctrlApplication.getDataController().configuredDevices);
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
                OutletInfo oi = adapter_available.getItem(position);
                adapter_available.removeAt(position);
                adapter_included.addOutlet(oi);
                adapter_included.notifyDataSetChanged();
                invalidateOptionsMenu();
            }
        });

        if (isSceneNotShortcut) {
            if (isLoaded) {
                setTitle(R.string.title_scene_edit);
                setIcon(Scenes.loadIcon(this, scene));
            } else
                setTitle(R.string.title_scene);
        } else {
            show_mainWindow.setVisibility(View.VISIBLE);
            enable_feedback.setVisibility(View.VISIBLE);
            setTitle(R.string.title_shortcut);
        }

        invalidateOptionsMenu();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.shortcut_create_menu, menu);
        return true;
    }

    private void save_and_close() {
        // Generate list of checked items
        scene.sceneOutlets = adapter_included.getDeviceCommands();
        if (scene.sceneName.isEmpty() || scene.length() == 0)
            return;

        if (isSceneNotShortcut) {
            if (scene_icon != null)
                Scenes.saveIcon(this, scene, Scenes.getResizedBitmap(this, scene_icon, 128, 128));
            else
                Scenes.saveIcon(this, scene, null);
            NetpowerctrlActivity.instance.getScenesAdapter().addScene(this, scene);
        } else {
            Intent extra = Shortcuts.createShortcutExecutionIntent(ShortcutCreatorActivity.this,
                    scene, show_mainWindow.isChecked(), enable_feedback.isChecked());
            // Return result
            Intent shortcut;
            if (scene_icon != null) {
                shortcut = Shortcuts.createShortcut(extra, scene.sceneName,
                        Scenes.getResizedBitmapIconSize(this, scene_icon));
            } else
                shortcut = Shortcuts.createShortcut(ShortcutCreatorActivity.this,
                        extra, scene.sceneName);
            setResult(RESULT_OK, shortcut);
        }
        finish();
    }

    /*
     * ActionBar icon clicked
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_icon:
                show_select_icon_dialog();
                return true;
            case R.id.menu_name:
                Scenes.requestName(this, scene, this);
                return true;
            case R.id.menu_switch_all_on:
                adapter_included.switchAll(true);
                return true;
            case R.id.menu_switch_all_off:
                adapter_included.switchAll(false);
                return true;
            case R.id.menu_switch_all_toogle:
                adapter_included.toggleAll();
                return true;
            case R.id.menu_switch_all_ignore:
                adapter_included.clear();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void show_select_icon_dialog() {
        AssetManager assetMgr = this.getAssets();
        Bitmap[] list_of_icons = null;
        String[] list_of_icon_paths = null;
        try {
            list_of_icon_paths = assetMgr.list("scene_icons");
            list_of_icons = new Bitmap[list_of_icon_paths.length];
            int c = 0;
            for (String filename : list_of_icon_paths) {
                list_of_icons[c] = BitmapFactory.decodeStream(assetMgr.open("scene_icons/" + filename));
                ++c;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        final Bitmap[] list_of_icons_dialog = list_of_icons;

        ArrayAdapterWithIcons adapter = new ArrayAdapterWithIcons(this,
                android.R.layout.select_dialog_item,
                android.R.id.text1, new ArrayList<ArrayAdapterWithIcons.Item>());
        adapter.items.add(new ArrayAdapterWithIcons.Item(getString(R.string.scene_icon_default), null));
        adapter.items.add(new ArrayAdapterWithIcons.Item(getString(R.string.scene_icon_select), null));

        if (list_of_icons != null)
            for (int i = 0; i < list_of_icons.length; ++i) {
                adapter.items.add(new ArrayAdapterWithIcons.Item(list_of_icon_paths[i],
                        new BitmapDrawable(getResources(), list_of_icons[i])));
            }

        AlertDialog.Builder select_icon_dialog = new AlertDialog.Builder(this);
        select_icon_dialog.setTitle(getString(R.string.scene_icon_title));
        select_icon_dialog.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (i == 0) {
                    setIcon(null);
                } else if (i == 1) {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.setType("image/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    int PICK_IMAGE = 1;
                    startActivityForResult(intent, PICK_IMAGE);
                } else {
                    setIcon(list_of_icons_dialog[i - 2]);
                }
                dialogInterface.dismiss();
            }
        });
        select_icon_dialog.create().show();
    }

    private void setIcon(Bitmap o) {
        scene_icon = o;
        if (o == null) {
            getActionBar().setIcon(getApplicationInfo().icon);
        } else
            getActionBar().setIcon(new BitmapDrawable(getResources(), o));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        if (resultCode == Activity.RESULT_OK) {
            Uri selectedImage = imageReturnedIntent.getData();
            try {
                setIcon(Scenes.getDrawableFromUri(this, selectedImage).getBitmap());
            } catch (IOException e) {
                Toast.makeText(this, getString(R.string.error_icon), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * This is called when the user clicks on remove-action on an included action of a scene.
     *
     * @param v
     * @param position
     */
    @Override
    public void onMenuItemClicked(View v, int position) {
        adapter_available.addOutlet(adapter_included.getItem(position));
        adapter_available.notifyDataSetChanged();
        adapter_included.removeAt(position);
        invalidateOptionsMenu();
    }

    /**
     * Called by the widget/object/dialog that is responsible for asking the
     * user for a new scene name after a name has been chosen.
     */
    @Override
    public void sceneNameChanged() {
        invalidateOptionsMenu();
    }

    /* Called whenever we call invalidateOptionsMenu(). Enables or disables the
     * "done" action bar button. */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (scene == null)
            return super.onPrepareOptionsMenu(menu);

        boolean en = (adapter_included.getCount() != 0) && scene.sceneName.length() > 0;
        btnDone.setEnabled(en);
        TextView text = (TextView) findViewById(R.id.hintText);
        if (adapter_included.getCount() == 0)
            text.setText(getString(R.string.error_scene_no_actions));
        else if (scene.sceneName.length() == 0)
            text.setText(getString(R.string.error_scene_no_name));
        text.setVisibility(en ? View.GONE : View.VISIBLE);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void setManipulatorObjects(int tag, final DynamicGridView view, OutletsCreateSceneAdapter adapter) {
        if (tag == OutletsSceneEditFragment.MANIPULATOR_TAG_AVAILABLE) {
            grid_available = view;
            adapter_available = adapter;
        } else if (tag == OutletsSceneEditFragment.MANIPULATOR_TAG_INCLUDED) {
            grid_included = view;
            adapter_included = adapter;
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