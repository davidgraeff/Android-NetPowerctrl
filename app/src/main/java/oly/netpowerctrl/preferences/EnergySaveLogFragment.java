package oly.netpowerctrl.preferences;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import oly.netpowerctrl.R;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.ui.notifications.InAppNotifications;
import oly.netpowerctrl.utils.ActionBarTitle;
import oly.netpowerctrl.utils.Logging;

public class EnergySaveLogFragment extends ListFragment {
    private final ArrayList<String> listItems = new ArrayList<>();
    private final ActionBarTitle actionBarTitle = new ActionBarTitle();
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
        actionBarTitle.setTitle(getActivity(), R.string.log_screen);

        setListAdapter(arrayAdapter);
        setEmptyText(getString(R.string.log_no_records));
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
        App.getMainThreadHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                setListAdapter(arrayAdapter);
                ListView v = getListView();
                if (v != null)
                    v.setItemChecked(arrayAdapter.getCount() - 1, true);
            }
        }, 10);
    }

    @Override
    public void onDestroy() {
        actionBarTitle.restoreTitle(getActivity());
        super.onDestroy();
    }

    private void loadData() {
        listItems.clear();
        File f = Logging.getLogFile(getActivity());
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
                    Logging.logFile = Logging.getLogFile(getActivity());
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
                if (Logging.logFile == null)
                    Logging.logFile = Logging.getLogFile(getActivity());
                App.setErrorReportContentLogFile(Logging.logFile.getAbsolutePath());
                InAppNotifications.silentException(null);
                App.setErrorReportContentCrash();
                Toast.makeText(getActivity(), R.string.log_data_send, Toast.LENGTH_SHORT).show();
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
