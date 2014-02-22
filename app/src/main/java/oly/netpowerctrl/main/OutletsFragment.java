package oly.netpowerctrl.main;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.DevicePort;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.dynamicgid.DynamicGridView;
import oly.netpowerctrl.listadapter.DevicePortsExecuteAdapter;
import oly.netpowerctrl.listadapter.NotReachableUpdate;
import oly.netpowerctrl.network.DeviceQuery;
import oly.netpowerctrl.network.DeviceUpdateStateOrTimeout;
import oly.netpowerctrl.shortcut.EditShortcutActivity;
import oly.netpowerctrl.utils.OnBackButton;

/**
 */
public class OutletsFragment extends Fragment implements PopupMenu.OnMenuItemClickListener,
        NotReachableUpdate, AdapterView.OnItemClickListener, OnBackButton {
    private DevicePortsExecuteAdapter adapter;
    private TextView hintText;

    public OutletsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new DevicePortsExecuteAdapter(getActivity());

        NetpowerctrlApplication.instance.detectNewDevicesAndReachability();
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy() {
        adapter.finish();
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.outlets, menu);
        boolean hiddenShown = false;
        if (adapter != null) {
            hiddenShown = adapter.getIsShowingHidden();
        }
        //noinspection ConstantConditions
        menu.findItem(R.id.menu_showhidden).setVisible(!hiddenShown);
        //noinspection ConstantConditions
        menu.findItem(R.id.menu_hidehidden).setVisible(hiddenShown);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_requery: {
                new DeviceQuery(new DeviceUpdateStateOrTimeout() {
                    @Override
                    public void onDeviceTimeout(DeviceInfo di) {
                    }

                    @Override
                    public void onDeviceUpdated(DeviceInfo di) {
                    }

                    @Override
                    public void onDeviceQueryFinished(List<DeviceInfo> timeout_devices) {
                        //noinspection ConstantConditions
                        Toast.makeText(getActivity(),
                                getActivity().getString(R.string.devices_refreshed,
                                        NetpowerctrlApplication.getDataController().getReachableConfiguredDevices(),
                                        NetpowerctrlApplication.getDataController().newDevices.size()),
                                Toast.LENGTH_SHORT).show();
                    }
                });
                return true;
            }
            case R.id.menu_add_scene:
                Scene scene = new Scene();
                scene.sceneItems = adapter.getScene();
                final String sceneJSON = scene.toJSON();
                Intent it = new Intent(getActivity(), EditShortcutActivity.class);
                it.putExtra(EditShortcutActivity.EDIT_SCENE_NOT_SHORTCUT, true);
                it.putExtra(EditShortcutActivity.LOAD_SCENE, sceneJSON);
                startActivity(it);
                return true;
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

            case R.id.menu_sortAlphabetically: {
                adapter.sortAlphabetically();
                NetpowerctrlApplication.getDataController().saveConfiguredDevices(false);
            }
        }
        return false;
    }

    private DynamicGridView mListView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_outlets, container, false);
        mListView = (DynamicGridView) view.findViewById(android.R.id.list);
        mListView.setOnItemClickListener(this);
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(getActivity(), getActivity().getString(R.string.hint_stop_edit), Toast.LENGTH_SHORT).show();
                mListView.startEditMode();
                return false;
            }
        });
        mListView.setAdapter(adapter);
        hintText = (TextView) view.findViewById(R.id.hintText);
        adapter.setNotReachableObserver(this);
        mListView.setAutomaticNumColumns(true, 350);
        mListView.setEmptyView(view.findViewById(R.id.loading));
        // We assign the empty view after a short delay time,
        // to reduce visual flicker on app start, where data
        // is loaded with a high chance within the first 500ms.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mListView.getEmptyView().setVisibility(View.GONE);
                mListView.setEmptyView(view.findViewById(android.R.id.empty));
            }
        }, 1000);

        Button btn = (Button) view.findViewById(R.id.btnChangeToDevices);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NetpowerctrlActivity.instance.changeToFragment(DevicesListFragment.class.getName());
            }
        });

        return view;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        final int position = (Integer) mListView.getTag();
        final DevicePort oi = adapter.getItem(position);

        switch (menuItem.getItemId()) {
            case R.id.menu_rename_outlet: {
                //noinspection ConstantConditions
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

                alert.setTitle(getString(R.string.outlet_rename_title));
                alert.setMessage(getString(R.string.outlet_rename_message, oi.getDeviceDescription()));

                final EditText input = new EditText(alert.getContext());
                input.setText(oi.getDescription());
                alert.setView(input);

                alert.setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //noinspection ConstantConditions
                        oi.setDescriptionByUser(input.getText().toString());
                        adapter.onDevicesUpdated(null);
                    }
                });

                alert.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });

                alert.show();
                return true;
            }
            case R.id.menu_hide_outlet: {
                oi.Hidden = true;
                NetpowerctrlApplication.getDataController().saveConfiguredDevices(true);
                return true;
            }
            case R.id.menu_unhide_outlet: {
                oi.Hidden = false;
                NetpowerctrlApplication.getDataController().saveConfiguredDevices(true);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onNotReachableUpdate(List<DeviceInfo> not_reachable) {
        if (not_reachable.size() > 0) {
            String devices = "";
            for (DeviceInfo di : not_reachable) {
                devices += di.DeviceName + " ";
            }
            hintText.setText(NetpowerctrlApplication.instance.getString(R.string.error_not_reachable) + ": " + devices);
            hintText.setVisibility(View.VISIBLE);
        } else {
            hintText.setVisibility(View.GONE);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        mListView.setTag(position);
        DevicePort oi = adapter.getItem(position);

        //noinspection ConstantConditions
        PopupMenu popup = new PopupMenu(getActivity(), view);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.outlets_item, popup.getMenu());

        //noinspection ConstantConditions
        popup.getMenu().findItem(R.id.menu_hide_outlet).setVisible(!oi.Hidden);
        //noinspection ConstantConditions
        popup.getMenu().findItem(R.id.menu_unhide_outlet).setVisible(oi.Hidden);

        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

    @Override
    public boolean onBackButton() {
        if (mListView.isEditMode()) {
            mListView.stopEditMode();
            return true;
        }
        return false;
    }
}
