package oly.netpowerctrl.groups;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.internal.widget.TintCheckBox;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.wefika.flowlayout.FlowLayout;

import java.util.Set;

import oly.netpowerctrl.R;

/**
 * Utility methods for groups
 */
public class GroupUtilities {

    public static void addGroupCheckBoxesToLayout(Context context, GroupCollection groupCollection, FlowLayout layout,
                                                  Set<String> listOfGroupsPreChecked, final Set<String> checked_groups,
                                                       final CompoundButton.OnCheckedChangeListener checkedChangeListener) {

        for (final Group group : groupCollection.getItems().values()) {
            boolean isContained = listOfGroupsPreChecked.contains(group.getUid());
            if (isContained)
                checked_groups.add(group.getUid());

            CheckBox p = new TintCheckBox(context);
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
            FlowLayout.LayoutParams lp = new FlowLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            layout.addView(p, lp);
        }
    }

    /**
     * Create a new group and optionally add the given device port to it.
     *
     * @param context The context
     */
    public static void createGroup(Context context, final GroupCollection groupCollection, final GroupCreatedCallback groupCreatedCallback) {
        //noinspection ConstantConditions
        AlertDialog.Builder alert = new AlertDialog.Builder(context);

        alert.setMessage(R.string.group_create_dialog_title);

        final EditText input = new EditText(alert.getContext());
        alert.setView(input);
        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //noinspection ConstantConditions
                String name = input.getText().toString().trim();
                if (name.isEmpty())
                    return;
                Group group = groupCollection.put(null, name);
                if (groupCreatedCallback != null)
                    groupCreatedCallback.onGroupCreated(group);
            }
        });

        alert.setNegativeButton(android.R.string.cancel, null);
        alert.show();
    }

    public static void renameGroup(Context context, final GroupCollection groupCollection, String groupFilter) {
        final Group group = groupCollection.getByUID(groupFilter);
        if (group == null)
            return;

        //noinspection ConstantConditions
        AlertDialog.Builder alert = new AlertDialog.Builder(context);

        alert.setTitle(R.string.menu_rename_group);

        final EditText input = new EditText(alert.getContext());
        input.setText(group.name);
        alert.setView(input);
        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //noinspection ConstantConditions
                String name = input.getText().toString().trim();
                if (name.isEmpty())
                    return;
                groupCollection.put(group.uid, name);
            }
        });

        alert.setNegativeButton(android.R.string.cancel, null);
        alert.show();
    }

    public interface GroupCreatedCallback {
        void onGroupCreated(Group group);
    }
}
