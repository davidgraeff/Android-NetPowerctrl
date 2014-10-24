package oly.netpowerctrl.outletsview;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.devices.DeviceCollection;
import oly.netpowerctrl.devices.DevicePort;
import oly.netpowerctrl.executables.ExecutableViewHolder;
import oly.netpowerctrl.groups.GroupCollection;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.network.DeviceQuery;
import oly.netpowerctrl.network.onHttpRequestResult;

public class OutletEditDialog extends DialogFragment implements onHttpRequestResult,
        LoadStoreIconData.IconSelected {
    private DevicePort devicePort;
    private ProgressDialog progressDialog;
    private EditText editName;
    private LoadStoreIconData.IconState iconState;
    private boolean[] checked;
    private ImageButton btnOn;
    private ImageButton btnOff;
    private ExecutableViewHolder executableViewHolder;
    private RecyclerView.Adapter<?> adapter;

    public OutletEditDialog() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View rootView = getActivity().getLayoutInflater().inflate(R.layout.fragment_outlet_edit, null);


        //noinspection ConstantConditions
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(getString(R.string.outlet_edit_title, devicePort.getTitle()));
        //builder.setMessage(getString(R.string.outlet_rename_message, devicePort.getTitle()));

        editName = (EditText) rootView.findViewById(R.id.outlet_name);
        editName.setText(devicePort.getTitle());

        btnOff = (ImageButton) rootView.findViewById(R.id.outlet_icon_off);
        btnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                iconState = LoadStoreIconData.IconState.StateOff;
                LoadStoreIconData.show_select_icon_dialog(getActivity(), "outlet_icons", OutletEditDialog.this, devicePort);
            }
        });

        btnOn = (ImageButton) rootView.findViewById(R.id.outlet_icon_on);
        btnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                iconState = LoadStoreIconData.IconState.StateOn;
                LoadStoreIconData.show_select_icon_dialog(getActivity(), "outlet_icons", OutletEditDialog.this, devicePort);
            }
        });

        loadImages();

        LinearLayout layout = (LinearLayout) rootView.findViewById(R.id.layout);

        final GroupCollection groupCollection = AppData.getInstance().groupCollection;

        CharSequence[] items = groupCollection.getGroupsArray();
        checked = new boolean[items.length];

        // Sync checked array with items array
        for (int i = 0; i < checked.length; ++i) {
            if (groupCollection.equalsAtIndex(i, devicePort.groups))
                checked[i] = true;

            CheckBox p = new CheckBox(getActivity());
            p.setChecked(checked[i]);
            // The first entry of weekDays_Strings is an empty string
            p.setText(items[i]);
            final int index = i;
            p.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    checked[index] = b;
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            layout.addView(p, lp);
        }

        builder.setView(rootView);
        builder.setPositiveButton(getString(R.string.save), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });
        builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        return builder.create();
    }

    private void loadImages() {
        btnOff.setImageDrawable(LoadStoreIconData.loadDrawable(getActivity(), devicePort.getUid(), LoadStoreIconData.IconType.DevicePortIcon,
                LoadStoreIconData.IconState.StateOff));
        btnOn.setImageDrawable(LoadStoreIconData.loadDrawable(getActivity(), devicePort.getUid(), LoadStoreIconData.IconType.DevicePortIcon,
                LoadStoreIconData.IconState.StateOn));
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog d = (AlertDialog) getDialog();
        d.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newName = editName.getText().toString().trim();
                if (!newName.isEmpty() && !devicePort.getTitle().equals(newName))
                    //noinspection ConstantConditions
                    AppData.getInstance().rename(devicePort, newName, OutletEditDialog.this);

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

                dismiss();
            }
        });
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
            oi.device.setHasChanged();
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

}
