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
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.transfer.GDrive;

/**
 * Neighbour discovery is activated if this fragment is on screen.
 */
public class GDriveFragment extends Fragment implements GDrive.GDriveConnectionState {
    private Switch gDrive_switch;

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

    private boolean suspendSwitch;

    @Override
    public void gDriveConnected(boolean connected, boolean canceled) {
        if (canceled)
            SharedPrefs.setGDriveEnabled(connected);

        suspendSwitch = true;
        gDrive_switch.setChecked(connected);
        suspendSwitch = false;
        progressBar.setVisibility(View.GONE);

        GDrive gDrive = NetpowerctrlActivity.instance.gDrive;
        if (connected) {
            gDrive.getListOfBackups();
        } else if (gDrive.isError())
            statusText.setText(gDrive.getErrorMessage());
        else
            statusText.setText(R.string.gDriveDisconnected);
    }

    @Override
    public void showProgress(boolean inProgress, String text) {
        statusText.setText(text);
        progressBar.setVisibility(inProgress ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDetach() {
        NetpowerctrlActivity.instance.gDrive.setObserver(null, null);

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
            NetpowerctrlActivity.instance.gDrive.onStart(NetpowerctrlActivity.instance);
        } else {
            progressBar.setVisibility(View.GONE);
            NetpowerctrlActivity.instance.gDrive.onStop();
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
                        .setIcon(android.R.drawable.ic_dialog_alert).show();
                return true;
            }
        }
        return false;
    }

    private TextView statusText;
    private ProgressBar progressBar;
    oly.netpowerctrl.transfer.GDriveBackupsAdapter GDriveBackupsAdapter;

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
                //TODO delete files + restore files
            }
        });

        Button btn = (Button) view.findViewById(R.id.button);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NetpowerctrlActivity.instance.gDrive.createNewBackup();
            }
        });

        NetpowerctrlActivity.instance.gDrive.setObserver(this, GDriveBackupsAdapter);

        gDrive_switch = (Switch) getActivity().findViewById(R.id.gDrive_switch);
        if (SharedPrefs.gDriveEnabled() && NetpowerctrlActivity.instance.gDrive.isConnected())
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
}
