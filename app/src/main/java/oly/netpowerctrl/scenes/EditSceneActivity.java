package oly.netpowerctrl.scenes;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import com.wefika.flowlayout.FlowLayout;

import java.io.IOException;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.device_base.data.JSONHelper;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.executables.ExecutableAdapterItem;
import oly.netpowerctrl.executables.ExecutablesBaseAdapter;
import oly.netpowerctrl.executables.ExecutablesListAdapter;
import oly.netpowerctrl.executables.ExecutablesSourceDevicePorts;
import oly.netpowerctrl.groups.GroupCollection;
import oly.netpowerctrl.groups.GroupUtilities;
import oly.netpowerctrl.listen_service.ListenService;
import oly.netpowerctrl.main.NfcTagWriterActivity;
import oly.netpowerctrl.ui.RecyclerItemClickListener;
import oly.netpowerctrl.ui.notifications.InAppNotifications;
import oly.netpowerctrl.ui.widgets.FloatingActionButton;
import oly.netpowerctrl.utils.AndroidShortcuts;

/**
 * This activity is responsible for creating a "scene" either for the scene list
 * in the application or for a shortcut intent for the home-screen.
 */
public class EditSceneActivity extends ActionBarActivity implements LoadStoreIconData.IconSelected {
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
    public static final int REQUEST_CODE = 123123;
    public static int lastScenePosition = -1;
    // UI widgets
    Switch show_mainWindow;
    Switch enable_feedback;
    boolean[] checked;
    RecyclerViewWithAdapter<ExecutablesListAdapter> availableElements;
    RecyclerViewWithAdapter<SceneElementsAdapter> sceneElements;
    private FloatingActionButton btnOk;
    private ExecutablesListAdapter adapter_available;
    private SceneElementsAdapter adapter_included;
    // Scene and flag variables
    private Scene scene;
    private boolean isLoaded = false;
    private boolean isSceneNotShortcut = false;
    // Scene Icon
    private Bitmap scene_icon;
    private ImageView btnSceneFav;
    private Button btnSceneAddHomescreen;
    private Button btnNFC;
    private FlowLayout groups_layout;
    private Toast toast;

    /*
 * ActionBar icon clicked
 */
//    public boolean onOptionsItemSelected(MenuItem item) {
//        SceneElementsAdapter adapter_included = mAdapter;
//        switch (item.getItemId()) {
//            case R.id.menu_switch_all_on:
//                adapter_included.switchAllOn();
//                return true;
//            case R.id.menu_switch_all_off:
//                adapter_included.switchAllOff();
//                return true;
//            case R.id.menu_switch_all_toogle:
//                adapter_included.toggleAll();
//                return true;
//            case R.id.menu_switch_all_ignore:
//                adapter_included.clear();
//                adapter_available.getSource().updateNow();
//                return true;
//            default:
//                return super.onOptionsItemSelected(item);
//        }
//    }

    @SuppressLint("ShowToast")
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

        // set content view, get references to widgets
        setContentView(R.layout.activity_create_scene);

        toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        ExecutablesSourceDevicePorts s = new ExecutablesSourceDevicePorts(null);
        adapter_available = new ExecutablesListAdapter(false, s, LoadStoreIconData.iconLoadingThread, true);
        adapter_included = new SceneElementsAdapter();

        sceneElements = new RecyclerViewWithAdapter<>(this, findViewById(R.id.scroll_vertical),
                findViewById(R.id.included), adapter_included, R.string.scene_create_include_twopane);
        availableElements = new RecyclerViewWithAdapter<>(this, findViewById(R.id.scroll_vertical),
                findViewById(R.id.available), adapter_available, R.string.scene_create_helptext_available);

