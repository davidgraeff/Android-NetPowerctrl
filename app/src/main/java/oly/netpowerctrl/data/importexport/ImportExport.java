package oly.netpowerctrl.data.importexport;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.data.LoadStoreCollections;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.Streams;

/**
 * Created by david on 07.09.14.
 */
public class ImportExport {
    public static void clearData(DataService dataService) {
        //noinspection ResultOfMethodCallIgnored
        dataService.getDir("images", 0).delete();
        LoadStoreCollections loadStoreCollections = dataService.getLoadStoreCollections();
        loadStoreCollections.clear(dataService.groups.getStorage());
        loadStoreCollections.clear(dataService.connections.getStorage());
        loadStoreCollections.clear(dataService.executables.getStorage());
        loadStoreCollections.clear(dataService.credentials.getStorage());
        loadStoreCollections.clear(dataService.timers.getStorage());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void exportData(Context context, Uri targetFileUri) {
        String password = SharedPrefs.getInstance().getBackupPassword();
        SharedPrefs.getInstance().setBackupPassword(password);

        File destinationPathFile = context.getFilesDir();
        File destinationPathFileTemp = context.getDir("temp", 0);

        // Create temporary zip file
        File zipFileTemp = new File(destinationPathFileTemp, "temp.zip");
        if (zipFileTemp.exists())
            if (!zipFileTemp.delete()) {
                destinationPathFileTemp.delete();
                return;
            }

        try {
            ZipFile zipFile = new ZipFile(zipFileTemp);
            ZipParameters parameters = new ZipParameters();
            parameters.setEncryptionMethod(Zip4jConstants.ENC_METHOD_AES);
            parameters.setAesKeyStrength(Zip4jConstants.AES_STRENGTH_256);
            parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
            parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_ULTRA);
            parameters.setPassword(password);

            String[] children = context.fileList();
            for (String aChildren : children) {
                //noinspection ResultOfMethodCallIgnored
                zipFile.addFolder(new File(destinationPathFile, aChildren), parameters);
            }
            zipFile.setComment(context.getPackageName());

        } catch (ZipException e) {
            e.printStackTrace();
            destinationPathFileTemp.delete();
            return;
        }


        try {
            ParcelFileDescriptor pfd =
                    context.getContentResolver().
                            openFileDescriptor(targetFileUri, "w");

            FileOutputStream fileOutputStream =
                    new FileOutputStream(pfd.getFileDescriptor());

            Streams.copy(new FileInputStream(zipFileTemp), fileOutputStream);

            fileOutputStream.close();
            pfd.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        destinationPathFileTemp.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void importData(DataService context, Uri sourceFileUri) {
        File destinationPathFile = context.getFilesDir();
        File destinationPathFileTemp = context.getDir("temp", 0);
        String destinationPathTemp = destinationPathFileTemp.getAbsolutePath();

        String password = SharedPrefs.getInstance().getBackupPassword();
        SharedPrefs.getInstance().setBackupPassword(password);

        // Create temporary zip file
        File zipFileTemp = new File(destinationPathFileTemp, "temp.zip");
        try {
            if (zipFileTemp.exists())
                zipFileTemp.delete();
            zipFileTemp.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            destinationPathFileTemp.delete();
            return;
        }

        // copy zip data
        try {
            InputStream in = context.getContentResolver().openInputStream(sourceFileUri);
            Streams.copy(in, new FileOutputStream(zipFileTemp));
        } catch (IOException e) {
            e.printStackTrace();
            destinationPathFileTemp.delete();
            return;
        }

        // unzip to dest_dir/temp
        try {
            ZipFile zipFile = new ZipFile(zipFileTemp);
            if (zipFile.isEncrypted()) {
                zipFile.setPassword(password);
            }
            zipFile.extractAll(destinationPathTemp);
        } catch (ZipException e) {
            e.printStackTrace();
            destinationPathFileTemp.delete();
            return;
        }

        if (!new File(destinationPathFileTemp, "icons").exists() &&
                !new File(destinationPathFileTemp, "devices").exists()) {
            // no backup file!
            destinationPathFileTemp.delete();
            return;
        }

        // remove current data
        clearData(context);

        String[] children = destinationPathFileTemp.list();
        for (String aChildren : children) {
            //noinspection ResultOfMethodCallIgnored
            if (!new File(destinationPathFileTemp, aChildren).renameTo(new File(destinationPathFile, aChildren))) {
                Log.e("Import", "Failed move " + aChildren);
            }
        }

        destinationPathFileTemp.delete();

        Toast.makeText(context, R.string.import_sucess, Toast.LENGTH_LONG).show();
    }
}
