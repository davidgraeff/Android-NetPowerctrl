package oly.netpowerctrl.main;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.PopupMenu;

import oly.netpowerctrl.R;
import oly.netpowerctrl.preferences.DevicePreferencesDialog;
import oly.netpowerctrl.service.DeviceQuery;
import oly.netpowerctrl.utils.DeviceConfigureEvent;
import oly.netpowerctrl.utils.DeviceInfo;
import oly.netpowerctrl.utils.SharedPrefs;

/**
 */
public class NewDevicesListFragment extends Fragment implements AbsListView.OnItemClickListener, DeviceConfigureEvent {
    private AbsListView mListView;

    public NewDevicesListFragment() {
    }

    @Override
    public void onCreateOptionsMenu(
        Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.unconfigured_device, menu);
    }

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_add_device: {
            DeviceInfo di = new DeviceInfo(getActivity());
            SharedPrefs.SaveTempDevice(getActivity(), di);

            Fragment fragment = DevicePreferencesDialog.instantiateNew(getActivity());
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction().addToBackStack(null).replace(R.id.content_frame, fragment).commit();
            return true;
		}

		case R.id.menu_requery: {
	    	DeviceQuery.sendBroadcastQuery(getActivity());
			return true;
		}

		}
		return false;
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_item, container, false);

        // Set the adapter
        mListView = (AbsListView) view.findViewById(android.R.id.list);
        ((AdapterView<ListAdapter>) mListView).setAdapter(NetpowerctrlActivity._this.adapterUpdateManger.adpNewDevices);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        NetpowerctrlActivity._this.adapterUpdateManger.adpNewDevices.setDeviceConfigureEvent(this);

        return view;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onConfigureDevice(View v, int position) {
        PopupMenu popup = new PopupMenu(getActivity(), v);
        MenuInflater inflater = popup.getMenuInflater();
        DeviceInfo di = NetpowerctrlActivity._this.adapterUpdateManger.newDevices.get(position);

        SharedPrefs.SaveTempDevice(getActivity(), di);

        Fragment fragment = DevicePreferencesDialog.instantiateNew(getActivity());
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().addToBackStack(null).replace(R.id.content_frame, fragment).commit();
    }
}