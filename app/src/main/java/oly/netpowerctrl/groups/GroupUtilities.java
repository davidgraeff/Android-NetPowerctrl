package oly.netpowerctrl.groups;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.EditText;
import android.widget.Toast;

import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.devices.DevicePort;

/**
 * Utility methods for groups
 */
public class GroupUtilities {
    public static void createGroup(final Context context, final DevicePort port, final groupsChangedInterface callback) {
        final GroupCollection groupCollection = NetpowerctrlApplication.getDataController().groupCollection;

        // No groups? Ask the user to create one
        if (groupCollection.length() == 0) {
            createGroupForDevicePort(context, port, callback);
            return;
        }

        CharSequence[] items = groupCollection.getGroupsArray();
        final boolean[] checked = new boolean[items.length];

        // Sync checked array with items array
        for (int i = 0; i < checked.length; ++i) {
            if (groupCollection.equalsAtIndex(i, port.groups))
                checked[i] = true;
        }

        //noinspection ConstantConditions
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.outlet_to_group_title, port.getDescription()))
                .setIcon(android.R.drawable.ic_dialog_info)
                .setMultiChoiceItems(items, checked, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                        checked[i] = b;
                    }
                })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        port.groups.clear();
                        int counter = 0;
                        for (int i = 0; i < checked.length; ++i) {
                            if (!checked[i]) {
                                continue;
                            }
                            port.groups.add(groupCollection.groups.get(i).uuid);
                            ++counter;
                        }
                        NetpowerctrlApplication.getDataController().deviceCollection.save();
                        Toast.makeText(context, context.getString(R.string.outlet_added_to_groups, counter), Toast.LENGTH_SHORT).show();
                        if (callback != null)
                            callback.onGroupsChanged(port);
                    }
                })
                .setNeutralButton(R.string.createGroup, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        createGroupForDevicePort(context, port, callback);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    private static void createGroupForDevicePort(Context context, final DevicePort port, final groupsChangedInterface callback) {
        //noinspection ConstantConditions
        AlertDialog.Builder alert = new AlertDialog.Builder(context);

        alert.setTitle(context.getString(R.string.outlet_to_group_title, port.getDescription()));
        alert.setMessage(R.string.group_create);

        final EditText input = new EditText(alert.getContext());
        input.setText(port.getDescription());
        alert.setView(input);
        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //noinspection ConstantConditions
                String name = input.getText().toString().trim();
                if (name.isEmpty())
                    return;
                UUID group_uuid = NetpowerctrlApplication.getDataController().groupCollection.add(name);
                port.addToGroup(group_uuid);
                NetpowerctrlApplication.getDataController().deviceCollection.save();
                if (callback != null)
                    callback.onGroupsChanged(port);
            }
        });

        alert.setNegativeButton(android.R.string.cancel, null);
        alert.show();
    }

    public static void renameGroup(Context context, UUID groupFilter) {
        final GroupCollection.GroupItem groupItem = NetpowerctrlApplication.getDataController().groupCollection.get(groupFilter);
        if (groupItem == null)
            return;

        //noinspection ConstantConditions
        AlertDialog.Builder alert = new AlertDialog.Builder(context);

        alert.setTitle(R.string.menu_rename_group);

        final EditText input = new EditText(alert.getContext());
        input.setText(groupItem.name);
        alert.setView(input);
        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //noinspection ConstantConditions
                String name = input.getText().toString().trim();
                if (name.isEmpty())
                    return;
                NetpowerctrlApplication.getDataController().groupCollection.edit(groupItem.uuid, name);
            }
        });

        alert.setNegativeButton(android.R.string.cancel, null);
        alert.show();
    }

    public interface groupsChangedInterface {
        void onGroupsChanged(DevicePort port);
    }

}
