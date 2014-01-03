package oly.netpowerctrl.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.DeviceCollection;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.main.NetpowerctrlApplication;

/**
 * NFC related
 */
public class NFC {
    public static void showSelectionDialog(Context context, String text) {
        final DeviceCollection dc;
        try {
            dc = DeviceCollection.fromJSON(text);
        } catch (IOException e) {
            Toast.makeText(context, context.getString(R.string.nfc_failed), Toast.LENGTH_SHORT).show();
            return;
        }

        String[] deviceNames = new String[dc.devices.size()];
        int i = 0;
        for (DeviceInfo di : dc.devices) {
            deviceNames[i] = di.DeviceName;
            ++i;
        }

        // arraylist to keep the selected items
        final ArrayList seletedItems = new ArrayList();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.nfc_select_devices_title));
        builder.setMultiChoiceItems(deviceNames, null,
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int indexSelected,
                                        boolean isChecked) {
                        if (isChecked) {
                            // If the user checked the item, add it to the selected items
                            seletedItems.add(indexSelected);
                        } else if (seletedItems.contains(indexSelected)) {
                            // Else, if the item is already in the array, remove it
                            seletedItems.remove(Integer.valueOf(indexSelected));
                        }
                    }
                })
                // Set the action buttons
                .setPositiveButton(context.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        int i = 0;
                        for (DeviceInfo di : dc.devices) {
                            if (seletedItems.contains(i))
                                NetpowerctrlApplication.instance.addToConfiguredDevices(di);
                            ++i;
                        }
                    }
                })
                .setNegativeButton(context.getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        AlertDialog dialog = builder.create();//AlertDialog dialog; create like this outside onClick
        dialog.show();
    }
}
