package oly.netpowerctrl.alarms;

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
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.application_state.PluginInterface;
import oly.netpowerctrl.devices.DevicePort;
import oly.netpowerctrl.devices.DevicesFragment;
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.network.AsyncRunnerResult;
import oly.netpowerctrl.utils.gui.SwipeDismissListViewTouchListener;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

public class TimerFragment extends Fragment implements TimerController.IAlarmsUpdated, AdapterView.OnItemClickListener, SwipeDismissListViewTouchListener.DismissCallbacks, AsyncRunnerResult, OnRefreshListener {
    private TimerAdapter timerAdapter;
    private TextView progressText;
    private PullToRefreshLayout mPullToRefreshLayout;

    public TimerFragment() {
    }

    private void refresh() {
        animateProgressText(true);
        alarmsUpdated(false, true);
        TimerController c = NetpowerctrlApplication.getDataController().timerController;
        c.requestData();
    }

    @Override
    public void onDestroy() {
        TimerController c = NetpowerctrlApplication.getDataController().timerController;
        c.unregisterObserver(this);
        timerAdapter.finish();
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_timer, container, false);
        progressText = (TextView) view.findViewById(R.id.progressText);

        TimerController c = NetpowerctrlApplication.getDataController().timerController;
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
        mPullToRefreshLayout = (PullToRefreshLayout) view.findViewById(R.id.ptr_layout);
        ActionBarPullToRefresh.from(getActivity())
                .allChildrenArePullable()
                .listener(this)
                .setup(mPullToRefreshLayout);
        ///// END: For pull to refresh

        Button btnChangeToDevices = (Button) view.findViewById(R.id.btnChangeToDevices);
        boolean hasDevices = NetpowerctrlApplication.getDataController().deviceCollection.hasDevices();

        btnChangeToDevices.setVisibility(hasDevices ? View.GONE : View.VISIBLE);
        btnChangeToDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.instance.changeToFragment(DevicesFragment.class.getName());
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        refresh();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.timers, menu);
        menu.findItem(R.id.menu_remove_all_timer).setEnabled(timerAdapter.getCount() > 0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_requery:
                refresh();
                return true;
            case R.id.menu_add_timer:
                AlarmEditPreferences fragment = (AlarmEditPreferences)
                        Fragment.instantiate(getActivity(), AlarmEditPreferences.class.getName());
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
                                TimerController c = NetpowerctrlApplication.getDataController().timerController;
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

        setHasOptionsMenu(true);
    }

    private void animateProgressText(final boolean in) {
        float c = progressText.getAlpha();
        if (c >= 1.0f && in || c == 0.0f && !in) {
            return;
        }

        progressText.clearAnimation();

        AlphaAnimation animation1 = new AlphaAnimation(c, in ? 1.0f : 0.0f);
        animation1.setInterpolator(new AccelerateInterpolator());
        animation1.setDuration(1000);
        animation1.setStartOffset(0);
        animation1.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                progressText.setAlpha(in ? 1.0f : 0.0f);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        progressText.startAnimation(animation1);
    }

    @Override
    public boolean alarmsUpdated(boolean addedOrRemoved, boolean inProgress) {
        if (inProgress) {
            TimerController c = NetpowerctrlApplication.getDataController().timerController;
            progressText.setText(getString(R.string.alarm_receiving, c.countAllDeviceAlarms()));
            animateProgressText(true);
        } else {
            Activity a = getActivity();
            if (a != null) {
                a.invalidateOptionsMenu();
                animateProgressText(false);
            }
        }
        mPullToRefreshLayout.setRefreshing(inProgress);
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        AlarmEditPreferences fragment = (AlarmEditPreferences)
                Fragment.instantiate(getActivity(), AlarmEditPreferences.class.getName());
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
    public void onDismiss(AbsListView listView, int[] reverseSortedPositions) {
        for (int position : reverseSortedPositions) {
            Alarm alarm = timerAdapter.getAlarm(position);
            PluginInterface plugin = alarm.port.device.getPluginInterface(NetpowerctrlApplication.getService());
            plugin.removeAlarm(alarm, this);
        }
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
        refresh();
    }
}
