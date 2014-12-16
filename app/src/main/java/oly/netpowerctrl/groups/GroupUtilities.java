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

import java.util.List;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;

/**
 * Utility methods for groups
 */
public class GroupUtilities {

    public static boolean[] addGroupCheckBoxesToLayout(Context context, FlowLayout layout,
                                                       List<UUID> listOfGroupsPreChecked,
                                                       final CompoundButton.OnCheckedChangeListener checkedChangeListener) {

        final GroupCollection groupCollection = AppData.getInstance().groupCollection;

        CharSequence[] items = groupCollection.getGroupsArray();
        final boolean checked[] = new boolean[items.length];

        // Sync checked array with items array
        for (int i = 0; i < checked.length; ++i) {
            if (groupCollection.equalsAtIndex(i, listOfGroupsPreChecked))
                checked[i] = true;

            CheckBox p = new TintCheckBox(context);
            p.setChecked(checked[i]);
            // The first entry of weekDays_Strings is an empty string
            p.setText(items[i]);
            final int index = i;
            p.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    checked[index] = b;
                    if (checkedChangeListener != null)
                        checkedChangeListener.onCheckedChanged(compoundButton, b);
                }
            });
            FlowLayout.LayoutParams lp = new FlowLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            layout.addView(p, lp);
        }

        return checked;
    }

    /**
     * Create a new group and optionally add the given device port to it.
     *
     * @param context The context
     */
    public static void createGroup(Context context, final GroupCreatedCallback groupCreatedCallback) {
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
                int index = AppData.getInstance().groupCollection.add(name);
                if (groupCreatedCallback != null)
                    groupCreatedCallback.onGroupCreated(index);
            }
        });

        alert.setNegativeButton(android.R.string.cancel, null);
        alert.show();
    }

    public static void renameGroup(Context context, UUID groupFilter) {
        final Group group = AppData.getInstance().groupCollection.get(groupFilter);
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
                AppData.getInstance().groupCollection.edit(group.uuid, name);
            }
        });

        alert.setNegativeButton(android.R.string.cancel, null);
        alert.show();
    }

    public interface GroupCreatedCallback {
        void onGroupCreated(int group_index);
    }
}
