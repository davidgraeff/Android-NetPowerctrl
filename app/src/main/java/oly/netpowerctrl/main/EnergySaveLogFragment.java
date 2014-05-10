package oly.netpowerctrl.main;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;

public class EnergySaveLogFragment extends ListFragment {
    private ArrayAdapter<String> arrayAdapter;
    private final ArrayList<String> listItems = new ArrayList<String>();
    private CharSequence title_before;

    public EnergySaveLogFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //noinspection ConstantConditions
        arrayAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, listItems);
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy() {
        if (title_before != null)
            //noinspection ConstantConditions
            getActivity().setTitle(title_before);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
        NetpowerctrlApplication.getMainThreadHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ListView v = getListView();
                if (v != null)
                    v.setItemChecked(arrayAdapter.getCount() - 1, true);
            }
        }, 10);
    }

    private void loadData() {
        listItems.clear();
        File f = getLogFile();
        if (f.exists()) {
            try {
                InputStream inStream = new FileInputStream(f);
                InputStreamReader inputReader = new InputStreamReader(inStream);
                BufferedReader reader = new BufferedReader(inputReader);
                String line;
                do {
                    line = reader.readLine();
                    if (line != null)
                        listItems.add(line);
                } while (line != null);
            } catch (FileNotFoundException ignored) {

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //noinspection ConstantConditions
        title_before = getActivity().getTitle();
        getActivity().setTitle(R.string.energy_saving_mode_log);
        setListAdapter(arrayAdapter);
    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.log, menu);
        //noinspection ConstantConditions
        menu.findItem(R.id.menu_remove_log).setVisible(!arrayAdapter.isEmpty());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_remove_log: {
                if (logFile == null)
                    logFile = getLogFile();
                if (logFile.exists()) {
                    if (logFile.delete())
                        logFile = null;
                }
                arrayAdapter.clear();
                //noinspection ConstantConditions
                getActivity().invalidateOptionsMenu();
                return true;
            }
            case R.id.menu_log_send_mail: {
                @SuppressWarnings("ConstantConditions")
                ApplicationInfo info = getActivity().getApplicationContext().getApplicationInfo();
                PackageManager pm = getActivity().getApplicationContext().getPackageManager();
                assert pm != null;
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"david.graeff@web.de"});
                intent.putExtra(Intent.EXTRA_TEXT, getStringFromFile());
                try {
                    assert info != null;
                    //noinspection ConstantConditions
                    intent.putExtra(Intent.EXTRA_SUBJECT, info.loadLabel(pm).toString() + "(" + pm.getPackageInfo(info.packageName, 0).versionName + ")" + " Log | Device: " + Build.MANUFACTURER + " " + Build.DEVICE + "(" + Build.MODEL + ") API: " + Build.VERSION.SDK_INT);
                } catch (PackageManager.NameNotFoundException ignored) {
                }
                intent.setType("plain/html");
                getActivity().startActivity(intent);
                return true;
            }

            case R.id.menu_help: {
                //noinspection ConstantConditions
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.menu_help)
                        .setMessage(R.string.help_log)
                        .setIcon(android.R.drawable.ic_menu_help).show();
                return true;
            }
        }
        return false;
    }


    private static File getLogFile() {
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

    private static String getStringFromFile() {
        FileInputStream fin;
        String ret = "";
        try {
            fin = new FileInputStream(getLogFile());
            ret = convertStreamToString(fin);
            //Make sure you close all streams.
            fin.close();
        } catch (FileNotFoundException ignored) {
        } catch (IOException ignored) {
        } catch (Exception ignored) {
        }
        return ret;
    }

    private static File logFile = null;

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
