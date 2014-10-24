package oly.netpowerctrl.scenes;

import android.app.FragmentManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;

import java.io.IOException;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.IconDeferredLoadingThread;
import oly.netpowerctrl.data.JSONHelper;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.device_ports.DevicePort;
import oly.netpowerctrl.device_ports.DevicePortSourceConfigured;
import oly.netpowerctrl.device_ports.DevicePortsBaseAdapter;
import oly.netpowerctrl.device_ports.DevicePortsListAdapter;
import oly.netpowerctrl.device_ports.ExecutableAdapterItem;
import oly.netpowerctrl.listen_service.ListenService;
import oly.netpowerctrl.utils.AndroidShortcuts;
import oly.netpowerctrl.utils.RecyclerItemClickListener;
import oly.netpowerctrl.utils.controls.ActivityWithIconCache;
import oly.netpowerctrl.utils.controls.onListItemElementClicked;

/**
 * This activity is responsible for creating a "scene" either for the scene list
 * in the application or for a shortcut intent for the home-screen.
 */
public class EditSceneActivity extends ActionBarActivity
        implements onListItemElementClicked, ActivityWithIconCache, onEditSceneBasicsChanged {

    /**
     * We pass arguments to this activity via the intent extra bundle.
     * Define some constants here.
     */
    public static final String EDIT_SCENE_NOT_SHORTCUT = "scenes";
    public static final String LOAD_SCENE = "load";
    public static final String RESULT_SCENE_JSON = "scene";
    public static final String RESULT_SCENE_UUID = "scene_uuid";
    public static final String RESULT_ACTION_UUID = "action_uuid";
    public static final String RESULT_ACTION_COMMAND = "action_command";
    private final IconDeferredLoadingThread mIconCache = new IconDeferredLoadingThread();
    boolean reInitUIDone = false;
    private View btnDone;
    private DevicePortsListAdapter adapter_available;
    private SceneElementsAdapter adapter_included;
    // Scene and flag variables
    private Scene scene;
    private boolean isLoaded = false;
    private boolean isSceneNotShortcut = false;
    // Scene Icon
    private Bitmap scene_icon;
    private EditSceneBasicFragment fragment_basics;
    private EditSceneIncludedFragment fragment_included;
    private EditSceneAvailableFragment fragment_available;
    private boolean twoPane = false;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // set theme based on user preference
        if (SharedPrefs.getInstance().isDarkTheme()) {
            setTheme(R.style.Theme_CustomDarkTheme);
        } else {
            setTheme(R.style.Theme_CustomLightTheme);
        }

        // Default result
        setResult(RESULT_CANCELED, null);

        mIconCache.start();
        reInitUI();

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
        FragmentManager m = getFragmentManager();
        if (fragment_available != null && fragment_included != null)
            m.beginTransaction().remove(fragment_available).remove(fragment_included).commit();

        // set content view, get references to widgets
        setContentView(R.layout.activity_create_scene);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        // Show the included and available list for the scene. Both are fragments. Either in a
        // viewPager (small width) or both visible at the same time.
        // If there is a viewPager, use that. Otherwise we show both fragments next to each other.
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        if (pager != null) {
            twoPane = false;
            EditScenePagerAdapter pagerAdapter = new EditScenePagerAdapter(this, m);
            fragment_basics = pagerAdapter.getFragmentBasic();
            fragment_included = pagerAdapter.getFragmentIncluded();
            fragment_available = pagerAdapter.getFragmentAvailable();
            pager.setAdapter(pagerAdapter);
        } else {
            twoPane = true;
            fragment_basics = (EditSceneBasicFragment) m.findFragmentById(R.id.scene_edit_fragment0);
            fragment_included = (EditSceneIncludedFragment) m.findFragmentById(R.id.scene_edit_fragment1);
            fragment_available = (EditSceneAvailableFragment) m.findFragmentById(R.id.scene_edit_fragment2);
        }

        if (reInitUIDone)
            return;
        reInitUIDone = true;

        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                loadContent();
            }
        }, 50);
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
        AppData.useAppData();
        ListenService.useService(getApplicationContext(), false, false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ListenService.stopUseService();
    }

    private void loadContent() {
        DevicePortSourceConfigured s = new DevicePortSourceConfigured();
        adapter_available = new DevicePortsListAdapter(false, s, mIconCache, true);
        adapter_included = new SceneElementsAdapter(this);

        Intent it = getIntent();
        if (it != null) {
            Bundle extra = it.getExtras();
            if (extra != null) {
                isSceneNotShortcut = extra.getBoolean(EDIT_SCENE_NOT_SHORTCUT);

                String scene_string = extra.getString(LOAD_SCENE, null);
                if (scene_string != null) {
                    try {
                        scene = new Scene();
                        scene.load(JSONHelper.getReader(scene_string));
                    } catch (IOException | ClassNotFoundException ignored) {
                        scene = null;
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
        removeIncludedFromAvailable(adapter_available, adapter_included);

        if (scene == null) {
            scene = new Scene();
        }

        // Add click listener for the remove button on each included action
        adapter_included.setListItemElementClickedListener(this);

        // Add click listener to available list to move the clicked action
        // to the included list.
        fragment_available.setOnItemClickListener(new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                DevicePort oi = (DevicePort) adapter_available.getItem(position).getExecutable();
                if (oi == null)
                    return;
                adapter_available.removeAt(position);
                adapter_included.addItem(oi, DevicePort.TOGGLE);
                onNameChanged();
            }
        }, null));

        if (isSceneNotShortcut && isLoaded) {
            onIconChanged(LoadStoreIconData.loadIcon(this, scene.uuid, LoadStoreIconData.IconType.SceneIcon, LoadStoreIconData.IconState.OnlyOneState));
        }

        onNameChanged();

        fragment_included.setTwoPaneFragment(twoPane);
        fragment_included.setAdapter(adapter_included);
        fragment_included.setAdapterAvailable(adapter_available);
        fragment_available.setAdapter(adapter_available);
        fragment_basics.setScene(scene, isSceneNotShortcut, isLoaded);
        fragment_basics.setOnBasicsChangedListener(this);
    }

    private void removeIncludedFromAvailable(DevicePortsBaseAdapter available, SceneElementsAdapter included) {
        // ToBeRemoved will be an ordered list of indecies to be removed
        int[] toBeRemoved = new int[available.mItems.size()];
        int lenToBeRemoved = 0;
        for (int index = 0; index < available.mItems.size(); ++index) {
            for (ExecutableAdapterItem adapter_list_item : included.mItems) {
                if (adapter_list_item.getExecutable() != null &&
                        adapter_list_item.getExecutableUid().equals(available.mItems.get(index).getExecutableUid())) {
                    toBeRemoved[lenToBeRemoved] = index;
                    lenToBeRemoved++;
                }
            }
        }
        // Remove now
        for (int i = lenToBeRemoved - 1; i >= 0; --i)
            available.removeAt(toBeRemoved[i]);
    }

    private void save_and_close() {
        // Generate list of checked items
        scene.sceneItems = SceneFactory.scenesFromList(adapter_included);
        scene.setMaster(adapter_included.getMaster());
        if (scene.sceneName.isEmpty() || scene.length() == 0)
            return;

        if (isSceneNotShortcut) {
            AppData.getInstance().sceneCollection.setBitmap(this, scene, scene_icon);
            AppData.getInstance().sceneCollection.add(scene);
        } else {
            Intent extra = AndroidShortcuts.createShortcutExecutionIntent(EditSceneActivity.this,
                    scene, fragment_basics.show_mainWindow.isChecked(), fragment_basics.enable_feedback.isChecked());
            // Return result
            Intent shortcut;
            if (scene_icon != null) {
                shortcut = AndroidShortcuts.createShortcut(extra, scene.sceneName,
                        LoadStoreIconData.resizeBitmap(this, scene_icon));
            } else
                shortcut = AndroidShortcuts.createShortcut(extra, scene.sceneName,
                        EditSceneActivity.this);
            setResult(RESULT_OK, shortcut);
        }
        finish();
    }

    /**
     * This is called when the user clicks on remove-action on an included action of a scene.
     *
     * @param v
     * @param position
     */
    @Override
    public void onListItemElementClicked(View v, int position) {
        adapter_available.addItem(adapter_included.take(position).getExecutable(), DevicePort.TOGGLE);
        onNameChanged();
    }

    /* Called whenever we call invalidateOptionsMenu(). Enables or disables the
     * "done" action bar button. */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (scene == null)
            return super.onPrepareOptionsMenu(menu);

        boolean en = (adapter_included.getItemCount() != 0) && scene.sceneName.length() > 0;
        btnDone.setVisibility(en ? View.VISIBLE : View.GONE);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public IconDeferredLoadingThread getIconCache() {
        return mIconCache;
    }


    /**
     * Called by the widget/object/dialog that is responsible for asking the
     * user for a new scene name after a name has been chosen.
     */
    @Override
    public void onNameChanged() {
        String sceneName = scene.sceneName;

        if (sceneName.length() == 0)
            sceneName = getString(R.string.title_scene_no_name);

        if (isSceneNotShortcut) {
            if (isLoaded) {
                setTitle(getString(R.string.title_scene_edit, sceneName));
            } else
                setTitle(getString(R.string.title_scene_create, sceneName));
        } else {
            setTitle(getString(R.string.title_shortcut, sceneName));
        }

        //noinspection ConstantConditions
        ActionBar actionBar = getSupportActionBar();

        if (adapter_included.getItemCount() == 0)
            actionBar.setSubtitle(getString(R.string.error_scene_no_actions));
        else if (scene.sceneName.length() == 0)
            actionBar.setSubtitle(getString(R.string.error_scene_no_name));
        else
            actionBar.setSubtitle("");

        invalidateOptionsMenu();
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onIconChanged(Bitmap icon) {
        scene_icon = icon;
//        ActionBar actionBar = getSupportActionBar();
//        if (icon == null) {
//            actionBar.setIcon(getApplicationInfo().icon);
//        } else
//            actionBar.setIcon(new BitmapDrawable(getResources(), icon));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        LoadStoreIconData.activityCheckForPickedImage(this, fragment_basics, requestCode, resultCode, imageReturnedIntent);
    }
}