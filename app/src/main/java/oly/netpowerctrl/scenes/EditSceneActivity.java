package oly.netpowerctrl.scenes;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.wefika.flowlayout.FlowLayout;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.main.NfcTagWriterActivity;
import oly.netpowerctrl.ui.RecyclerItemClickListener;
import oly.netpowerctrl.ui.notifications.InAppNotifications;
import oly.netpowerctrl.ui.widgets.FloatingActionButton;
import oly.netpowerctrl.utils.AndroidShortcuts;
import oly.netpowerctrl.utils.AnimationController;
import oly.netpowerctrl.utils.MutableBoolean;

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
    public static final String RESULT_SCENE_BITMAP_FILES_TEMP = "scene_bitmap_file_temps";
    public static final String RESULT_SCENE_BITMAP_FILES_DEST = "scene_bitmap_file_dests";
    public static final String RESULT_SCENE_BITMAP_STATES = "scene_bitmap_states";
    public static final String RESULT_SCENE_REMOVE_UID = "scene_remove_uuid";
    public static final String RESULT_SCENE_IS_FAVOURITE = "scene_is_favourite";
    public static final String RESULT_SCENE_UUID = "scene_uuid";
    public static final String RESULT_ACTION_UUID = "action_uuid";
    public static final String RESULT_ACTION_COMMAND = "action_command";
    public static final int REQUEST_CODE = 123123;
    // UI widgets
    CheckBox show_mainWindow;
    CheckBox enable_feedback;
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
    private Bitmap scene_icon_nostate;
    private Bitmap scene_icon_off;
    private Bitmap scene_icon_on;
    private ImageView btnSceneFav;
    private Button btnSceneAddHomescreen;
    private Button btnNFC;
    private FlowLayout groups_layout;
    private Toast toast;
    private boolean iconMenuVisible = false;
    private boolean isFavourite;

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
        setTitle("");

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

        show_mainWindow = (CheckBox) findViewById(R.id.shortcut_show_mainwindow);
        enable_feedback = (CheckBox) findViewById(R.id.shortcut_enable_feedback);
        groups_layout = (FlowLayout) findViewById(R.id.groups_layout);

        View.OnClickListener iconClick = new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (!isSceneNotShortcut || iconMenuVisible) {
                    AnimationController.animatePress(view);
                    LoadStoreIconData.show_select_icon_dialog(EditSceneActivity.this, "scene_icons", EditSceneActivity.this, view);
                    return;
                }

                iconMenuVisible = true;
                AnimationController.animateViewInOut(findViewById(R.id.executable_icons), true, false);
            }
        };

        findViewById(R.id.scene_image).setOnClickListener(iconClick);
        findViewById(R.id.scene_image_on).setOnClickListener(iconClick);
        findViewById(R.id.scene_image_off).setOnClickListener(iconClick);

        findViewById(R.id.close_icons).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                iconMenuVisible = false;
                AnimationController.animateViewInOut(findViewById(R.id.executable_icons), false, false);
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
                isFavourite = !isFavourite;
                updateFavButton();
                toast.setText(isFavourite ? R.string.scene_make_favourite : R.string.scene_remove_favourite);
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

        EditText nameEdit = (EditText) findViewById(R.id.scene_name);
        nameEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scene.sceneName = s.toString().trim();
                updateSaveButton();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void afterTextChanged(Editable s) {
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

    @Override
    public void onBackPressed() {
        MainActivity.getNavigationController().onBackPressed();
    }

    private void updateFavButton() {
        Resources r = getResources();
        btnSceneFav.setImageDrawable(isFavourite ? r.getDrawable(android.R.drawable.btn_star_big_on)
                : r.getDrawable(android.R.drawable.btn_star_big_off));
    }

    @Override
    public void setIcon(Object context_object, Bitmap icon) {
        MutableBoolean isDefault = new MutableBoolean();
        switch (((View) context_object).getId()) {
            case R.id.scene_image:
                if (icon == null)
                    icon = LoadStoreIconData.loadBitmap(this, scene.uuid, LoadStoreIconData.IconState.OnlyOneState, isDefault);
                scene_icon_nostate = isDefault.value ? null : icon;
                break;
            case R.id.scene_image_off:
                if (icon == null)
                    icon = LoadStoreIconData.loadBitmap(this, scene.uuid, LoadStoreIconData.IconState.StateOff, isDefault);
                scene_icon_off = isDefault.value ? null : icon;
                break;
            case R.id.scene_image_on:
                if (icon == null)
                    icon = LoadStoreIconData.loadBitmap(this, scene.uuid, LoadStoreIconData.IconState.StateOn, isDefault);
                scene_icon_on = isDefault.value ? null : icon;
                break;
        }

        Bitmap result = Bitmap.createBitmap(icon.getWidth(), icon.getHeight(), Bitmap.Config.ARGB_8888);
        Bitmap rounder = Bitmap.createBitmap(icon.getWidth(), icon.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas resultCanvas = new Canvas(result);
        Canvas canvas = new Canvas(rounder);

        //RectF rect = new RectF(0.0f, 0.0f, icon.getWidth(), icon.getHeight());

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

        int backColor;
        if (SharedPrefs.getInstance().isDarkTheme())
            backColor = Palette.generate(icon).getDarkVibrantColor(getResources().getColor(R.color.colorBackgroundDark));
        else
            backColor = Palette.generate(icon).getVibrantColor(getResources().getColor(R.color.colorBackgroundLight));

        Paint mButtonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mButtonPaint.setStyle(Paint.Style.FILL);
        mButtonPaint.setColor(backColor);
        mButtonPaint.setShadowLayer(10f, 5f, 5f, Color.GRAY);

        resultCanvas.drawCircle(min, min, min, mButtonPaint);
        resultCanvas.drawBitmap(icon, 0, 0, null);
        resultCanvas.drawBitmap(rounder, 0, 0, xferPaint);

        ((ImageView) context_object).setImageBitmap(result);
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

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void loadContent() {
        Intent it = getIntent();
        if (it != null) {
            Bundle extra = it.getExtras();
            if (extra != null) {
                isSceneNotShortcut = extra.getBoolean(EDIT_SCENE_NOT_SHORTCUT);

                String scene_string = extra.getString(LOAD_SCENE, null);
                if (scene_string != null) {
                    try {
                        scene = Scene.loadFromJson(JSONHelper.getReader(scene_string));
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
                    Intent intent = new Intent();
                    intent.putExtra(RESULT_SCENE_REMOVE_UID, scene.getUid());
                    setResult(RESULT_OK, intent);
                    finish();
                }
            });


        // Load current set of available outlets
        removeIncludedFromAvailable(adapter_available, adapter_included);

        if (scene == null) {
            scene = Scene.createNewSzene();
        }

        isFavourite = AppData.getInstance().favCollection.isFavourite(scene.getUid());

        // Add click listener for the remove button on each included action
        sceneElements.setOnItemClickListener(new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public boolean onItemClick(View view, int position, boolean isLongClick) {
                if (view.getId() != R.id.outlet_list_close)
                    return false;
                adapter_available.addItem(adapter_included.take(position).getExecutable(), DevicePort.TOGGLE);
                updateSaveButton();
                return true;
            }
        }, null));

        // Add click listener to available list to move the clicked action
        // to the included list.
        availableElements.setOnItemClickListener(new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public boolean onItemClick(View view, int position, boolean isLongClick) {
                DevicePort devicePort = (DevicePort) adapter_available.getItem(position).getExecutable();
                if (devicePort == null)
                    return false;
                if (AppData.getInstance().findDevicePort(devicePort.getUid()) != devicePort) {
                    throw new RuntimeException("DevicePort not equal!");
                }
                adapter_available.removeAt(position);
                adapter_included.addItem(devicePort, DevicePort.TOGGLE);
                updateSaveButton();
                return true;
            }
        }, null));

        if (isSceneNotShortcut) {
            setIcon(findViewById(R.id.scene_image), null);
            setIcon(findViewById(R.id.scene_image_off), null);
            setIcon(findViewById(R.id.scene_image_on), null);
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
        List<SceneItem> sceneItems = SceneFactory.sceneItemsFromList(adapter_included);
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

            GroupCollection groupCollection = AppData.getInstance().groupCollection;
            scene.groups.clear();
            for (int i = 0; i < checked.length; ++i) {
                if (!checked[i]) {
                    continue;
                }
                scene.groups.add(groupCollection.get(i).uuid);
            }

            Intent intent = new Intent();
            intent.putExtra(RESULT_SCENE_JSON, scene.toString());
            intent.putExtra(RESULT_SCENE_IS_FAVOURITE, isFavourite);

            List<String> tempFilenames = new ArrayList<>();
            List<String> tempFilenameStates = new ArrayList<>();
            List<String> bitmapFileNames = new ArrayList<>();

            // Save all icons that are setup.
            File tempFilename = LoadStoreIconData.saveTempIcon(this, LoadStoreIconData.resizeBitmap(this, scene_icon_nostate, 128, 128));
            if (tempFilename != null) {
                tempFilenames.add(tempFilename.toString());
                tempFilenameStates.add(LoadStoreIconData.IconState.StateUnknown.name());
                bitmapFileNames.add(LoadStoreIconData.getFilename(this, scene.uuid,
                        LoadStoreIconData.IconState.StateUnknown).toString());
            }

            tempFilename = LoadStoreIconData.saveTempIcon(this, LoadStoreIconData.resizeBitmap(this, scene_icon_off, 128, 128));
            if (tempFilename != null) {
                tempFilenames.add(tempFilename.toString());
                tempFilenameStates.add(LoadStoreIconData.IconState.StateOff.name());
                bitmapFileNames.add(LoadStoreIconData.getFilename(this, scene.uuid,
                        LoadStoreIconData.IconState.StateOff).toString());
            }

            tempFilename = LoadStoreIconData.saveTempIcon(this, LoadStoreIconData.resizeBitmap(this, scene_icon_on, 128, 128));
            if (tempFilename != null) {
                tempFilenames.add(tempFilename.toString());
                tempFilenameStates.add(LoadStoreIconData.IconState.StateOn.name());
                bitmapFileNames.add(LoadStoreIconData.getFilename(this, scene.uuid,
                        LoadStoreIconData.IconState.StateOn).toString());
            }

            intent.putExtra(RESULT_SCENE_BITMAP_FILES_TEMP, tempFilenames.toArray());
            intent.putExtra(RESULT_SCENE_BITMAP_FILES_DEST, bitmapFileNames.toArray());
            intent.putExtra(RESULT_SCENE_BITMAP_STATES, tempFilenameStates.toArray());
            setResult(RESULT_OK, intent);
        } else {
            Intent extra = AndroidShortcuts.createShortcutExecutionIntent(EditSceneActivity.this,
                    scene, show_mainWindow.isChecked(), enable_feedback.isChecked());
            // Return result
            Intent shortcut;
            if (scene_icon_nostate != null) {
                shortcut = AndroidShortcuts.createShortcut(extra, scene.sceneName,
                        LoadStoreIconData.resizeBitmap(this, scene_icon_nostate));
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

//        if (sceneName.length() == 0)
//            sceneName = getString(R.string.title_scene_no_name);

        TextView textView = (TextView) findViewById(R.id.scene_name);
        textView.setText(sceneName);
        //getSupportActionBar().setTitle(sceneName);

        ActionBar actionBar = getSupportActionBar();
        if (isSceneNotShortcut) {
            if (isLoaded) {
                actionBar.setSubtitle(R.string.title_scene_edit);
            } else
                actionBar.setSubtitle(R.string.title_scene_create);
        } else {
            actionBar.setSubtitle(R.string.title_shortcut);
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