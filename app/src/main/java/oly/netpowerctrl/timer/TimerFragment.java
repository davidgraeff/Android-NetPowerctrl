package oly.netpowerctrl.timer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
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
import android.widget.Toast;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.devices.DevicePort;
import oly.netpowerctrl.devices.DevicesFragment;
import oly.netpowerctrl.listen_service.ListenService;
import oly.netpowerctrl.listen_service.PluginInterface;
import oly.netpowerctrl.listen_service.onServiceReady;
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.network.onHttpRequestResult;
import oly.netpowerctrl.utils.AnimationController;
import oly.netpowerctrl.utils.controls.FloatingActionButton;
import oly.netpowerctrl.utils.controls.SwipeDismissListViewTouchListener;

public class TimerFragment extends Fragment implements onCollectionUpdated<TimerController, Timer>, AdapterView.OnItemClickListener, SwipeDismissListViewTouchListener.DismissCallbacks, onHttpRequestResult, SwipeRefreshLayout.OnRefreshListener, onServiceReady {
    private TimerAdapter timerAdapter;
    private TextView progressText;
    private SwipeRefreshLayout mPullToRefreshLayout;

    public TimerFragment() {
    }

    private void refresh(ListenService service) {
        if (service == null)
            return;

        TimerController c = AppData.getInstance().timerController;
        if (c.refresh(service))
            AnimationController.animateViewInOut(progressText, true, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        ListenService service = ListenService.getService();
        if (service == null)
            ListenService.observersServiceReady.register(this);
        else
            refresh(service);
    }

    @Override
    public void onPause() {
        ListenService.observersServiceReady.unregister(this);
        AnimationController.animateViewInOut(progressText, false, false);
        TimerController c = AppData.getInstance().timerController;
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

        TimerController c = AppData.getInstance().timerController;
        timerAdapter = new TimerAdapter(getActivity(), c);
        timerAdapter.start();

        c.registerObserver(this);

        View empty = view.findViewById(android.R.id.empty);
        ListView mListView = (ListView) view.findViewById(R.id.list_timer);
        mListView.setOnItemClickListener(this);
        mListView.setEmptyView(empty);
        mListView.setAdapter(timerAdapter);

        ///// For swiping elements out (hiding)
        SwipeDismissListViewTouchListener touchListener =
                new SwipeDismissListViewTouchListener(
                        mListView, this);
        mListView.setOnTouchListener(touchListener);
        mListView.setOnScrollListener(touchListener.makeScrollListener());
        ///// END: For swiping elements out (hiding)

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
                refresh(ListenService.getService());
                return true;
            case R.id.menu_add_timer:
                MainActivity.getNavigationController().changeToDialog(getActivity(), TimerEditFragmentDialog.class.getName());
                return true;
            case R.id.menu_remove_all_timer:
                //noinspection ConstantConditions
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.delete_all_groups)
                        .setMessage(R.string.confirmation_delete_all_alarms)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                TimerController c = AppData.getInstance().timerController;
                                c.clear();
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
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        TimerEditFragmentDialog fragment = (TimerEditFragmentDialog)
                Fragment.instantiate(getActivity(), TimerEditFragmentDialog.class.getName());
        fragment.setParameter(timerAdapter.getAlarm(i));

        MainActivity.getNavigationController().changeToDialog(getActivity(), fragment);
    }

    @Override
    public boolean canDismiss(int position) {
        return true;
    }

    @Override
    public void onDismiss(int dismissedPosition) {
        Timer timer = timerAdapter.getAlarm(dismissedPosition);
        timerAdapter.remove(dismissedPosition);
        timerAdapter.notifyDataSetChanged();
        PluginInterface plugin = timer.port.device.getPluginInterface();
        plugin.removeAlarm(timer, this);
    }

    @Override
    public void httpRequestResult(DevicePort oi, boolean success, String error_message) {
        Toast.makeText(getActivity(), R.string.alarm_saving_now, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void httpRequestStart(@SuppressWarnings("UnusedParameters") DevicePort oi) {

    }

    @Override
    public boolean onServiceReady(ListenService service) {
        refresh(service);
        return false;
    }

    @Override
    public void onServiceFinished() {

    }

    @Override
    public boolean updated(TimerController timerController, Timer timer, ObserverUpdateActions action, int position) {
        boolean inProgress = timerController.isRequestActive();
        if (inProgress) {
            progressText.setText(getString(R.string.alarm_receiving, timerController.countAllDeviceAlarms()));
            AnimationController.animateViewInOut(progressText, true, false);
        } else {
            Activity a = getActivity();
            if (a != null) {
                a.invalidateOptionsMenu();
                AnimationController.animateViewInOut(progressText, false, false);
            }
        }
        mPullToRefreshLayout.setRefreshing(inProgress);
        return true;
    }

    @Override
    public void onRefresh() {
        refresh(ListenService.getService());
    }
}
