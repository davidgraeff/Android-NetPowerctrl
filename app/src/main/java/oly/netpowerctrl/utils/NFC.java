package oly.netpowerctrl.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.SparseIntArray;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.DeviceCollection;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.datastructure.SceneCollection;
import oly.netpowerctrl.main.NetpowerctrlActivity;
import oly.netpowerctrl.main.NetpowerctrlApplication;

/**
 * NFC related
 */
public class NFC {
    public static class VersionException extends Exception {
        public int version;

        public VersionException(int version) {
            this.version = version;
        }
    }

    public static class NFC_Transfer {
        public static final int PROTOCOL_VERSION = 2;
        public SceneCollection scenes = null;
        public DeviceCollection devices = null;
        public String source_device;
        public int version = 0;

        public static NFC_Transfer fromData(SceneCollection scenes, DeviceCollection devices) {
            NFC_Transfer dc = new NFC_Transfer();
            dc.scenes = scenes;
            dc.devices = devices;
            return dc;
        }

        public static NFC_Transfer fromJSON(String scenes_as_string) throws IOException, VersionException {
            NFC_Transfer dc = new NFC_Transfer();

            // Get JsonReader from String
            byte[] bytes;
            try {
                bytes = scenes_as_string.getBytes("UTF-8");
            } catch (UnsupportedEncodingException ignored) {
                return null;
            }
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            JsonReader reader = new JsonReader(new InputStreamReader(byteArrayInputStream));

            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                assert name != null;
                if (name.equals("scenes_collection")) {
                    dc.scenes = SceneCollection.fromJSON(reader);
                } else if (name.equals("devices_collection")) {
                    dc.devices = DeviceCollection.fromJSON(reader);
                } else if (name.equals("version")) {
                    dc.version = reader.nextInt();
                    if (dc.version != PROTOCOL_VERSION) {
                        throw new VersionException(dc.version);
                    }
                } else if (name.equals("source_device")) {
                    dc.source_device = reader.nextString();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
            return dc;
        }

        public void toJSON(JsonWriter writer) throws IOException {
            writer.beginObject();
            writer.name("version").value(PROTOCOL_VERSION);
            writer.name("source_device").value(android.os.Build.MODEL);
            writer.name("scenes_collection");
            scenes.toJSON(writer);
            writer.name("devices_collection");
            devices.toJSON(writer);
            writer.endObject();
        }
    }

    public static void parseNFC(Context context, String text) {
        final NFC_Transfer dc;
        try {
            dc = NFC_Transfer.fromJSON(text);
        } catch (IOException e) {
            Toast.makeText(context, context.getString(R.string.nfc_failed), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return;
        } catch (IllegalStateException e) {
            Toast.makeText(context, context.getString(R.string.nfc_failed) + " " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return;
        } catch (VersionException ignored) {
            Toast.makeText(context, context.getString(R.string.nfc_failed_version), Toast.LENGTH_LONG).show();
            return;
        }

        if (dc.devices != null)
            showSelectionDevices(context, dc);
        else if (dc.scenes != null)
            showSelectionScenes(context, dc.scenes);
    }

    private static void showSelectionDevices(final Context context, final NFC_Transfer transfer) {
        final DeviceCollection dc = transfer.devices;
        String[] deviceNames = new String[dc.devices.size()];
        int i = 0;
        for (DeviceInfo di : dc.devices) {
            deviceNames[i] = di.DeviceName;
            ++i;
        }

        // ArrayList to keep the selected items
        final SparseIntArray selectedItems = new SparseIntArray();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.nfc_select_devices_title));
        builder.setMultiChoiceItems(deviceNames, null,
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int indexSelected,
                                        boolean isChecked) {
                        if (isChecked) {
                            // If the user checked the item, add it to the selected items
                            selectedItems.put(indexSelected, indexSelected);
                        } else if (selectedItems.get(indexSelected, -1) != -1) {
                            // Else, if the item is already in the array, remove it
                            selectedItems.delete(indexSelected);
                        }
                    }
                })
                // Set the action buttons
                .setPositiveButton(context.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        int i = 0;
                        boolean devices_added = false;
                        for (DeviceInfo di : dc.devices) {
                            if (selectedItems.get(i, -1) != -1) {
                                NetpowerctrlApplication.instance.addToConfiguredDevices(di, false);
                                devices_added = true;
                            }
                            ++i;
                        }
                        if (devices_added) {
                            NetpowerctrlApplication.instance.saveConfiguredDevices(true);
                        }
                        if (transfer.scenes != null)
                            showSelectionScenes(context, transfer.scenes);
                    }
                })
                .setNegativeButton(context.getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (transfer.scenes != null)
                            showSelectionScenes(context, transfer.scenes);
                    }
                });
        AlertDialog dialog = builder.create();//AlertDialog dialog; create like this outside onClick
        dialog.show();
    }

    private static void showSelectionScenes(Context context, final SceneCollection sc) {
        String[] sceneNames = new String[sc.scenes.size()];
        int i = 0;
        for (Scene scene : sc.scenes) {
            boolean already_installed = NetpowerctrlActivity.instance.getScenesAdapter().getScenes().contains(scene);
            sceneNames[i] = scene.sceneName;
            if (already_installed)
                sceneNames[i] += " " + context.getString(R.string.scene_replace);
            ++i;
        }

        // ArrayList to keep the selected items
        final SparseIntArray selectedItems = new SparseIntArray();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.nfc_select_scenes_title));
        builder.setMultiChoiceItems(sceneNames, null,
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int indexSelected,
                                        boolean isChecked) {
                        if (isChecked) {
                            // If the user checked the item, add it to the selected items
                            selectedItems.put(indexSelected, indexSelected);
                        } else if (selectedItems.get(indexSelected, -1) != -1) {
                            // Else, if the item is already in the array, remove it
                            selectedItems.delete(indexSelected);
                        }
                    }
                })
                // Set the action buttons
                .setPositiveButton(context.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        int i = 0;
                        for (Scene di : sc.scenes) {
                            if (selectedItems.get(i, -1) != -1)
                                NetpowerctrlActivity.instance.getScenesAdapter().addScene(di);
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
