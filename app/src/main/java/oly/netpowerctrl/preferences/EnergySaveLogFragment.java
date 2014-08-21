package oly.netpowerctrl.preferences;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.utils.Logging;
import oly.netpowerctrl.utils_gui.DoneCancelFragmentHelper;

public class EnergySaveLogFragment extends ListFragment {
    private final ArrayList<String> listItems = new ArrayList<>();
    DoneCancelFragmentHelper doneCancelFragmentHelper = new DoneCancelFragmentHelper();
    private ArrayAdapter<String> arrayAdapter;

    public EnergySaveLogFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //noinspection ConstantConditions
        arrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, listItems);
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        super.onStart();

        //noinspection ConstantConditions
        doneCancelFragmentHelper.setTitle(getActivity(), R.string.log_screen);
        setListAdapter(arrayAdapter);
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

    @Override
    public void onDestroy() {
        doneCancelFragmentHelper.restoreTitle(getActivity());
        super.onDestroy();
    }

    private void loadData() {
        listItems.clear();
        File f = Logging.getLogFile();
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
                if (Logging.logFile == null)
                    Logging.logFile = Logging.getLogFile();
                if (Logging.logFile.exists()) {
                    if (Logging.logFile.delete())
                        Logging.logFile = null;
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
                intent.putExtra(Intent.EXTRA_TEXT, Logging.getStringFromFile());
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


}
