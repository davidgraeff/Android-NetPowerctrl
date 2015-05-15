package oly.netpowerctrl.main;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.rey.material.widget.FloatingActionButton;
import com.wefika.flowlayout.FlowLayout;

import java.util.Set;
import java.util.TreeSet;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.data.graphic.IconSelected;
import oly.netpowerctrl.data.graphic.IconState;
import oly.netpowerctrl.data.graphic.LoadStoreIconData;
import oly.netpowerctrl.data.graphic.SelectDrawableDialog;
import oly.netpowerctrl.data.graphic.Utils;
import oly.netpowerctrl.data.onServiceReady;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.executables.onNameChangeResult;
import oly.netpowerctrl.groups.Group;
import oly.netpowerctrl.groups.GroupCollection;
import oly.netpowerctrl.groups.GroupUtilities;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.scenes.Scene;
import oly.netpowerctrl.scenes.SceneElementsAddDialog;
import oly.netpowerctrl.scenes.SceneElementsAssigning;
import oly.netpowerctrl.scenes.SceneHelp;
import oly.netpowerctrl.ui.FragmentUtils;
import oly.netpowerctrl.ui.notifications.InAppNotifications;
import oly.netpowerctrl.utils.AndroidShortcuts;
import oly.netpowerctrl.utils.AnimationController;
import oly.netpowerctrl.utils.MutableBoolean;

/**
 * This activity is responsible for creating a "executable" either for the executable list
 * in the application or for a shortcut intent for the home-screen.
 */
public class EditActivity extends ActionBarActivity implements IconSelected, onNameChangeResult {
    /**
     * We pass arguments to this activity via the intent extra bundle und the result is passed via
     * setResult().
     * Define some constants here.
     */
    public static final String LOAD_UUID = "load_uuid";
    public static final String CREATE_SCENE = "create_scene";
    public static final String LOAD_ADAPTER_POSITION = "load_adapter_position";

    public static final int EDIT_TYPE_SHORTCUT = 0;
    public static final int EDIT_TYPE_SCENE = 1;
    public static final int EDIT_TYPE_DEVICE_PORT = 2;
    public static final int REQUEST_CODE = 123123;
    // UI widgets
    CompoundButton show_mainWindow;
    CompoundButton enable_feedback;
    CompoundButton chk_hide;
    private int mEditType = EDIT_TYPE_SHORTCUT;
    private Set<String> checked_groups = new TreeSet<>();
    private boolean isChanged = false;
    private FloatingActionButton btnSaveOrTrash;
    // If we are editing a scene, sceneElementsAssigning will be set up.
    private SceneElementsAssigning sceneElementsAssigning;
    // Scene and flag variables
    private Executable executable = null;
    private boolean isLoaded = false;
    // Scene Icon
    private Bitmap icon_nostate;
    private Bitmap icon_off;
    private Bitmap icon_on;
    // Options
    private ImageView btnFav;
    private Button btnSceneCopyHomescreen;
    private Button btnSceneLinkHomescreen;
    private Button btnNFC;
    private boolean isFavourite;
    // Groups
    private FlowLayout groups_layout;
    // Other
    private Toast toast;
    private boolean iconMenuVisible = false;
    private int load_adapter_position = -1;
    private View btnAdd;
    private ProgressDialog progressDialog;
    private View scene_items_view;

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
        setContentView(R.layout.activity_edit);
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

        scene_items_view = findViewById(R.id.items_container);

