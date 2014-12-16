package oly.netpowerctrl.outletsview;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.wefika.flowlayout.FlowLayout;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.devices.DeviceCollection;
import oly.netpowerctrl.executables.ExecutableViewHolder;
import oly.netpowerctrl.groups.GroupCollection;
import oly.netpowerctrl.groups.GroupUtilities;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.network.DeviceQuery;
import oly.netpowerctrl.network.onHttpRequestResult;
import oly.netpowerctrl.ui.navigation.NavigationController;
import oly.netpowerctrl.ui.notifications.InAppNotifications;
import oly.netpowerctrl.ui.widgets.FloatingActionButton;
import oly.netpowerctrl.utils.ActionBarTitle;
import oly.netpowerctrl.utils.AnimationController;
import oly.netpowerctrl.utils.MutableBoolean;
import oly.netpowerctrl.utils.fragments.onFragmentBackButton;

public class OutletEditFragment extends Fragment implements onHttpRequestResult,
        LoadStoreIconData.IconSelected, onFragmentBackButton {
    ActionBarTitle actionBarTitle = new ActionBarTitle();
    private DevicePort devicePort;
    private ProgressDialog progressDialog;
    private EditText editName;
    private boolean[] checked;
    private ExecutableViewHolder executableViewHolder;
    private RecyclerView.Adapter<?> adapter;
    private FloatingActionButton btnOk;
    private ImageView btnSceneFav;
    private Toast toast;
    private boolean isFavourite;
    private boolean iconMenuVisible = false;
    private View execute_icons_container;
    private View executable_icons;
    private View scene_image;
    private boolean loadingDoNotSaveIcon = false;

    public OutletEditFragment() {
    }

    @Override
    public void onStart() {
        super.onStart();
        MainActivity.getNavigationController().createBackButton(new NavigationController.OnNavigationBackButtonPressed() {
            @Override
            public void onNavigationBackButtonPressed() {
                MainActivity.getNavigationController().changeToFragment(OutletsViewFragment.class.getName());
            }
        });

        if (execute_icons_container != null && execute_icons_container.findViewById(R.id.executable_icons) == null) {
            ViewGroup v = (ViewGroup) execute_icons_container;
            v.addView(executable_icons);
            v.addView(scene_image);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        actionBarTitle.restoreTitle((ActionBarActivity) getActivity());
        ViewGroup v = (ViewGroup) execute_icons_container;
        v.removeView(executable_icons);
        v.removeView(scene_image);
        MainActivity.getNavigationController().createDrawerToggle();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_outlet_edit, container, false);

        // Add icons view
        execute_icons_container = inflater.inflate(R.layout.executable_icons,
                (RelativeLayout) getActivity().findViewById(R.id.outer_layout), true);
        executable_icons = execute_icons_container.findViewById(R.id.executable_icons);
        scene_image = execute_icons_container.findViewById(R.id.scene_image);

        toast = Toast.makeText(getActivity(), "", Toast.LENGTH_SHORT);

        if (devicePort == null)
            return rootView;

        actionBarTitle.setTitle(getActivity(), "");
        actionBarTitle.setSubTitle((ActionBarActivity) getActivity(), getString(R.string.outlet_edit_title, devicePort.getTitle()));

        editName = (EditText) rootView.findViewById(R.id.scene_name);
        editName.setText(devicePort.getTitle());
        editName.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSaveButton();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void afterTextChanged(Editable s) {
            }
        });

        btnOk = (FloatingActionButton) rootView.findViewById(R.id.btnOk);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                save_and_close();
            }
        });
        updateSaveButton();

        btnSceneFav = (ImageView) rootView.findViewById(R.id.btnSceneFavourite);
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

        View.OnClickListener iconClick = new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (iconMenuVisible) {
                    AnimationController.animatePress(view);
                    LoadStoreIconData.show_select_icon_dialog(getActivity(), "scene_icons", OutletEditFragment.this, view);
                    return;
                }

                iconMenuVisible = true;
                AnimationController.animateViewInOut(execute_icons_container.findViewById(R.id.executable_icons), true, false);
            }
        };

        // We may return from another activity, but the menu should be visible. animate in now.
        if (iconMenuVisible)
            AnimationController.animateViewInOut(execute_icons_container.findViewById(R.id.executable_icons), true, false);

        execute_icons_container.findViewById(R.id.scene_image).setOnClickListener(iconClick);
        execute_icons_container.findViewById(R.id.scene_image_on).setOnClickListener(iconClick);
        execute_icons_container.findViewById(R.id.scene_image_off).setOnClickListener(iconClick);

        loadingDoNotSaveIcon = true;
        setIcon(execute_icons_container.findViewById(R.id.scene_image), null);
        setIcon(execute_icons_container.findViewById(R.id.scene_image_off), null);
        setIcon(execute_icons_container.findViewById(R.id.scene_image_on), null);
        loadingDoNotSaveIcon = false;

        execute_icons_container.findViewById(R.id.close_icons).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                iconMenuVisible = false;
                AnimationController.animateViewInOut(execute_icons_container.findViewById(R.id.executable_icons), false, false);
            }
        });

        updateFavButton();

        @SuppressLint("WrongViewCast")
        FlowLayout layout = (FlowLayout) rootView.findViewById(R.id.groups_layout);
        checked = GroupUtilities.addGroupCheckBoxesToLayout(getActivity(), layout, devicePort.groups, null);

        return rootView;
    }

    private void updateFavButton() {
        Resources r = getResources();
        btnSceneFav.setImageDrawable(isFavourite ? r.getDrawable(android.R.drawable.btn_star_big_on)
                : r.getDrawable(android.R.drawable.btn_star_big_off));
    }

    private void updateSaveButton() {
        boolean en = (editName.length() > 0);
        Resources r = getResources();
        btnOk.setDrawable(en ? r.getDrawable(android.R.drawable.ic_menu_save) :
                r.getDrawable(R.drawable.btn_save_disabled));
    }

    @Override
    public void setIcon(Object context_object, Bitmap icon) {
        MutableBoolean isDefault = new MutableBoolean();
        switch (((View) context_object).getId()) {
            case R.id.scene_image:
                if (!loadingDoNotSaveIcon)
                    AppData.getInstance().deviceCollection.setDevicePortBitmap(getActivity(),
                            devicePort, icon, LoadStoreIconData.IconState.OnlyOneState);
                if (icon == null)
                    icon = LoadStoreIconData.loadBitmap(getActivity(), devicePort,
                            LoadStoreIconData.IconState.OnlyOneState, isDefault);
                break;
            case R.id.scene_image_off:
                if (!loadingDoNotSaveIcon)
                    AppData.getInstance().deviceCollection.setDevicePortBitmap(getActivity(),
                            devicePort, icon, LoadStoreIconData.IconState.StateOff);
                if (icon == null)
                    icon = LoadStoreIconData.loadBitmap(getActivity(), devicePort,
                            LoadStoreIconData.IconState.StateOff, isDefault);
                break;
            case R.id.scene_image_on:
                if (!loadingDoNotSaveIcon)
                    AppData.getInstance().deviceCollection.setDevicePortBitmap(getActivity(),
                            devicePort, icon, LoadStoreIconData.IconState.StateOn);
                if (icon == null)
                    icon = LoadStoreIconData.loadBitmap(getActivity(), devicePort,
                            LoadStoreIconData.IconState.StateOn, isDefault);
                break;
            default:
                throw new RuntimeException("Image id not known!");
        }

        if (icon == null) {
            throw new RuntimeException("Default icon null!");
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

        int backColor = Color.GRAY;
//        if (SharedPrefs.getInstance().isDarkTheme())
//            backColor = Palette.generate(icon).getDarkVibrantColor(getResources().getColor(R.color.colorBackgroundDark));
//        else
//            backColor = Palette.generate(icon).getVibrantColor(getResources().getColor(R.color.colorBackgroundLight));

        Paint mButtonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mButtonPaint.setStyle(Paint.Style.FILL);
        mButtonPaint.setColor(backColor);
        mButtonPaint.setShadowLayer(10f, 5f, 5f, Color.GRAY);

        resultCanvas.drawCircle(min, min, min, mButtonPaint);
        resultCanvas.drawBitmap(icon, 0, 0, null);
        resultCanvas.drawBitmap(rounder, 0, 0, xferPaint);

        ((ImageView) context_object).setImageBitmap(result);
    }


    private void save_and_close() {
        AppData appData = AppData.getInstance();
        String newName = editName.getText().toString().trim();
        if (!newName.isEmpty() && !devicePort.getTitle().equals(newName))
            //noinspection ConstantConditions
            appData.rename(devicePort, newName, OutletEditFragment.this);

        GroupCollection groupCollection = appData.groupCollection;
        devicePort.groups.clear();
        int counter = 0;
        for (int i = 0; i < checked.length; ++i) {
            if (!checked[i]) {
                continue;
            }
            devicePort.groups.add(groupCollection.get(i).uuid);
            ++counter;
        }
        DeviceCollection deviceCollection = appData.deviceCollection;
        deviceCollection.groupsUpdated(devicePort.device);
        deviceCollection.save(devicePort.device);
        appData.favCollection.setFavourite(devicePort.getUid(), isFavourite);
        Toast.makeText(getActivity(), getString(R.string.outlet_added_to_groups, counter), Toast.LENGTH_SHORT).show();

        executableViewHolder.reload();
        adapter.notifyItemChanged(executableViewHolder.position);

        MainActivity.getNavigationController().changeToFragment(OutletsViewFragment.class.getName());
    }

    public void setDevicePort(DevicePort devicePort, ExecutableViewHolder executableViewHolder, RecyclerView.Adapter<?> adapter) {
        this.executableViewHolder = executableViewHolder;
        this.devicePort = devicePort;
        this.adapter = adapter;
        this.isFavourite = AppData.getInstance().favCollection.isFavourite(devicePort.getUid());
    }

    @Override
    public void httpRequestResult(DevicePort oi, boolean success, String error_message) {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        // Warning: getActivity may not work here anymore!

        if (!success) {
            //noinspection ConstantConditions
            Toast.makeText(App.instance, App.instance.getString(R.string.renameFailed, error_message), Toast.LENGTH_SHORT).show();
        } else {
            new DeviceQuery(App.instance, null, oi.device);
        }
    }

    @Override
    public void httpRequestStart(DevicePort oi) {
        Context context = getActivity();
        if (context == null)
            return;

        if (progressDialog == null)
            progressDialog = new ProgressDialog(context);

        progressDialog.setTitle(R.string.renameInProgress);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        LoadStoreIconData.activityCheckForPickedImage(getActivity(), this, requestCode, resultCode, imageReturnedIntent);
    }

    @Override
    public boolean onBackButton() {
        MainActivity.getNavigationController().changeToFragment(OutletsViewFragment.class.getName());
        return true;
    }
}
