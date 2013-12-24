package oly.netpowerctrl.main;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
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
import oly.netpowerctrl.utils.DeviceConfigureEvent;
import oly.netpowerctrl.utils.DeviceInfo;
import oly.netpowerctrl.utils.SharedPrefs;

/**
 */
public class ConfiguredDevicesListFragment extends Fragment implements AbsListView.OnItemClickListener, PopupMenu.OnMenuItemClickListener, DeviceConfigureEvent {
    private AbsListView mListView;

    public ConfiguredDevicesListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.configured_device, menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_item, container, false);

        // Set the adapter
        mListView = (AbsListView) view.findViewById(android.R.id.list);
        ((AdapterView<ListAdapter>) mListView).setAdapter(NetpowerctrlActivity._this.adapterUpdateManger.adpConfiguredDevices);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        NetpowerctrlActivity._this.adapterUpdateManger.adpConfiguredDevices.setDeviceConfigureEvent(this);

        return view;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_delete_all_devices: {
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.delete_all_devices)
                        .setMessage(R.string.confirmation_delete_all_devices)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Delete all devices
                                NetpowerctrlActivity._this.adapterUpdateManger.deleteAllConfiguredDevices();
                            }})
                        .setNegativeButton(android.R.string.no, null).show();

                return true;
            }

        }
        return false;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
  		final int position = (Integer)mListView.getTag();
  		DeviceInfo current_device = NetpowerctrlActivity._this.adapterUpdateManger.configuredDevices.get(position);

  	    switch (menuItem.getItemId()) {
  	    case R.id.menu_configure_device: {
            SharedPrefs.SaveTempDevice(getActivity(), current_device);

            Fragment fragment = DevicePreferencesDialog.instantiateNew(getActivity());
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction().addToBackStack(null).replace(R.id.content_frame, fragment).commit();
			return true;
  		}

  	    case R.id.menu_delete_device: {
  	    	if (!current_device.isConfigured())
  	    		return true;

			new AlertDialog.Builder(getActivity())
				.setTitle(R.string.delete_device)
				.setMessage(R.string.confirmation_delete_device)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int whichButton) {
                        NetpowerctrlActivity._this.adapterUpdateManger.deleteDevice(position);
				    }})
				 .setNegativeButton(android.R.string.no, null).show();
			return true;
  		}

  	    case R.id.menu_copy_device: {
            NetpowerctrlActivity._this.adapterUpdateManger.CopyDevice(current_device);
			return true;
  		}

  	    default:
  	    	return false;
  	    }
    }

    @Override
    public void onConfigureDevice(View v, int position) {
        mListView.setTag(position);
		PopupMenu popup = new PopupMenu(getActivity(), v);
	    MenuInflater inflater = popup.getMenuInflater();
		DeviceInfo current_device = NetpowerctrlActivity._this.adapterUpdateManger.configuredDevices.get(position);
        inflater.inflate(R.menu.configured_device_item, popup.getMenu());

		popup.setOnMenuItemClickListener(this);
	    popup.show();
    }
}
