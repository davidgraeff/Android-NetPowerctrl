package oly.netpowerctrl.timer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
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
import oly.netpowerctrl.application_state.NetpowerctrlService;
import oly.netpowerctrl.application_state.PluginInterface;
import oly.netpowerctrl.application_state.RuntimeDataController;
import oly.netpowerctrl.application_state.onServiceReady;
import oly.netpowerctrl.device_ports.DevicePort;
import oly.netpowerctrl.devices.DevicesFragment;
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.network.AsyncRunnerResult;
import oly.netpowerctrl.utils_gui.AnimationController;
import oly.netpowerctrl.utils_gui.SwipeDismissListViewTouchListener;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

public class TimerFragment extends Fragment implements TimerController.IAlarmsUpdated, AdapterView.OnItemClickListener, SwipeDismissListViewTouchListener.DismissCallbacks, AsyncRunnerResult, OnRefreshListener, onServiceReady {
    private TimerAdapter timerAdapter;
    private TextView progressText;
    private PullToRefreshLayout mPullToRefreshLayout;
    private AnimationController animationController;


    public TimerFragment() {
    }

    private void refresh(NetpowerctrlService service) {
        if (service == null)
            return;

        TimerController c = RuntimeDataController.getDataController().timerController;
        if (c.refresh(service))
            AnimationController.animateViewInOut(progressText, true, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        NetpowerctrlService service = NetpowerctrlService.getService();
        if (service == null)
            NetpowerctrlService.observersServiceReady.register(this);
        else
            refresh(service);
    }

    @Override
    public void onPause() {
        NetpowerctrlService.observersServiceReady.unregister(this);
        AnimationController.animateViewInOut(progressText, false, false);
        TimerController c = RuntimeDataController.getDataController().timerController;
        c.unregisterObserver(this);
        if (timerAdapter != null)
            timerAdapter.finish();
        super.onPause();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_timer, container, false);
        progressText = (TextView) view.findViewById(R.id.progressText);

        TimerController c = RuntimeDataController.getDataController().timerController;
        timerAdapter = new TimerAdapter(getActivity(), c);
        timerAdapter.start();

        c.registerObserver(this);

        View empty = view.findViewById(android.R.id.empty);
        ListView mListView = (ListView) view.findViewById(R.id.list_timer);
        mListView.setOnItemClickListener(this);
        mListView.setEmptyView(empty);
        mListView.setAdapter(timerAdapter);

        animationController.setAdapter(timerAdapter);
        animationController.setListView(mListView);
        timerAdapter.setRemoveAnimation(animationController);

        ///// For swiping elements out (hiding)
        SwipeDismissListViewTouchListener touchListener =
                new SwipeDismissListViewTouchListener(
                        mListView, this);
        mListView.setOnTouchListener(touchListener);
        mListView.setOnScrollListener(touchListener.makeScrollListener());
        ///// END: For swiping elements out (hiding)

        ///// For pull to refresh
        mPullToRefreshLayout = (PullToRefreshLayout) view.findViewById(R.id.ptr_layout);
        ActionBarPullToRefresh.from(getActivity())
                .allChildrenArePullable()
                .listener(this)
                .setup(mPullToRefreshLayout);
        ///// END: For pull to refresh

        Button btnChangeToDevices = (Button) view.findViewById(R.id.btnChangeToDevices);
        boolean hasDevices = RuntimeDataController.getDataController().deviceCollection.hasDevices();

        btnChangeToDevices.setVisibility(hasDevices ? View.GONE : View.VISIBLE);
        btnChangeToDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.getNavigationController().changeToFragment(DevicesFragment.class.getName());
            }
        });

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
                refresh(NetpowerctrlService.getService());
                return true;
            case R.id.menu_add_timer:
                TimerEditPreferences fragment = (TimerEditPreferences)
                        Fragment.instantiate(getActivity(), TimerEditPreferences.class.getName());
                fragment.setParameter(null);
                //noinspection ConstantConditions
                getFragmentManager().beginTransaction().addToBackStack(null).
                        replace(R.id.content_frame, fragment).commit();
                return true;
            case R.id.menu_remove_all_timer:
                //noinspection ConstantConditions
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.delete_all_groups)
                        .setMessage(R.string.confirmation_delete_all_alarms)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                TimerController c = RuntimeDataController.getDataController().timerController;
                                // Delete all alarms
                                for (int i = 0; i < timerAdapter.getCount(); ++i)
                                    c.removeAlarm(timerAdapter.getAlarm(i), null);
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
        animationController = new AnimationController(getActivity());
        setHasOptionsMenu(true);
    }

    @Override
    public boolean alarmsUpdated(boolean addedOrRemoved, boolean inProgress) {
        if (inProgress) {
            TimerController c = RuntimeDataController.getDataController().timerController;
            progressText.setText(getString(R.string.alarm_receiving, c.countAllDeviceAlarms()));
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
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        TimerEditPreferences fragment = (TimerEditPreferences)
                Fragment.instantiate(getActivity(), TimerEditPreferences.class.getName());
        fragment.setParameter(timerAdapter.getAlarm(i));
        //noinspection ConstantConditions
        getFragmentManager().beginTransaction().addToBackStack(null).
                replace(R.id.content_frame, fragment).commit();
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
    public void asyncRunnerResult(DevicePort oi, boolean success, String error_message) {
        Toast.makeText(getActivity(), R.string.alarm_saving_now, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void asyncRunnerStart(@SuppressWarnings("UnusedParameters") DevicePort oi) {

    }

    @Override
    public void onRefreshStarted(View view) {
        refresh(NetpowerctrlService.getService());
    }

    @Override
    public boolean onServiceReady(NetpowerctrlService service) {
        refresh(service);
        return false;
    }

    @Override
    public void onServiceFinished() {

    }
}
