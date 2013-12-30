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

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.OutletInfo;
import oly.netpowerctrl.listadapter.OutledSwitchListAdapter;
import oly.netpowerctrl.service.DeviceQuery;
import oly.netpowerctrl.utils.GridOrListFragment;

/**
 */
public class OutletsFragment extends GridOrListFragment implements AdapterView.OnItemLongClickListener, PopupMenu.OnMenuItemClickListener {
    private OutledSwitchListAdapter adapter;

    public OutletsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.outlets, menu);
        boolean hiddenShown = adapter.getIsShowingHidden();
        menu.findItem(R.id.menu_showhidden).setVisible(!hiddenShown);
        menu.findItem(R.id.menu_hidehidden).setVisible(hiddenShown);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_requery: {
                DeviceQuery.sendBroadcastQuery(getActivity());
                return true;
            }
            case R.id.menu_showhidden: {
                adapter.setShowHidden(true);
                getActivity().invalidateOptionsMenu();
                return true;
            }
            case R.id.menu_hidehidden: {
                adapter.setShowHidden(false);
                getActivity().invalidateOptionsMenu();
                return true;
            }
        }
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        adapter = NetpowerctrlActivity._this.adapterUpdateManger.adpOutlets;
        mListView.setOnItemLongClickListener(this);

        mListView.setAdapter(adapter);
        setAutoCheckDataAvailable(true);
        return view;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        mListView.setTag(position);
        OutletInfo oi = (OutletInfo) adapter.getItem(position);

        PopupMenu popup = new PopupMenu(getActivity(), view);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.outlets_item, popup.getMenu());

        popup.getMenu().findItem(R.id.menu_hide_outlet).setVisible(!oi.Hidden);
        popup.getMenu().findItem(R.id.menu_unhide_outlet).setVisible(oi.Hidden);
        popup.getMenu().findItem(R.id.menu_up).setVisible(position > 0);
        popup.getMenu().findItem(R.id.menu_down).setVisible(position < adapter.getCount());

        popup.setOnMenuItemClickListener(this);
        popup.show();
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        final int position = (Integer) mListView.getTag();
        final OutletInfo oi = (OutletInfo) adapter.getItem(position);

        switch (menuItem.getItemId()) {
            case R.id.menu_rename_outlet: {
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

                alert.setTitle(getResources().getString(R.string.outlet_rename_title));
                alert.setMessage(getResources().getString(R.string.outlet_rename_message));

                final EditText input = new EditText(alert.getContext());
                input.setText(oi.UserDescription.isEmpty() ? oi.Description : oi.UserDescription);
                alert.setView(input);

                alert.setPositiveButton(getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        oi.UserDescription = input.getText().toString();
                        adapter.update();
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
                adapter.update();
                return true;
            }
            case R.id.menu_unhide_outlet: {
                oi.Hidden = false;
                adapter.update();
                return true;
            }

            case R.id.menu_up: {
                return true;
            }

            case R.id.menu_down: {
                return true;
            }
        }
        return false;
    }
}
