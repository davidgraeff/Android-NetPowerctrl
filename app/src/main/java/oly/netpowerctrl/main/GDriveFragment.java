package oly.netpowerctrl.main;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.drive.Metadata;

import oly.netpowerctrl.R;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.transfer.GDrive;
import oly.netpowerctrl.transfer.GDriveCreateBackupTask;

/**
 * Neighbour discovery is activated if this fragment is on screen.
 */
public class GDriveFragment extends Fragment implements GDrive.GDriveConnectionState, PopupMenu.OnMenuItemClickListener {
    oly.netpowerctrl.transfer.GDriveBackupsAdapter GDriveBackupsAdapter;
    private Switch gDrive_switch;
    private boolean suspendSwitch;
    private TextView statusText;
    private ProgressBar progressBar;
    private View controlsIfLoggedIn;
    private int clickedPosition;

    public GDriveFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        //set the actionbar to use the custom view (can also be done with a style)
        //noinspection ConstantConditions
        ActionBar bar = getActivity().getActionBar();
        assert bar != null;
        bar.setDisplayShowCustomEnabled(true);
        bar.setDisplayShowHomeEnabled(true);
        bar.setDisplayShowTitleEnabled(false);
        bar.setCustomView(R.layout.gdrive_switch);
    }

    @Override
    public void onStop() {
        super.onStop();
        GDriveBackupsAdapter.clear();
    }

    @Override
    public void gDriveConnected(final boolean connected, final boolean canceled) {
        if (canceled)
            SharedPrefs.setGDriveEnabled(connected);

        suspendSwitch = true;
        gDrive_switch.setChecked(connected);
        suspendSwitch = false;
        progressBar.setVisibility(View.GONE);

        GDrive gDrive = MainActivity.instance.gDrive;
        if (connected) {
            controlsIfLoggedIn.setVisibility(View.VISIBLE);
            gDrive.getListOfBackups(GDriveBackupsAdapter);
        } else if (gDrive.isError()) {
            GDriveBackupsAdapter.clear();
            statusText.setText(gDrive.getErrorMessage());
            controlsIfLoggedIn.setVisibility(View.GONE);
        } else {
            GDriveBackupsAdapter.clear();
            statusText.setText(R.string.gDriveDisconnected);
            controlsIfLoggedIn.setVisibility(View.GONE);
        }
    }

    @Override
    public void showProgress(boolean inProgress, String text) {
        statusText.setText(text);
        progressBar.setVisibility(inProgress ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDetach() {
        MainActivity.instance.gDrive.setObserver(null);

        super.onDetach();
        //noinspection ConstantConditions
        ActionBar bar = getActivity().getActionBar();
        assert bar != null;
        bar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP |
                ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
    }

    private void enableGDrive(boolean enable) {
        if (suspendSwitch)
            return;

        SharedPrefs.setGDriveEnabled(enable);
        if (enable) {
            progressBar.setVisibility(View.VISIBLE);
            MainActivity.instance.gDrive.onStart(MainActivity.instance);
        } else {
            progressBar.setVisibility(View.GONE);
            GDriveBackupsAdapter.clear();
            MainActivity.instance.gDrive.onStop();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.only_help, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_help: {
                //noinspection ConstantConditions
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.menu_help)
                        .setMessage(R.string.help_gDrive)
                        .setIcon(android.R.drawable.ic_menu_help).show();
                return true;
            }
        }
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_gdrive, container, false);

        statusText = (TextView) view.findViewById(R.id.hintText);
        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        //noinspection ConstantConditions
        ListView list = (ListView) view.findViewById(R.id.list);
        list.setEmptyView(view.findViewById(android.R.id.empty));

        GDriveBackupsAdapter = new oly.netpowerctrl.transfer.GDriveBackupsAdapter(getActivity());
        list.setAdapter(GDriveBackupsAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                clickedPosition = i;
                PopupMenu popup = new PopupMenu(getActivity(), view);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.g_drive, popup.getMenu());

                popup.setOnMenuItemClickListener(GDriveFragment.this);
                popup.show();
            }
        });

        Button btn = (Button) view.findViewById(R.id.btnSave);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.instance.gDrive.createNewBackup(new GDriveCreateBackupTask.BackupDoneSuccess() {
                    @Override
                    public void done() {
                        MainActivity.instance.gDrive.getListOfBackups(GDriveBackupsAdapter);
                    }
                });
            }
        });
        btn = (Button) view.findViewById(R.id.btnRefresh);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.instance.gDrive.getListOfBackups(GDriveBackupsAdapter);
            }
        });
        btn = (Button) view.findViewById(R.id.btnLogout);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.instance.gDrive.resetAccount();
            }
        });

        controlsIfLoggedIn = view.findViewById(R.id.buttons);

        MainActivity.instance.gDrive.setObserver(this);

        gDrive_switch = (Switch) getActivity().findViewById(R.id.gDrive_switch);
        if (SharedPrefs.gDriveEnabled() && MainActivity.instance.gDrive.isConnected())
            gDriveConnected(true, false);
        else
            gDriveConnected(false, false);
        gDrive_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                enableGDrive(b);
            }
        });

        return view;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        final Metadata item = (Metadata) GDriveBackupsAdapter.getItem(clickedPosition);

        switch (menuItem.getItemId()) {
            case R.id.menu_gDrive_remove: {
                MainActivity.instance.gDrive.deleteBackup(item);
                return true;
            }
            case R.id.menu_gDrive_restore: {
                MainActivity.instance.gDrive.restoreBackup(item.getDriveId());
                return true;
            }
        }
        return false;
    }
}
