package oly.netpowerctrl.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.datastructure.SceneCollection;

/**
 * For backup and restore
 */
public class Backup {

    public static void createScenesBackup(Context context, List<Scene> scenes) {
        SceneCollection sc = SceneCollection.fromScenes(scenes, null);
        JSONHelper jh = new JSONHelper();
        try {
            sc.toJSON(jh.createWriter());
            Calendar t = Calendar.getInstance();
            String default_name = DateFormat.getMediumDateFormat(context).format(t.getTime()).replace(".", "_") + " - " + DateFormat.getTimeFormat(context).format(t.getTime()).replace(":", "_");
            File file = new File(context.getExternalFilesDir(null), "backup");
            if (!file.isDirectory() && !file.mkdirs())
                throw new IOException("Directory could not be created!");
            file = new File(context.getExternalFilesDir(null), "backup/" + default_name + ".json");
            OutputStream os = new FileOutputStream(file);
            String backupString = jh.getString();
            os.write(backupString.getBytes());
            os.close();
            Toast.makeText(context, context.getString(R.string.scene_backup_created) + ": " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (IOException ignored) {
            Toast.makeText(context, context.getString(R.string.scene_backup_failed) + " " + ignored.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public static void restoreScenesBackup(final Context context, final SceneCollection sceneCollection) {
        final File backup_directory = new File(context.getExternalFilesDir(null), "backup/");
        if (backup_directory == null) {
            Toast.makeText(context, context.getString(R.string.scene_backup_nobackups), Toast.LENGTH_SHORT).show();
            return;
        }

        final String[] files = backup_directory.list();
        if (files == null || files.length == 0) {
            Toast.makeText(context, context.getString(R.string.scene_backup_nobackups), Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.nfc_select_scenes_title));
        builder.setItems(files,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        File file = new File(backup_directory, files[i]);
                        String jsonData = "";
                        try {
                            FileInputStream is = new FileInputStream(file);
                            jsonData = new Scanner(is, "UTF-8").useDelimiter("\\A").next();
                            ;
                            SceneCollection sc = SceneCollection.fromJSON(JSONHelper.getReader(jsonData), null);
                            Toast.makeText(context, context.getString(R.string.scene_backup_restored), Toast.LENGTH_SHORT).show();
                            if (sc.scenes != null)
                                for (Scene scene : sc.scenes)
                                    sceneCollection.addScene(scene);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            Toast.makeText(context, context.getString(R.string.scene_backup_restore_failed), Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            Log.w("RESTORE_BACKUP", jsonData);
                            Toast.makeText(context, context.getString(R.string.scene_backup_restore_failed), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        AlertDialog dialog = builder.create();//AlertDialog dialog; create like this outside onClick
        dialog.show();
    }
}
