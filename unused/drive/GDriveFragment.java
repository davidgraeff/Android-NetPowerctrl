package oly.netpowerctrl.backup.drive;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.google.android.gms.drive.Metadata;

import oly.netpowerctrl.R;
import oly.netpowerctrl.main.MainActivity;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

/**
 * Neighbour discovery is activated if this fragment is on screen.
 */
public class GDriveFragment extends Fragment implements GDrive.GDriveConnectionState, PopupMenu.OnMenuItemClickListener, OnRefreshListener {
    public final GDrive gDrive = new GDrive();
    private oly.netpowerctrl.backup.drive.GDriveBackupsAdapter GDriveBackupsAdapter;
    private TextView statusText;
    private PullToRefreshLayout mPullToRefreshLayout;
    private int clickedPosition;

    public GDriveFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        gDrive.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        enableGDrive(true);
    }

    @Override
    public void onPause() {
        GDriveBackupsAdapter.clear();
        gDrive.onStop();
        gDrive.setObserver(null);
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        gDrive.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        gDrive.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void enableGDrive(boolean enable) {
        if (enable) {
            gDrive.setObserver(this);
            gDrive.onStart(MainActivity.instance);
        } else {
            GDriveBackupsAdapter.clear();
            gDrive.onStop();
            gDrive.resetAccount();
        }
    }


    @Override
    public void gDriveConnected(final boolean connected, final boolean canceled) {
        mPullToRefreshLayout.setRefreshing(false);
        if (connected) {
            statusText.setText(R.string.gDriveConnected);
            gDrive.getListOfBackups(GDriveBackupsAdapter);
        } else if (gDrive.isError()) {
            GDriveBackupsAdapter.clear();
            statusText.setText(gDrive.getErrorMessage());
        } else {
            GDriveBackupsAdapter.clear();
            statusText.setText(R.string.gDriveDisconnected);
        }
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void showProgress(boolean inProgress, String text) {
        statusText.setText(text);
        mPullToRefreshLayout.setRefreshing(inProgress);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.g_drive_fragment, menu);

        boolean connected = gDrive.isConnected();
        menu.findItem(R.id.menu_login).setVisible(!connected);
        menu.findItem(R.id.menu_logout).setVisible(connected);
        menu.findItem(R.id.refresh).setVisible(connected);
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
            case R.id.menu_backup:
                gDrive.createNewBackup(new GDriveCreateBackupTask.BackupDoneSuccess() {
                    @Override
                    public void done() {
                        gDrive.getListOfBackups(GDriveBackupsAdapter);
                    }
                });
                return true;
            case R.id.menu_login:
                enableGDrive(true);
                return true;
            case R.id.menu_logout:
                enableGDrive(false);
                return true;
            case R.id.refresh:
                gDrive.getListOfBackups(GDriveBackupsAdapter);
                return true;
        }
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_gdrive, container, false);

        statusText = (TextView) view.findViewById(R.id.hintText);

        //noinspection ConstantConditions
        ListView list = (ListView) view.findViewById(R.id.list);
        list.setEmptyView(view.findViewById(android.R.id.empty));

        GDriveBackupsAdapter = new oly.netpowerctrl.backup.drive.GDriveBackupsAdapter(getActivity());
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

        ///// For pull to refresh
        mPullToRefreshLayout = (PullToRefreshLayout) view.findViewById(R.id.ptr_layout);
        ActionBarPullToRefresh.from(getActivity())
                .allChildrenArePullable()
                .listener(this)
                .setup(mPullToRefreshLayout);
        ///// END: For pull to refresh

        return view;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        final Metadata item = GDriveBackupsAdapter.getItem(clickedPosition);

        switch (menuItem.getItemId()) {
            case R.id.menu_gDrive_remove: {
                gDrive.deleteBackup(item, new GDriveRemoveTask.DoneSuccess() {
                    @Override
                    public void done() {
                        gDrive.getListOfBackups(GDriveBackupsAdapter);
                    }
                });
                return true;
            }
            case R.id.menu_gDrive_restore: {
                gDrive.restoreBackup(item.getDriveId());
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRefreshStarted(View view) {
        gDrive.getListOfBackups(GDriveBackupsAdapter);
    }
}
