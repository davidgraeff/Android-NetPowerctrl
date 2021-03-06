package oly.netpowerctrl.groups;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;

import com.rey.material.app.Dialog;
import com.rey.material.app.SimpleDialog;
import com.rey.material.widget.CheckBox;
import com.wefika.flowlayout.FlowLayout;

import java.util.Set;

import oly.netpowerctrl.R;
import oly.netpowerctrl.credentials.Credentials;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.ui.FragmentUtils;
import oly.netpowerctrl.ui.ThemeHelper;

/**
 * Utility methods for groups
 */
public class GroupUtilities {

    public static void addGroupCheckBoxesToLayout(GroupCollection groupCollection, FlowLayout layout,
                                                  Set<String> listOfGroupsPreChecked, final Set<String> checked_groups,
                                                  final CompoundButton.OnCheckedChangeListener checkedChangeListener) {

        LayoutInflater l = LayoutInflater.from(layout.getContext());

        for (final Group group : groupCollection.getItems().values()) {
            boolean isContained = listOfGroupsPreChecked.contains(group.getUid());
            if (isContained)
                checked_groups.add(group.getUid());

            CheckBox p = (CheckBox) l.inflate(R.layout.executable_group, layout, false);
            p.setChecked(isContained);
            // The first entry of weekDays_Strings is an empty string
            p.setText(group.name);
            p.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (b)
                        checked_groups.add(group.getUid());
                    else
                        checked_groups.remove(group.getUid());

                    if (checkedChangeListener != null)
                        checkedChangeListener.onCheckedChanged(compoundButton, b);
                }
            });
            layout.addView(p);
        }
    }

    /**
     * Create a new group and optionally add the given device port to it.
     *
     * @param context The context
     */
    public static void createGroup(Activity context, final GroupCollection groupCollection, final GroupCreatedCallback groupCreatedCallback) {
        RenameOrCreateDialog dialogFragment = new RenameOrCreateDialog(groupCollection, null, groupCreatedCallback);
        //LogDialog dialogFragment = new LogDialog();
        FragmentUtils.changeToDialog(context, dialogFragment);
    }

    public static void renameGroup(Activity context, final GroupCollection groupCollection, String groupUID) {
        final Group group = groupCollection.getByUID(groupUID);
        if (group == null)
            return;

        RenameOrCreateDialog dialogFragment = new RenameOrCreateDialog(groupCollection, group, null);
        FragmentUtils.changeToDialog(context, dialogFragment);
    }

    public static void askForGroup(Context context, final Credentials credentials) {
        final SimpleDialog.Builder builder = new SimpleDialog.Builder(ThemeHelper.getDialogRes(context));
        builder.title(context.getString(R.string.device_createGroup));
        builder.message(context.getString(R.string.confirmation_device_createGroup));
        builder.negativeAction(context.getString(android.R.string.no)).positiveAction(context.getString(android.R.string.yes));
        final Dialog dialog = builder.build(context);
        dialog.positiveActionClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                DataService dataService = DataService.getService();
                String groupUID_derivedByDeviceID = credentials.deviceUID;
                for (Executable executable : dataService.executables.filterExecutables(credentials)) {
                    executable.addToGroup(groupUID_derivedByDeviceID);
                    dataService.executables.put(executable);
                }
                dataService.groups.put(groupUID_derivedByDeviceID, credentials.getDeviceName());
            }
        });
        dialog.negativeActionClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    public interface GroupCreatedCallback {
        void onGroupCreated(Group group);
    }
}
