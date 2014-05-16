package oly.netpowerctrl.main;

import android.app.AlertDialog;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupMenu;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.devices.DevicesFragment;

public class TimerFragment extends Fragment implements PopupMenu.OnMenuItemClickListener {

    public TimerFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_timer, container, false);

        Button btn = (Button) view.findViewById(R.id.btnDonate);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //noinspection ConstantConditions
                PopupMenu popup = new PopupMenu(getActivity(), view);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.donate_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(TimerFragment.this);
                popup.show();
            }
        });

        View empty = view.findViewById(android.R.id.empty);
        ((ListView) view.findViewById(R.id.list_timer)).setEmptyView(empty);

        Button btnChangeToDevices = (Button) view.findViewById(R.id.btnChangeToDevices);
        boolean hasDevices = NetpowerctrlApplication.getDataController().deviceCollection != null && NetpowerctrlApplication.getDataController().deviceCollection.hasDevices();

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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.timers, menu);
        menu.findItem(R.id.menu_remove_all_timer).setEnabled(false);
        menu.findItem(R.id.menu_add_timer).setEnabled(false);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add_timer:
                return true;
            case R.id.menu_remove_all_timer:
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
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_main_beer:
                MainActivity.instance.donate.buy("beer");
                return true;
            case R.id.menu_main_beer2:
                MainActivity.instance.donate.buy("beer2");
                return true;
            case R.id.menu_main_beer3:
                MainActivity.instance.donate.buy("beer3");
                return true;
            case R.id.menu_main_beer4:
                MainActivity.instance.donate.buy("beer4");
                return true;
        }
        return false;
    }
}
