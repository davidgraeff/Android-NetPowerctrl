package oly.netpowerctrl.main;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.OutletInfo;
import oly.netpowerctrl.listadapter.NotReachableUpdate;
import oly.netpowerctrl.listadapter.OutletSwitchListAdapter;
import oly.netpowerctrl.utils.GridOrListFragment;

/**
 */
public class OutletsFragment extends GridOrListFragment implements AdapterView.OnItemLongClickListener, PopupMenu.OnMenuItemClickListener, NotReachableUpdate {
    private OutletSwitchListAdapter adapter;
    private TextView hinText;

    public OutletsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = NetpowerctrlActivity.instance.adpOutlets;
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy() {
        adapter.setNotReachableObserver(null);
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.outlets, menu);
        boolean hiddenShown = false;
        if (adapter != null)
            hiddenShown = adapter.getIsShowingHidden();
        //noinspection ConstantConditions
        menu.findItem(R.id.menu_showhidden).setVisible(!hiddenShown);
        //noinspection ConstantConditions
        menu.findItem(R.id.menu_hidehidden).setVisible(hiddenShown);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_requery: {
                NetpowerctrlApplication.instance.detectNewDevicesAndReachability();
                return true;
            }
            case R.id.menu_showhidden: {
                adapter.setShowHidden(true);
                //noinspection ConstantConditions
                getActivity().invalidateOptionsMenu();
                return true;
            }
            case R.id.menu_hidehidden: {
                adapter.setShowHidden(false);
                //noinspection ConstantConditions
                getActivity().invalidateOptionsMenu();
                return true;
            }
            case R.id.menu_showdevicename: {
                adapter.setShowDeviceNames(!adapter.isShowDeviceNames());
                //noinspection ConstantConditions
                getActivity().invalidateOptionsMenu();
                return true;
            }
            case R.id.menu_sortAlphabetically: {
                adapter.sortAlphabetically();
                NetpowerctrlApplication.instance.saveConfiguredDevices(false);
            }
        }
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        hinText = (TextView) view.findViewById(R.id.hintText);
        adapter.setNotReachableObserver(this);
        mListView.setOnItemLongClickListener(this);
        mListView.setAdapter(adapter);
        setAutoCheckDataAvailable(true);
        return view;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        mListView.setTag(position);
        OutletInfo oi = adapter.getItem(position);

        //noinspection ConstantConditions
        PopupMenu popup = new PopupMenu(getActivity(), view);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.outlets_item, popup.getMenu());

        //noinspection ConstantConditions
        popup.getMenu().findItem(R.id.menu_hide_outlet).setVisible(!oi.Hidden);
        //noinspection ConstantConditions
        popup.getMenu().findItem(R.id.menu_unhide_outlet).setVisible(oi.Hidden);
        //noinspection ConstantConditions
        popup.getMenu().findItem(R.id.menu_up).setVisible(position > 0);
        //noinspection ConstantConditions
        popup.getMenu().findItem(R.id.menu_down).setVisible(position < adapter.getCount() - 1);

        popup.setOnMenuItemClickListener(this);
        popup.show();
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        final int position = (Integer) mListView.getTag();
        final OutletInfo oi = adapter.getItem(position);

        switch (menuItem.getItemId()) {
            case R.id.menu_rename_outlet: {
                //noinspection ConstantConditions
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

                alert.setTitle(getResources().getString(R.string.outlet_rename_title));
                alert.setMessage(getResources().getString(R.string.outlet_rename_message).replaceFirst("%s", oi.getDeviceDescription()));

                final EditText input = new EditText(alert.getContext());
                input.setText(oi.getDescription());
                alert.setView(input);

                alert.setPositiveButton(getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //noinspection ConstantConditions
                        oi.setDescriptionByUser(input.getText().toString());
                        adapter.onDevicesUpdated();
                    }
                });

                alert.setNegativeButton(getResources().getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });

                alert.show();
                return true;
            }
            case R.id.menu_hide_outlet: {
                oi.Hidden = true;
                NetpowerctrlApplication.instance.saveConfiguredDevices(true);
                return true;
            }
            case R.id.menu_unhide_outlet: {
                oi.Hidden = false;
                NetpowerctrlApplication.instance.saveConfiguredDevices(true);
                return true;
            }

            case R.id.menu_up: {
                adapter.swapPosition(position, position - 1);
                NetpowerctrlApplication.instance.saveConfiguredDevices(false);
                return true;
            }

            case R.id.menu_down: {
                adapter.swapPosition(position, position + 1);
                NetpowerctrlApplication.instance.saveConfiguredDevices(false);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onNotReachableUpdate(int not_reachable) {
        if (not_reachable > 0) {
            hinText.setText(NetpowerctrlApplication.instance.getString(R.string.error_not_reachable) + ": " + Integer.valueOf(not_reachable).toString());
            hinText.setVisibility(View.VISIBLE);
        } else {
            hinText.setVisibility(View.GONE);
        }
    }
}
