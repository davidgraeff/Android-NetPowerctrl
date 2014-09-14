package oly.netpowerctrl.scenes;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.device_ports.DevicePortsCreateSceneAdapter;
import oly.netpowerctrl.device_ports.DevicePortsListAdapter;
import oly.netpowerctrl.device_ports.DevicePortsListFragment;

/**
 */
public class EditSceneIncludedFragment extends DevicePortsListFragment {
    private boolean isTwoPaneFragment;
    private DevicePortsListAdapter adapter_available;

    public EditSceneIncludedFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mAdapter != null && mAdapter.getCount() > 0)
            inflater.inflate(R.menu.scene_included_actions, menu);
    }

    public void setTwoPaneFragment(boolean isTwoPaneFragment) {
        this.isTwoPaneFragment = isTwoPaneFragment;
    }

    public void setAdapterAvailable(DevicePortsListAdapter adapter_available) {
        this.adapter_available = adapter_available;
    }

    public void checkEmpty() {
        final View view = getView();
        assert view != null;

        mListView.setEmptyView(view.findViewById(R.id.empty));
        TextView textView = (TextView) view.findViewById(R.id.empty_text);
        if (isTwoPaneFragment)
            textView.setText(R.string.scene_create_include_twopane);
        else
            textView.setText(R.string.scene_create_include_onepane);
        textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_media_ff, 0);
    }

    /*
 * ActionBar icon clicked
 */
    public boolean onOptionsItemSelected(MenuItem item) {
        DevicePortsCreateSceneAdapter adapter_included = (DevicePortsCreateSceneAdapter) mAdapter;
        switch (item.getItemId()) {
            case R.id.menu_switch_all_on:
                adapter_included.switchAllOn();
                return true;
            case R.id.menu_switch_all_off:
                adapter_included.switchAllOff();
                return true;
            case R.id.menu_switch_all_toogle:
                adapter_included.toggleAll();
                return true;
            case R.id.menu_switch_all_ignore:
                adapter_included.clear();
                adapter_available.getSource().updateNow();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
