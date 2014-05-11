package oly.netpowerctrl.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.util.Date;

import oly.netpowerctrl.application_state.NetpowerctrlApplication;

/**
 * Created by david on 11.05.14.
 */
public class Logging {
    public static File logFile = null;

    public static File getLogFile() {
        return new File(NetpowerctrlApplication.instance.getExternalFilesDir("logs"), "main_log.txt");
    }

    private static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public static String getStringFromFile() {
        FileInputStream fin;
        String ret = "";
        try {
            fin = new FileInputStream(getLogFile());
            ret = convertStreamToString(fin);
            //Make sure you close all streams.
            fin.close();
        } catch (Exception ignored) {
        }
        return ret;
    }

    synchronized static public void appendLog(String text) {
        text = DateFormat.getDateTimeInstance().format(new Date()) + "\n  " + text;
        //Log.w("log", text);
        //noinspection ConstantConditions
        if (logFile == null) {
            logFile = getLogFile();
            text = "APP START\n" + text;
        }

        File parent = logFile.getParentFile();
        if (parent != null)
            if (!parent.mkdirs()) {
                Log.w("log", "failed to create log dir");
                return;
            }

        if (!logFile.exists()) {
            try {
                if (!logFile.createNewFile()) {
                    Log.e("EnergySaveLogFragment", logFile.getName() + " create failed");
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.flush();
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
