package oly.netpowerctrl.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.service.ShortcutCreatorActivity;
import oly.netpowerctrl.utils.SharedPrefs;

/**
 */
public class GroupListFragment extends Fragment implements AbsListView.OnItemClickListener {
    private AbsListView mListView;

    public GroupListFragment() {
    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.scenes, menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_remove_scene: {
                new AlertDialog.Builder(getActivity())
				.setTitle(R.string.delete_all_groups)
				.setMessage(R.string.confirmation_delete_all_groups)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int whichButton) {
				    	// Delete all groups
                        NetpowerctrlActivity._this.adapterUpdateManger.adpGroups.deleteAll();
				    }})
				 .setNegativeButton(android.R.string.no, null).show();
                return true;
            }

            case R.id.menu_add_scene: {
				Intent it = new Intent(NetpowerctrlActivity._this, ShortcutCreatorActivity.class);
				it.putExtra("groups", true);
				it.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
				startActivityForResult(it, NetpowerctrlActivity.ACTIVITY_REQUEST_ADDGROUP);
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
        ((AdapterView<ListAdapter>) mListView).setAdapter(NetpowerctrlActivity._this.adapterUpdateManger.adpGroups);

// Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    }
}