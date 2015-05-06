package oly.netpowerctrl.groups;

import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.rey.material.app.Dialog;
import com.rey.material.app.SimpleDialog;
import com.rey.material.widget.EditText;

import oly.netpowerctrl.R;

/**
 * Created by david on 05.05.15.
 */
public class RenameOrCreateDialog extends DialogFragment {
    final Group group;
    final GroupUtilities.GroupCreatedCallback groupCreatedCallback;
    private final GroupCollection groupCollection;
    @Nullable
    private EditText input;


    @SuppressLint("ValidFragment")
    public RenameOrCreateDialog(@NonNull GroupCollection groupCollection, @Nullable Group group, @Nullable GroupUtilities.GroupCreatedCallback groupCreatedCallback) {
        this.groupCollection = groupCollection;
        this.group = group;
        this.groupCreatedCallback = groupCreatedCallback;
    }

    public RenameOrCreateDialog() {
        groupCollection = null;
        group = null;
        groupCreatedCallback = null;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group_name, container, false);
        input = (EditText) view.findViewById(R.id.group_name);
        if (group != null) {
            input.setText(group.name);
            //view.findViewById(R.id.hint).setVisibility(View.GONE);
        }
        return view;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final SimpleDialog.Builder builder = new SimpleDialog.Builder(R.style.SimpleDialogLight);
        if (group != null)
            builder.title(getString(R.string.menu_rename_group));
        else
            builder.title(getString(R.string.group_create_dialog_title));
        builder.negativeAction(getString(android.R.string.no)).positiveAction(getString(android.R.string.yes));
        final Dialog dialog = builder.build(getActivity());
        //final Dialog dialog = new com.rey.material.app.Dialog(getActivity());
        //dialog.layoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.positiveActionClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //noinspection ConstantConditions
                String name = input.getText().toString().trim();
                if (name.isEmpty())
                    return;

                if (group != null)
                    groupCollection.put(group.uid, name);
                else {
                    Group group = groupCollection.put(null, name);
                    if (groupCreatedCallback != null)
                        groupCreatedCallback.onGroupCreated(group);
                }
                dialog.dismiss();
            }
        });
        dialog.negativeActionClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        return dialog;
    }
}
