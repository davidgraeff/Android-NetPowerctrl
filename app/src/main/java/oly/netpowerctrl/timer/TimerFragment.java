package oly.netpowerctrl.timer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.devices.DevicesFragment;
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.pluginservice.PluginService;
import oly.netpowerctrl.pluginservice.onServiceReady;
import oly.netpowerctrl.ui.widgets.FloatingActionButton;
import oly.netpowerctrl.utils.AnimationController;

public class TimerFragment extends Fragment implements onCollectionUpdated<TimerCollection, Timer>, AdapterView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener, onServiceReady {
    private TimerAdapter timerAdapter;
    private TextView progressText;
    private SwipeRefreshLayout mPullToRefreshLayout;

    public TimerFragment() {
    }

    private void refresh(PluginService service) {
        if (service == null)
            return;

        TimerCollection c = AppData.getInstance().timerCollection;
        if (c.refresh(service))
            AnimationController.animateBottomViewIn(progressText);
    }

    @Override
    public void onResume() {
        super.onResume();
        PluginService.observersServiceReady.register(this);
    }

    @Override
    public void onPause() {
        PluginService.observersServiceReady.unregister(this);
        progressText.setVisibility(View.GONE);
        TimerCollection c = AppData.getInstance().timerCollection;
        c.unregisterObserver(this);
        if (timerAdapter != null)
            timerAdapter.finish();
        super.onPause();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_alarms, container, false);
        progressText = (TextView) view.findViewById(R.id.progressText);

        TimerCollection c = AppData.getInstance().timerCollection;
        timerAdapter = new TimerAdapter(getActivity(), c);
        timerAdapter.start();

        c.registerObserver(this);

        View empty = view.findViewById(android.R.id.empty);
        ListView mListView = (ListView) view.findViewById(R.id.list_timer);
        mListView.setOnItemClickListener(this);
        mListView.setEmptyView(empty);
        mListView.setAdapter(timerAdapter);

        ///// For pull to refresh
        mPullToRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.ptr_layout);
        mPullToRefreshLayout.setOnRefreshListener(this);
        mPullToRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        ///// END: For pull to refresh

        Button btnChangeToDevices = (Button) view.findViewById(R.id.btnChangeToDevices);
        boolean hasDevices = AppData.getInstance().deviceCollection.hasDevices();

        btnChangeToDevices.setVisibility(hasDevices ? View.GONE : View.VISIBLE);
        btnChangeToDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.getNavigationController().changeToFragment(DevicesFragment.class.getName());
            }
        });

        FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.btnAdd);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.getNavigationController().changeToDialog(getActivity(), TimerEditFragmentDialog.class.getName());
            }
        });
        fab.setVisibility(View.INVISIBLE);
        AnimationController.animateBottomViewIn(fab);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.timers, menu);
        menu.findItem(R.id.menu_remove_all_timer).setEnabled(timerAdapter != null && timerAdapter.getCount() > 0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                onRefresh();
                return true;
            case R.id.menu_add_timer:
                MainActivity.getNavigationController().changeToDialog(getActivity(), TimerEditFragmentDialog.class.getName());
                return true;
            case R.id.menu_remove_all_timer:
                //noinspection ConstantConditions
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.delete_all_alarms)
                        .setMessage(R.string.confirmation_delete_all_alarms)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                TimerCollection c = AppData.getInstance().timerCollection;
                                c.removeAll();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();
                return true;
            case R.id.menu_help:
                //noinspection ConstantConditions
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.menu_help)
                        .setMessage(R.string.help_timers)
                        .setIcon(android.R.drawable.ic_menu_help).show();
                return true;

        }
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        TimerEditFragmentDialog fragment = (TimerEditFragmentDialog)
                Fragment.instantiate(getActivity(), TimerEditFragmentDialog.class.getName());
        fragment.setParameter(timerAdapter.getAlarm(position));

        MainActivity.getNavigationController().changeToDialog(getActivity(), fragment);
    }

    @Override
    public boolean onServiceReady(PluginService service) {
        if (!AppData.observersOnDataLoaded.dataLoaded)
            return true;
        refresh(service);
        return false;
    }

    @Override
    public void onServiceFinished() {

    }

    @Override
    public boolean updated(@NonNull TimerCollection timerCollection, Timer timer, @NonNull ObserverUpdateActions action, int position) {
        boolean inProgress = timerCollection.isRequestActive();
        if (inProgress) {
            progressText.setText(getString(R.string.alarm_receiving, timerCollection.countAllDeviceAlarms()));
            AnimationController.animateBottomViewIn(progressText);
        } else {
            Activity a = getActivity();
            if (a != null) {
                a.invalidateOptionsMenu();
                AnimationController.animateBottomViewOut(progressText);
            }
        }
        mPullToRefreshLayout.setRefreshing(inProgress);
        return true;
    }

    @Override
    public void onRefresh() {
        refresh(PluginService.getService());
    }
}
