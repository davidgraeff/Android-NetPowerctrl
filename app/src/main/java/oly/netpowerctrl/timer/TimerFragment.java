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
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.devices.DevicesFragment;
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.pluginservice.PluginService;
import oly.netpowerctrl.pluginservice.onServiceReady;
import oly.netpowerctrl.ui.RecyclerItemClickListener;
import oly.netpowerctrl.ui.RecyclerViewWithAdapter;
import oly.netpowerctrl.ui.widgets.FloatingActionButton;
import oly.netpowerctrl.utils.AnimationController;
import oly.netpowerctrl.utils.DividerItemDecoration;

public class TimerFragment extends Fragment implements onCollectionUpdated<TimerCollection, Timer>, SwipeRefreshLayout.OnRefreshListener, onServiceReady, PopupMenu.OnMenuItemClickListener {
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
            AnimationController.animateBottomViewIn(progressText, false);
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

    @NonNull
    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_alarms, container, false);

        TimerCollection c = AppData.getInstance().timerCollection;
        timerAdapter = new TimerAdapter(getActivity(), c);
        timerAdapter.start();
        RecyclerViewWithAdapter<TimerAdapter> recyclerViewWithAdapter = new RecyclerViewWithAdapter<>(getActivity(), null, rootView, timerAdapter, R.string.alarms_no_alarms);
        recyclerViewWithAdapter.setOnItemClickListener(new RecyclerItemClickListener(getActivity(), new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public boolean onItemClick(View view, int position, boolean isLongClick) {
                TimerEditFragmentDialog fragment = (TimerEditFragmentDialog)
                        Fragment.instantiate(getActivity(), TimerEditFragmentDialog.class.getName());
                fragment.setParameter(timerAdapter.getAlarm(position));

                MainActivity.getNavigationController().changeToDialog(getActivity(), fragment);
                return false;
            }
        }, null));
        recyclerViewWithAdapter.getRecyclerView().addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST) {
            @Override
            public boolean dividerForPosition(int position) {
                return true;
            }
        });
        progressText = (TextView) rootView.findViewById(R.id.progressText);

        c.registerObserver(this);

        ///// For pull to refresh
        mPullToRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.ptr_layout);
        mPullToRefreshLayout.setOnRefreshListener(this);
        mPullToRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        ///// END: For pull to refresh

        Button btnChangeToDevices = (Button) rootView.findViewById(R.id.btnChangeToDevices);
        boolean hasDevices = AppData.getInstance().deviceCollection.hasDevices();

        btnChangeToDevices.setVisibility(hasDevices ? View.GONE : View.VISIBLE);
        btnChangeToDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.getNavigationController().changeToFragment(DevicesFragment.class.getName());
            }
        });

        FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.btnAdd);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popup = new PopupMenu(getActivity(), view);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.timer_add, popup.getMenu());
                popup.setOnMenuItemClickListener(TimerFragment.this);
                popup.show();
            }
        });
        fab.setVisibility(View.INVISIBLE);
        AnimationController.animateBottomViewIn(fab, false);

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.timers, menu);
        menu.findItem(R.id.menu_remove_all).setEnabled(timerAdapter != null && timerAdapter.getItemCount() > 0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_remove_all:
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
            progressText.setText(getString(R.string.alarm_receiving, timerCollection.countReceivedAlarms(), timerCollection.countAllAlarms()));
            AnimationController.animateBottomViewIn(progressText, false);
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

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        TimerEditFragmentDialog fragmentDialog = (TimerEditFragmentDialog) Fragment.instantiate(getActivity(), TimerEditFragmentDialog.class.getName());
        switch (menuItem.getItemId()) {
            case R.id.menu_timer_android_once:
                fragmentDialog.setArguments(TimerEditFragmentDialog.createArgumentBundle(Timer.TYPE_ONCE, true));
                MainActivity.getNavigationController().changeToDialog(getActivity(), fragmentDialog);
                return true;
            case R.id.menu_timer_android_weekdays:
                fragmentDialog.setArguments(TimerEditFragmentDialog.createArgumentBundle(Timer.TYPE_RANGE_ON_WEEKDAYS, true));
                MainActivity.getNavigationController().changeToDialog(getActivity(), fragmentDialog);
                return true;
            case R.id.menu_timer_device_weekdays:
                fragmentDialog.setArguments(TimerEditFragmentDialog.createArgumentBundle(Timer.TYPE_RANGE_ON_WEEKDAYS, false));
                MainActivity.getNavigationController().changeToDialog(getActivity(), fragmentDialog);
                return true;
        }
        throw new RuntimeException("Menu switch missing entry!");
    }
}
