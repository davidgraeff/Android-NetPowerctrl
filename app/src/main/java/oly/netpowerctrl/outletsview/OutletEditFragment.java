package oly.netpowerctrl.outletsview;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import oly.netpowerctrl.ui.notifications.InAppNotifications;
import oly.netpowerctrl.ui.widgets.FloatingActionButton;
import oly.netpowerctrl.utils.ActionBarTitle;
import oly.netpowerctrl.utils.fragments.onFragmentBackButton;

public class OutletEditFragment extends Fragment implements onHttpRequestResult,
        LoadStoreIconData.IconSelected, onFragmentBackButton {
    ActionBarTitle actionBarTitle = new ActionBarTitle();
    private DevicePort devicePort;
    private ProgressDialog progressDialog;
    private EditText editName;
    private LoadStoreIconData.IconState iconState;
    private boolean[] checked;
    private ImageButton btnOn;
    private ImageButton btnOff;
    private ExecutableViewHolder executableViewHolder;
    private RecyclerView.Adapter<?> adapter;
    private FloatingActionButton btnOk;
    private ImageView btnSceneFav;
    private Toast toast;

    public OutletEditFragment() {
    }

    @Override
    public void onStart() {
        super.onStart();
        actionBarTitle.setTitle(getActivity(), getString(R.string.outlet_edit_title, devicePort.getTitle(getActivity())));
        MainActivity.getNavigationController().createBackButton();
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar_actionbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.getNavigationController().changeToFragment(OutletsViewFragment.class.getName());
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        actionBarTitle.restoreTitle(getActivity());
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar_actionbar);
        toolbar.setNavigationOnClickListener(null);
        MainActivity.getNavigationController().createDrawerToggle();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_outlet_edit, container, false);

        toast = Toast.makeText(getActivity(), "", Toast.LENGTH_SHORT);

        editName = (EditText) rootView.findViewById(R.id.scene_name);
        editName.setText(devicePort.getTitle(getActivity()));
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

        btnOff = (ImageButton) rootView.findViewById(R.id.outlet_icon_off);
        btnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                iconState = LoadStoreIconData.IconState.StateOff;
                LoadStoreIconData.show_select_icon_dialog(getActivity(), "outlet_icons", OutletEditFragment.this, devicePort);
            }
        });

        btnOn = (ImageButton) rootView.findViewById(R.id.outlet_icon_on);
        btnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                iconState = LoadStoreIconData.IconState.StateOn;
                LoadStoreIconData.show_select_icon_dialog(getActivity(), "outlet_icons", OutletEditFragment.this, devicePort);
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
                boolean newFavStatus = !devicePort.isFavourite();
                updateFavButton();
                toast.setText(newFavStatus ? R.string.scene_make_favourite : R.string.scene_remove_favourite);
                InAppNotifications.moveToastNextToView(toast, getResources(), view, false);
                toast.show();
            }
        });

        loadImages();

        @SuppressLint("WrongViewCast")
        FlowLayout layout = (FlowLayout) rootView.findViewById(R.id.groups_layout);
        checked = GroupUtilities.addGroupCheckBoxesToLayout(getActivity(), layout, devicePort.groups);

        return rootView;
    }

    private void updateFavButton() {
        boolean isFav = devicePort.isFavourite();
        Resources r = getResources();
        btnSceneFav.setImageDrawable(isFav ? r.getDrawable(android.R.drawable.btn_star_big_on)
                : r.getDrawable(android.R.drawable.btn_star_big_off));
    }

    private void updateSaveButton() {
        boolean en = (editName.length() > 0);
        Resources r = getResources();
        btnOk.setDrawable(en ? r.getDrawable(android.R.drawable.ic_menu_save) :
                r.getDrawable(R.drawable.btn_save_disabled));
    }

    private void loadImages() {
        btnOff.setImageDrawable(LoadStoreIconData.loadDrawable(getActivity(), devicePort.getUid(), LoadStoreIconData.IconType.DevicePortIcon,
                LoadStoreIconData.IconState.StateOff));
        btnOn.setImageDrawable(LoadStoreIconData.loadDrawable(getActivity(), devicePort.getUid(), LoadStoreIconData.IconType.DevicePortIcon,
                LoadStoreIconData.IconState.StateOn));
    }

    private void save_and_close() {
        String newName = editName.getText().toString().trim();
        if (!newName.isEmpty() && !devicePort.getTitle(getActivity()).equals(newName))
            //noinspection ConstantConditions
            AppData.getInstance().rename(devicePort, newName, OutletEditFragment.this);

        GroupCollection groupCollection = AppData.getInstance().groupCollection;
        devicePort.groups.clear();
        int counter = 0;
        for (int i = 0; i < checked.length; ++i) {
            if (!checked[i]) {
                continue;
            }
            devicePort.groups.add(groupCollection.get(i).uuid);
            ++counter;
        }
        DeviceCollection deviceCollection = AppData.getInstance().deviceCollection;
        deviceCollection.groupsUpdated(devicePort.device);
        deviceCollection.save(devicePort.device);
        Toast.makeText(getActivity(), getString(R.string.outlet_added_to_groups, counter), Toast.LENGTH_SHORT).show();

        executableViewHolder.reload();
        adapter.notifyItemChanged(executableViewHolder.position);

        MainActivity.getNavigationController().changeToFragment(OutletsViewFragment.class.getName());
    }

    public void setDevicePort(DevicePort devicePort, ExecutableViewHolder executableViewHolder, RecyclerView.Adapter<?> adapter) {
        this.executableViewHolder = executableViewHolder;
        this.devicePort = devicePort;
        this.adapter = adapter;
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
    public void setIcon(Object context_object, Bitmap bitmap) {
        if (context_object == null || iconState == null)
            return;
        AppData.getInstance().deviceCollection.setDevicePortBitmap(getActivity(),
                (DevicePort) context_object, bitmap, iconState);

        loadImages();
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
