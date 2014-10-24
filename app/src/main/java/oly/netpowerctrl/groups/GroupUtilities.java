package oly.netpowerctrl.groups;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.EditText;

import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.device_ports.DevicePort;

/**
 * Utility methods for groups
 */
public class GroupUtilities {
    /**
     * Create a new group and optionally add the given device port to it.
     *
     * @param context The context
     * @param port    The device port that is added to the newly created group. May be null.
     */
    public static void createGroupForDevicePort(Context context, final DevicePort port) {
        //noinspection ConstantConditions
        AlertDialog.Builder alert = new AlertDialog.Builder(context);

        alert.setMessage(R.string.group_create);

        final EditText input = new EditText(alert.getContext());
        if (port != null)
            input.setText(port.getTitle());
        alert.setView(input);
        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //noinspection ConstantConditions
                String name = input.getText().toString().trim();
                if (name.isEmpty())
                    return;
                UUID group_uuid = AppData.getInstance().groupCollection.add(name);
                if (port != null)
                    port.addToGroup(group_uuid);
                AppData.getInstance().deviceCollection.save(port.device);
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
}