        btnAdd = findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SceneElementsAddDialog dialog = new SceneElementsAddDialog();
                dialog.setSceneElementsAssigning(sceneElementsAssigning);
                FragmentUtils.changeToDialog(EditActivity.this, dialog);
            }
        });

        CompoundButton.OnCheckedChangeListener updateSaveButtonOnChecked = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isChanged = true;
                updateSaveButton();
            }
        };

        show_mainWindow = (CompoundButton) findViewById(R.id.shortcut_show_mainwindow);
        show_mainWindow.setOnCheckedChangeListener(updateSaveButtonOnChecked);
        enable_feedback = (CompoundButton) findViewById(R.id.shortcut_enable_feedback);
        enable_feedback.setOnCheckedChangeListener(updateSaveButtonOnChecked);
        chk_hide = (CompoundButton) findViewById(R.id.chk_hide);
        chk_hide.setOnCheckedChangeListener(updateSaveButtonOnChecked);

        groups_layout = (FlowLayout) findViewById(R.id.groups_layout);
        Button btnGroupAdd = (Button) findViewById(R.id.btnAddGroup);
        btnGroupAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final DataService dataService = DataService.getService();
                GroupUtilities.createGroup(EditActivity.this, dataService.groups,
                        new GroupUtilities.GroupCreatedCallback() {
                            @Override
                            public void onGroupCreated(Group group) {
                                updateGroups(dataService.groups);
                            }
                        });
            }
        });


        View.OnClickListener iconClick = new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (iconMenuVisible || mEditType == EDIT_TYPE_SHORTCUT) {
                    AnimationController.animatePress(view);
                    DialogFragment f = SelectDrawableDialog.createSelectDrawableDialog("widget_icons", EditActivity.this, view);
                    FragmentUtils.changeToDialog(EditActivity.this, f);
                    return;
                }

                isChanged = true;
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

        btnSaveOrTrash = (FloatingActionButton) findViewById(R.id.btnSaveOrTrash);
        btnSaveOrTrash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isLoaded && !isChanged) {
                    if (mEditType == EDIT_TYPE_SCENE) {
                        DataService dataService = DataService.getService();
                        dataService.executables.remove(executable);
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                } else
                    save_and_close();
            }
        });

        btnNFC = (Button) findViewById(R.id.btnNFC);
        btnNFC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(EditActivity.this, NfcTagWriterActivity.class);
                intent.putExtra("uid", executable.getUid());
                intent.putExtra("name", executable.getTitle());
                startActivity(intent);
            }
        });

        btnFav = (ImageView) findViewById(R.id.btnSceneFavourite);
        btnFav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isFavourite = !isFavourite;
                isChanged = true;
                updateFavButton();
                toast.setText(isFavourite ? R.string.scene_make_favourite : R.string.scene_remove_favourite);
                InAppNotifications.moveToastNextToView(toast, getResources(), view, false);
                toast.show();
            }
        });

        btnSceneCopyHomescreen = (Button) findViewById(R.id.btnSceneCopyHomescreen);
        btnSceneCopyHomescreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //noinspection ConstantConditions
                Intent extra = AndroidShortcuts.createExecutionCopyIntent(EditActivity.this, executable, false, false);
                AndroidShortcuts.createHomeIcon(EditActivity.this, executable, extra);
                Toast.makeText(EditActivity.this, R.string.scene_add_homescreen_success, Toast.LENGTH_SHORT).show();
            }
        });

        btnSceneLinkHomescreen = (Button) findViewById(R.id.btnSceneLinkHomescreen);
        btnSceneLinkHomescreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //noinspection ConstantConditions
                Intent extra = AndroidShortcuts.createExecutionLinkIntent(EditActivity.this, executable.getUid(), false, false);
                AndroidShortcuts.createHomeIcon(EditActivity.this, executable, extra);
                Toast.makeText(EditActivity.this, R.string.scene_add_homescreen_success, Toast.LENGTH_SHORT).show();
            }
        });

        DataService.observersServiceReady.register(new onServiceReady() {
            @Override
            public boolean onServiceReady(DataService service) {
                Intent it = getIntent();
                if (it != null) {
                    Bundle extra = it.getExtras();
                    if (extra != null) {
                        if (extra.containsKey(LOAD_UUID)) {
                            loadContent(service, extra);
                            return false;
                        } else if (extra.containsKey(CREATE_SCENE)) {
                            createContent(service, true);
                            return false;
                        }
                    }
                }
                createContent(service, false);
                return false;
            }

            @Override
            public void onServiceFinished(DataService service) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    private void updateFavButton() {
        btnFav.setImageDrawable(isFavourite ? ContextCompat.getDrawable(this, android.R.drawable.btn_star_big_on)
                : ContextCompat.getDrawable(this, android.R.drawable.btn_star_big_off));
    }

    @Override
    public void setIcon(Object context_object, Bitmap icon) {
        MutableBoolean isDefault = new MutableBoolean();
        switch (((View) context_object).getId()) {
            case R.id.scene_image:
                if (icon == null)
                    icon = LoadStoreIconData.loadBitmap(this, executable, IconState.OnlyOneState, isDefault);
                icon_nostate = isDefault.value ? null : icon;
                break;
            case R.id.scene_image_off:
                if (icon == null)
                    icon = LoadStoreIconData.loadBitmap(this, executable, IconState.StateOff, isDefault);
                icon_off = isDefault.value ? null : icon;
                break;
            case R.id.scene_image_on:
                if (icon == null)
                    icon = LoadStoreIconData.loadBitmap(this, executable, IconState.StateOn, isDefault);
                icon_on = isDefault.value ? null : icon;
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
    protected void onStop() {
        super.onStop();
    }

    private void createContent(final DataService dataService, boolean createNewScene) {
        executable = Scene.createNewScene();
        btnSceneCopyHomescreen.setVisibility(View.GONE);
        btnSceneLinkHomescreen.setVisibility(View.GONE);
        btnNFC.setVisibility(View.GONE);
        btnFav.setVisibility(View.GONE);
        mEditType = createNewScene ? EDIT_TYPE_SCENE : EDIT_TYPE_SHORTCUT;
        if (mEditType == EDIT_TYPE_SHORTCUT) {
            getSupportActionBar().setSubtitle(R.string.title_shortcut);
        } else {
            getSupportActionBar().setSubtitle(R.string.title_scene_create);
        }
        sceneElementsAssigning = new SceneElementsAssigning(dataService, (FlowLayout) findViewById(R.id.included),
                new SceneElementsAssigning.SceneElementsChanged() {
            @Override
            public void onSceneElementsChanged() {
                isChanged = true;
                updateSaveButton();
            }
        }, (Scene) executable);

        setIcon(findViewById(R.id.scene_image), null);
        setIcon(findViewById(R.id.scene_image_off), null);
        setIcon(findViewById(R.id.scene_image_on), null);

        prepareInterface(dataService);
    }

    private void loadContent(final DataService dataService, Bundle extra) {
        String executable_uuid = extra.getString(LOAD_UUID, null);
        load_adapter_position = extra.getInt(LOAD_ADAPTER_POSITION, -1);
        executable = dataService.executables.findByUID(executable_uuid);
        if (executable == null) {
            finish();
            return;
        }
        mEditType = executable instanceof Scene ? EDIT_TYPE_SCENE : EDIT_TYPE_DEVICE_PORT;

        if (mEditType == EDIT_TYPE_SCENE) {
            sceneElementsAssigning = new SceneElementsAssigning(dataService, (FlowLayout) findViewById(R.id.included),
                    new SceneElementsAssigning.SceneElementsChanged() {
                @Override
                public void onSceneElementsChanged() {
                    isChanged = true;
                    updateSaveButton();
                }
            }, (Scene) executable);
            getSupportActionBar().setSubtitle(R.string.title_scene_edit);
        } else {
            getSupportActionBar().setSubtitle(getString(R.string.outlet_edit_title, executable.getTitle()));
            scene_items_view.setVisibility(View.GONE);
            btnAdd.setVisibility(View.GONE);
        }

        isLoaded = true;
        isFavourite = dataService.favourites.isFavourite(executable.getUid());

        setIcon(findViewById(R.id.scene_image), null);
        setIcon(findViewById(R.id.scene_image_off), null);
        setIcon(findViewById(R.id.scene_image_on), null);

        btnSceneCopyHomescreen.setVisibility(View.VISIBLE);
        btnSceneLinkHomescreen.setVisibility(View.VISIBLE);
        btnNFC.setVisibility(View.VISIBLE);

        TextView textView = (TextView) findViewById(R.id.scene_name);
        textView.setText(executable.getTitle());

        updateFavButton();

        prepareInterface(dataService);
    }

    private void prepareInterface(DataService dataService) {

        View btnHelp = findViewById(R.id.btnHelp);
        final int help_res;
        final int help_title_res;
        if (mEditType == EDIT_TYPE_SHORTCUT) {
            show_mainWindow.setVisibility(View.VISIBLE);
            enable_feedback.setVisibility(View.VISIBLE);
            groups_layout.setVisibility(View.GONE);
            chk_hide.setVisibility(View.GONE);
            help_res = R.string.help_shortcut;
            help_title_res = R.string.shortcut_new_scene;
        } else {
            updateGroups(dataService.groups);
            help_res = mEditType == EDIT_TYPE_SCENE ? R.string.help_scene : R.string.help_edit_outlet;
            help_title_res = mEditType == EDIT_TYPE_SCENE ? R.string.scene_add : R.string.help_edit_outlet_title;
        }

        btnHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SceneHelp.showHelp(EditActivity.this, help_title_res, help_res);
            }
        });

        chk_hide.setVisibility(View.GONE);
        chk_hide.setChecked(executable.isHidden());

        EditText nameEdit = (EditText) findViewById(R.id.scene_name);
        nameEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void afterTextChanged(Editable s) {
                isChanged = true;
                updateSaveButton();
            }
        });

        isChanged = false;
        updateSaveButton();
    }

    private void updateGroups(GroupCollection groupCollection) {
        groups_layout.removeAllViews();
        GroupUtilities.addGroupCheckBoxesToLayout(groupCollection, groups_layout, executable.getGroupUIDs(), checked_groups,
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        isChanged = true;
                        updateSaveButton();
                    }
                });
    }

    private void save_and_close() {
        DataService dataService = DataService.getService();
        String newName = ((EditText) findViewById(R.id.scene_name)).getText().toString().trim();

        if (newName.isEmpty()) {
            Toast.makeText(this, R.string.error_scene_no_name, Toast.LENGTH_SHORT).show();
            return;
        }

        // Save name + scene elements
        if (mEditType != EDIT_TYPE_DEVICE_PORT) {
            if (!sceneElementsAssigning.hasElements()) {
                Toast.makeText(this, R.string.error_scene_no_actions, Toast.LENGTH_SHORT).show();
                return;
            }

            Scene scene = (Scene) executable;

            // Generate list of checked items
            sceneElementsAssigning.applyToScene(scene);

            if (scene.length() == 0) {
                Toast.makeText(this, R.string.error_scene_no_actions, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        executable.setTitle(newName, this);
        if (progressDialog != null) return;

        if (mEditType == EDIT_TYPE_SHORTCUT) {
            Intent extra = AndroidShortcuts.createExecutionCopyIntent(EditActivity.this,
                    executable, show_mainWindow.isChecked(), enable_feedback.isChecked());
            // Return result
            Intent shortcut;
            if (icon_nostate != null) {
                shortcut = AndroidShortcuts.createShortcut(extra, executable.getTitle(),
                        Utils.resizeBitmap(this, icon_nostate));
            } else
                shortcut = AndroidShortcuts.createShortcut(extra, executable.getTitle(),
                        EditActivity.this);
            setResult(RESULT_OK, shortcut);
            finish();
            return;
        }

        // If loaded: save fav + groups + hidden
        if (isLoaded) {
            dataService.favourites.setFavourite(executable.getUid(), isFavourite);

            executable.getGroupUIDs().clear();
            for (String groupUID : checked_groups) {
                executable.getGroupUIDs().add(groupUID);
            }
        }

        executable.setHidden(chk_hide.isChecked());
        dataService.executables.put(executable);

        Intent intent = new Intent();
        intent.putExtra(LOAD_ADAPTER_POSITION, load_adapter_position);

        if (mEditType == EDIT_TYPE_DEVICE_PORT) {
            dataService.executables.setExecutableBitmap(this,
                    (executable), icon_nostate, IconState.OnlyOneState);
            dataService.executables.setExecutableBitmap(this,
                    (executable), icon_off, IconState.StateOff);
            dataService.executables.setExecutableBitmap(this,
                    (executable), icon_on, IconState.StateOn);
        } else {
            // Save all icons that are setup.
            LoadStoreIconData.saveIcon(this, Utils.resizeBitmap(this, icon_nostate, 128, 128),
                    executable.getUid(), IconState.OnlyOneState);
            LoadStoreIconData.saveIcon(this, Utils.resizeBitmap(this, icon_off, 128, 128),
                    executable.getUid(), IconState.StateOff);
            LoadStoreIconData.saveIcon(this, Utils.resizeBitmap(this, icon_on, 128, 128),
                    executable.getUid(), IconState.StateOn);

            LoadStoreIconData.clearIconCache();
            dataService.executables.put(executable);
        }

        dataService.groups.executableToGroupAdded();

        setResult(RESULT_OK, intent);

        finish();
    }

    private void updateSaveButton() {
        if (isLoaded && !isChanged) {
            btnSaveOrTrash.setVisibility(mEditType == EDIT_TYPE_SCENE ? View.VISIBLE : View.GONE);
            btnSaveOrTrash.setIcon(ContextCompat.getDrawable(this, android.R.drawable.ic_menu_delete), btnSaveOrTrash.getIcon() != null);
            return;
        }
        String newName = ((EditText) findViewById(R.id.scene_name)).getText().toString().trim();
        boolean en = newName.length() > 0;
        if (mEditType != EDIT_TYPE_DEVICE_PORT) en &= sceneElementsAssigning.hasElements();

        btnSaveOrTrash.setVisibility(View.VISIBLE);
        btnSaveOrTrash.setIcon(en ? ContextCompat.getDrawable(this, android.R.drawable.ic_menu_save) :
                ContextCompat.getDrawable(this, R.drawable.btn_save_disabled), btnSaveOrTrash.getIcon() != null);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        SelectDrawableDialog.activityCheckForPickedImage(this, this, requestCode, resultCode, imageReturnedIntent);
    }

    @Override
    public void onNameChangeResult(boolean success, String error_message) {
        if (progressDialog != null) {
            if (progressDialog.isShowing()) {
                //get the Context object that was used to great the dialog
                Context context = ((ContextWrapper) progressDialog.getContext()).getBaseContext();

                //if the Context used here was an activity AND it hasn't been finished or destroyed
                //then dismiss it
                if (context instanceof Activity) {
                    if (!((Activity) context).isFinishing() && !((Activity) context).isDestroyed())
                        progressDialog.dismiss();
                } else //if the Context used wasnt an Activity, then dismiss it too
                    progressDialog.dismiss();
            }
            progressDialog = null;
        }

        if (!success) {
            //noinspection ConstantConditions
            Toast.makeText(App.instance, App.instance.getString(R.string.renameFailed, error_message), Toast.LENGTH_SHORT).show();
        } else {
            executable.title = (((EditText) findViewById(R.id.scene_name)).getText().toString().trim());
            save_and_close();
        }
    }

    @Override
    public void onNameChangeStart(Executable executable) {
        if (progressDialog == null)
            progressDialog = new ProgressDialog(this);

        progressDialog.setTitle(R.string.renameInProgress);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }
}