        btnOk = (FloatingActionButton) findViewById(R.id.btnOk);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                save_and_close();
            }
        });

        show_mainWindow = (Switch) findViewById(R.id.shortcut_show_mainwindow);
        enable_feedback = (Switch) findViewById(R.id.shortcut_enable_feedback);
        groups_layout = (FlowLayout) findViewById(R.id.groups_layout);

        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestName(scene);
            }
        });

        findViewById(R.id.scene_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LoadStoreIconData.show_select_icon_dialog(EditSceneActivity.this, "scene_icons", EditSceneActivity.this, null);
            }
        });

        btnNFC = (Button) findViewById(R.id.btnNFC);
        btnNFC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(EditSceneActivity.this, NfcTagWriterActivity.class);
                intent.putExtra("uuid", scene.uuid);
                intent.putExtra("name", scene.sceneName);
                startActivity(intent);
            }
        });

        btnSceneFav = (ImageView) findViewById(R.id.btnSceneFavourite);
        btnSceneFav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean newFavStatus = !scene.isFavourite();
                if (isLoaded)
                    AppData.getInstance().sceneCollection.setFavourite(scene, newFavStatus);
                else
                    scene.setFavourite(newFavStatus);
                updateFavButton();
                toast.setText(newFavStatus ? R.string.scene_make_favourite : R.string.scene_remove_favourite);
                InAppNotifications.moveToastNextToView(toast, getResources(), view, false);
                toast.show();
            }
        });

        btnSceneAddHomescreen = (Button) findViewById(R.id.btnSceneAddHomescreen);
        btnSceneAddHomescreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //noinspection ConstantConditions
                AndroidShortcuts.createHomeIcon(EditSceneActivity.this, scene);
                Toast.makeText(EditSceneActivity.this, R.string.scene_add_homescreen_success, Toast.LENGTH_SHORT).show();
            }
        });

        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                loadContent();
            }
        }, 50);
    }

    private void updateFavButton() {
        boolean isFav = scene.isFavourite();
        Resources r = getResources();
        btnSceneFav.setImageDrawable(isFav ? r.getDrawable(android.R.drawable.btn_star_big_on)
                : r.getDrawable(android.R.drawable.btn_star_big_off));
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
                onNameChanged();
            }
        });

        alert.setNegativeButton(this.getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        alert.show();
    }

    @Override
    public void setIcon(Object context_object, Bitmap icon) {
        scene_icon = icon;
        if (icon == null) {
            icon = BitmapFactory.decodeResource(getResources(), getApplicationInfo().icon);
        }
        Bitmap result = Bitmap.createBitmap(icon.getWidth(), icon.getHeight(), Bitmap.Config.ARGB_8888);
        Bitmap rounder = Bitmap.createBitmap(icon.getWidth(), icon.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas resultCanvas = new Canvas(result);
        Canvas canvas = new Canvas(rounder);

        RectF rect = new RectF(0.0f, 0.0f, icon.getWidth(), icon.getHeight());

        // We're going to apply this paint eventually using a porter-duff xfer mode.
        // This will allow us to only overwrite certain pixels. RED is arbitrary. This
        // could be any color that was fully opaque (alpha = 255)
        Paint xferPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        xferPaint.setColor(Color.RED);

        // We're just reusing xferPaint to paint a normal looking rounded box, the 20.f
        // is the amount we're rounding by.
        float min = Math.min(icon.getWidth() / 2, icon.getHeight() / 2);
        canvas.drawCircle(min, min, min, xferPaint);
        xferPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        min -= 10;

        Paint mButtonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mButtonPaint.setStyle(Paint.Style.FILL);
        mButtonPaint.setColor(Color.WHITE);
        mButtonPaint.setShadowLayer(10f, 5f, 5f, Color.GRAY);

        resultCanvas.drawCircle(min, min, min, mButtonPaint);
        resultCanvas.drawBitmap(icon, 0, 0, null);
        resultCanvas.drawBitmap(rounder, 0, 0, xferPaint);

        ((ImageView) findViewById(R.id.scene_image)).setImageBitmap(result);

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
        lastScenePosition = -1;
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
                        lastScenePosition = AppData.getInstance().sceneCollection.indexOf(scene);
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

        FloatingActionButton btnRemove = (FloatingActionButton) findViewById(R.id.btnRemove);
        if (scene == null)
            btnRemove.hide(true);
        else
            btnRemove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AppData.getInstance().sceneCollection.removeScene(scene);
                    setResult(RESULT_OK, null);
                    finish();
                }
            });


        // Load current set of available outlets
        removeIncludedFromAvailable(adapter_available, adapter_included);

        if (scene == null) {
            scene = new Scene();
        }

        // Add click listener for the remove button on each included action
        sceneElements.setOnItemClickListener(new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public boolean onItemClick(View view, int position, boolean isLongClick) {
                if (view.getId() != R.id.outlet_list_close)
                    return false;
                adapter_available.addItem(adapter_included.take(position).getExecutable(), DevicePort.TOGGLE);
                onNameChanged();
                return true;
            }
        }, null));

        // Add click listener to available list to move the clicked action
        // to the included list.
        availableElements.setOnItemClickListener(new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public boolean onItemClick(View view, int position, boolean isLongClick) {
                DevicePort oi = (DevicePort) adapter_available.getItem(position).getExecutable();
                if (oi == null)
                    return false;
                adapter_available.removeAt(position);
                adapter_included.addItem(oi, DevicePort.TOGGLE);
                onNameChanged();
                return true;
            }
        }, null));

        if (isSceneNotShortcut) {
            Bitmap bitmap = null;
            if (isLoaded)
                bitmap = LoadStoreIconData.loadBitmap(this, scene.uuid, LoadStoreIconData.IconType.SceneIcon, LoadStoreIconData.IconState.OnlyOneState);
            setIcon(this, bitmap);
        }

        onNameChanged();

        if (show_mainWindow == null || scene == null)
            return;

        if (!isSceneNotShortcut) {
            show_mainWindow.setVisibility(View.VISIBLE);
            enable_feedback.setVisibility(View.VISIBLE);
            isLoaded = false;
        }

        checked = GroupUtilities.addGroupCheckBoxesToLayout(this, groups_layout, scene.groups);

        btnSceneAddHomescreen.setVisibility(isLoaded ? View.VISIBLE : View.GONE);
        btnNFC.setVisibility(isLoaded ? View.VISIBLE : View.GONE);
        updateFavButton();
    }

    private void removeIncludedFromAvailable(ExecutablesBaseAdapter available, SceneElementsAdapter included) {
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
        List<SceneItem> sceneItems = SceneFactory.scenesFromList(adapter_included);
        if (scene.sceneName.isEmpty()) {
            Toast.makeText(this, R.string.error_scene_no_name, Toast.LENGTH_SHORT).show();
            return;
        }

        if (sceneItems.size() == 0) {
            Toast.makeText(this, R.string.error_scene_no_actions, Toast.LENGTH_SHORT).show();
            return;
        }

        scene.sceneItems = sceneItems;
        scene.setMaster(adapter_included.getMaster());

        if (isSceneNotShortcut) {
            SceneCollection sceneCollection = AppData.getInstance().sceneCollection;
            LoadStoreIconData.saveIcon(this, LoadStoreIconData.resizeBitmap(this, scene_icon, 128, 128), scene.uuid,
                    LoadStoreIconData.IconType.SceneIcon, LoadStoreIconData.IconState.StateUnknown);

            GroupCollection groupCollection = AppData.getInstance().groupCollection;
            scene.groups.clear();
            for (int i = 0; i < checked.length; ++i) {
                if (!checked[i]) {
                    continue;
                }
                scene.groups.add(groupCollection.get(i).uuid);
            }

            lastScenePosition = sceneCollection.add(scene, false);
            setResult(RESULT_OK, null);
        } else {
            Intent extra = AndroidShortcuts.createShortcutExecutionIntent(EditSceneActivity.this,
                    scene, show_mainWindow.isChecked(), enable_feedback.isChecked());
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

    private void updateSaveButton() {
        boolean en = (adapter_included.getItemCount() != 0) && scene.sceneName.length() > 0;
        Resources r = getResources();
        btnOk.setDrawable(en ? r.getDrawable(android.R.drawable.ic_menu_save) :
                r.getDrawable(R.drawable.btn_save_disabled));
    }

    /**
     * Called by the widget/object/dialog that is responsible for asking the
     * user for a new scene name after a name has been chosen.
     */
    public void onNameChanged() {
        String sceneName = scene.sceneName;

        if (sceneName.length() == 0)
            sceneName = getString(R.string.title_scene_no_name);

        //TextView textView = (TextView) findViewById(R.id.scene_name);
        //textView.setText(sceneName);
        getSupportActionBar().setTitle(sceneName);

        if (isSceneNotShortcut) {
            if (isLoaded) {
                setTitle(R.string.title_scene_edit);
            } else
                setTitle(R.string.title_scene_create);
        } else {
            setTitle(R.string.title_shortcut);
        }

        //noinspection ConstantConditions
//        ActionBar actionBar = getSupportActionBar();
//
//        if (adapter_included.getItemCount() == 0)
//            actionBar.setSubtitle(getString(R.string.error_scene_no_actions));
//        else if (scene.sceneName.length() == 0)
//            actionBar.setSubtitle(getString(R.string.error_scene_no_name));
//        else
//            actionBar.setSubtitle("");

        updateSaveButton();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        LoadStoreIconData.activityCheckForPickedImage(this, this, requestCode, resultCode, imageReturnedIntent);
    }
}