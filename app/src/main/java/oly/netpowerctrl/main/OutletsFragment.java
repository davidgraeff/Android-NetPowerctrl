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
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.anelservice.DeviceQuery;
import oly.netpowerctrl.anelservice.DeviceUpdateStateOrTimeout;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.OutletInfo;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.datastructure.SceneOutlet;
import oly.netpowerctrl.dragdrop.DragDropEnabled;
import oly.netpowerctrl.dragdrop.DragListener;
import oly.netpowerctrl.dragdrop.DragNDropListView;
import oly.netpowerctrl.dragdrop.DropListener;
import oly.netpowerctrl.listadapter.NotReachableUpdate;
import oly.netpowerctrl.listadapter.OutletSwitchListAdapter;
import oly.netpowerctrl.utils.GridOrListFragment;

/**
 */
public class OutletsFragment extends GridOrListFragment implements PopupMenu.OnMenuItemClickListener, NotReachableUpdate, AdapterView.OnItemClickListener {
    private OutletSwitchListAdapter adapter;
    private TextView hintText;

    public OutletsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = NetpowerctrlActivity.instance.getOutletsAdapter();
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
        boolean devicesShown = false;
        boolean dragDropEnabled = false;
        if (adapter != null) {
            hiddenShown = adapter.getIsShowingHidden();
            devicesShown = adapter.isShowDeviceNames();
            if (mListView instanceof DragDropEnabled) {
                dragDropEnabled = ((DragDropEnabled) mListView).isDragDropEnabled();
            }
        }
        //noinspection ConstantConditions
        menu.findItem(R.id.menu_showhidden).setVisible(!hiddenShown);
        //noinspection ConstantConditions
        menu.findItem(R.id.menu_hidehidden).setVisible(hiddenShown);

        //noinspection ConstantConditions
        menu.findItem(R.id.menu_showdevicename).setVisible(!devicesShown);
        //noinspection ConstantConditions
        menu.findItem(R.id.menu_hidedevicename).setVisible(devicesShown);

        if (mListView instanceof DragDropEnabled) {
            //noinspection ConstantConditions
            menu.findItem(R.id.menu_showdragdrop).setVisible(!dragDropEnabled);
            //noinspection ConstantConditions
            menu.findItem(R.id.menu_hidedragdrop).setVisible(dragDropEnabled);
        } else {
            //noinspection ConstantConditions
            menu.findItem(R.id.menu_showdragdrop).setVisible(false);
            //noinspection ConstantConditions
            menu.findItem(R.id.menu_hidedragdrop).setVisible(false);
        }
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
                                        NetpowerctrlApplication.instance.getReachableConfiguredDevices(),
                                        NetpowerctrlApplication.instance.newDevices.size()),
                                Toast.LENGTH_SHORT).show();
                    }
                });
                return true;
            }
            case R.id.menu_add_scene:
                //noinspection ConstantConditions
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

                alert.setTitle(getString(R.string.outlet_to_scene_title));
                alert.setMessage(getString(R.string.outlet_to_scene_message));

                final EditText input = new EditText(alert.getContext());
                input.setText("");
                alert.setView(input);

                alert.setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Scene og = new Scene();
                        og.sceneName = input.getText().toString();
                        if (og.sceneName.trim().isEmpty())
                            return;
                        for (int i = 0; i < adapter.getCount(); ++i) {
                            og.add(SceneOutlet.fromOutletInfo(adapter.getItem(i), true));
                        }
                        NetpowerctrlActivity.instance.getScenesAdapter().addScene(og);
                    }
                });

                alert.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });

                alert.show();
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
            case R.id.menu_showdevicename: {
                adapter.setShowDeviceNames(true);
                //noinspection ConstantConditions
                getActivity().invalidateOptionsMenu();
                return true;
            }
            case R.id.menu_hidedevicename: {
                adapter.setShowDeviceNames(false);
                //noinspection ConstantConditions
                getActivity().invalidateOptionsMenu();
                return true;
            }
            case R.id.menu_showdragdrop: {
                ((DragDropEnabled) mListView).setDragDropEnabled(true);
                //noinspection ConstantConditions
                getActivity().invalidateOptionsMenu();
                return true;
            }
            case R.id.menu_hidedragdrop: {
                ((DragDropEnabled) mListView).setDragDropEnabled(false);
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
        assert view != null;
        hintText = (TextView) view.findViewById(R.id.hintText);
        emptyText.setText(R.string.empty_no_outlets);
        adapter.setNotReachableObserver(this);
        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(this);
        if (mListView instanceof DragNDropListView) {
            ((DragNDropListView) mListView).setDropListener(mDropListener);
            ((DragNDropListView) mListView).setDragListener(mDragListener);
        }
        setAutoCheckDataAvailable(true);
        return view;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        final int position = (Integer) mListView.getTag();
        final OutletInfo oi = adapter.getItem(position);

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
                NetpowerctrlApplication.instance.saveConfiguredDevices(true);
                return true;
            }
            case R.id.menu_unhide_outlet: {
                oi.Hidden = false;
                NetpowerctrlApplication.instance.saveConfiguredDevices(true);
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
        OutletInfo oi = adapter.getItem(position);

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

    private DropListener mDropListener = new DropListener() {
        public void onDrop(int from, int to) {
            adapter.onDrop(from, to);
            mListView.invalidateViews();
        }
    };

    private DragListener mDragListener = new DragListener() {
        public void onStartDrag(View itemView) {
            ImageView iv = (ImageView) itemView.findViewById(R.id.MoveHandler);
            if (iv != null) iv.setVisibility(View.INVISIBLE);
        }

        /**
         * Drag/Drop operation finished: Save devices/outlets.
         * @param itemView - the view of the item to be dragged i.e. the drag view
         */
        public void onStopDrag(View itemView) {
            ImageView iv = (ImageView) itemView.findViewById(R.id.MoveHandler);
            if (iv != null) iv.setVisibility(View.VISIBLE);
            NetpowerctrlApplication.instance.saveConfiguredDevices(false);
        }

    };
}
