package oly.netpowerctrl.utils;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringBufferInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import oly.netpowerctrl.R;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.preferences.SharedPrefs;

/**
 * Created by david on 11.05.14.
 */
public class Logging {
    private final File logFile;
    private final SimpleDateFormat sdf;
    public boolean mLogDetect;
    private boolean mLogEnergy;
    private boolean mLogWidgets;
    private boolean mLogAlarm;
    private boolean mLogExtensions;
    private boolean mLogMain;
    // Have to be a member. Local variable would be of no use -> weak reference
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            SharedPrefs sharedPrefs = SharedPrefs.getInstance();
            if (sharedPrefs.isPreferenceNameLogs(s)) {
                mLogEnergy = sharedPrefs.logEnergy();
                mLogAlarm = sharedPrefs.logAlarm();
                mLogWidgets = sharedPrefs.logWidget();
                mLogExtensions = sharedPrefs.logExtensions();
                mLogDetect = sharedPrefs.logDetection();
                mLogMain = mLogAlarm | mLogWidgets | mLogExtensions | mLogEnergy | mLogDetect;
            }
        }
    };
    private LogChanged logChangedListener;

    private Logging() {
        SharedPrefs sharedPrefs = SharedPrefs.getInstance();
        mLogEnergy = sharedPrefs.logEnergy();
        mLogAlarm = sharedPrefs.logAlarm();
        mLogWidgets = sharedPrefs.logWidget();
        mLogExtensions = sharedPrefs.logExtensions();
        mLogDetect = sharedPrefs.logDetection();
        mLogMain = mLogAlarm | mLogWidgets | mLogExtensions | mLogEnergy | mLogDetect;

        sdf = new SimpleDateFormat("yyyy-MM-dd|HH:mm");

        // Listen to preferences changes
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(App.instance);
        sp.registerOnSharedPreferenceChangeListener(preferenceChangeListener);


        logFile = new File(App.instance.getExternalFilesDir("logs"), "main_log.txt");
        File parent = logFile.getParentFile();
        if (parent != null)
            if (!parent.mkdirs() && !parent.isDirectory()) {
                Log.w("log", "failed to create log dir");
                return;
            }

        if (!logFile.exists()) {
            try {
                if (!logFile.createNewFile()) {
                    Log.e("EnergySaveLogFragment", logFile.getName() + " create failed");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Singleton
     */
    public static Logging getInstance() {
        return SingletonHolder.instance;
    }

    public BufferedReader getReader() {
        try {
            InputStream inStream = new FileInputStream(logFile);
            InputStreamReader inputReader = new InputStreamReader(inStream);
            return new BufferedReader(inputReader);
        } catch (FileNotFoundException ignored) {
        }
        return new BufferedReader(new InputStreamReader(new StringBufferInputStream("")));
    }

    public void clear() {
        if (logFile.exists())
            if (!logFile.delete())
                Log.e("Logging", "Failed to delete " + logFile.getAbsolutePath());
    }

    public File getLogFile() {
        return logFile;
    }

    //
//    private static String convertStreamToString(InputStream is) throws Exception {
//        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
//        StringBuilder sb = new StringBuilder();
//        String line;
//        while ((line = reader.readLine()) != null) {
//            sb.append(line).append("\n");
//        }
//        reader.close();
//        return sb.toString();
//    }
//
//    public static String getStringFromFile(Context context) {
//        FileInputStream fin;
//        String ret = "";
//        try {
//            fin = new FileInputStream(getLogFile(context));
//            ret = convertStreamToString(fin);
//            //Make sure you close all streams.
//            fin.close();
//        } catch (Exception ignored) {
//        }
//        return ret;
//    }


    public void logMain(String text) {
        if (!mLogMain) return;
        appendLog("MAIN|" + text);
    }

    public void logDetect(String text) {
        if (!mLogDetect) return;
        appendLog("DTC|" + text);
    }

    public void logWidgets(String text) {
        if (!mLogWidgets) return;
        appendLog("WDG|" + text);
    }

    public void logAlarm(String text) {
        if (!mLogAlarm) return;
        appendLog("ALM|" + text);
    }

    public void logEnergy(String text) {
        if (!mLogEnergy) return;
        appendLog("PWR|" + text);
    }

    public void logExtensions(String text) {
        if (!mLogExtensions) return;
        appendLog("EXT|" + text);
    }

    synchronized public void appendLog(String text) {
        text = sdf.format(new Date()) + "|" + text.replace('\n', '\t');

        try {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.flush();
            buf.close();

            if (logChangedListener != null)
                logChangedListener.onLogChanged();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setLogChangedListener(LogChanged logChangedListener) {
        this.logChangedListener = logChangedListener;
    }

    public String getLogFileSize() {
        return App.instance.getString(R.string.log_file_size, logFile.length() / 1024.0f);
    }

    public interface LogChanged {
        void onLogChanged();
    }

    private static class SingletonHolder {
        public static final Logging instance = new Logging();
    }
}

