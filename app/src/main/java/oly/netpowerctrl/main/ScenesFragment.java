package oly.netpowerctrl.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.OutletCommandGroup;
import oly.netpowerctrl.listadapter.ScenesListAdapter;
import oly.netpowerctrl.shortcut.ShortcutCreatorActivity;
import oly.netpowerctrl.utils.GridOrListFragment;
import oly.netpowerctrl.utils.ListItemMenu;

/**
 */
public class ScenesFragment extends GridOrListFragment implements ListItemMenu, PopupMenu.OnMenuItemClickListener {
    private final static int ACTIVITY_REQUEST_ADDGROUP = 12;

    public ScenesFragment() {
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
                                // Delete all scenes
                                NetpowerctrlActivity._this.adapterUpdateManger.adpGroups.deleteAll();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();
                return true;
            }

            case R.id.menu_add_scene: {
                Intent it = new Intent(getActivity(), ShortcutCreatorActivity.class);
                it.putExtra("groups", true);
                startActivityForResult(it, ACTIVITY_REQUEST_ADDGROUP);
                return true;
            }

        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACTIVITY_REQUEST_ADDGROUP && resultCode == Activity.RESULT_OK) {
            Bundle shortcut_bundle = data.getExtras();
            Intent groupIntent = shortcut_bundle.getParcelable(Intent.EXTRA_SHORTCUT_INTENT);
            shortcut_bundle = groupIntent.getExtras();
            OutletCommandGroup og = OutletCommandGroup.fromString(shortcut_bundle.getString("commands"),
                    getActivity());
            NetpowerctrlActivity._this.adapterUpdateManger.adpGroups.addScene(og);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        ScenesListAdapter adapter = NetpowerctrlActivity._this.adapterUpdateManger.adpGroups;
        adapter.setListItemMenu(this);

        mListView.setAdapter(adapter);
        setAutoCheckDataAvailable(true);
        return view;
    }

    @Override
    public void onMenuItemClicked(View v, int position) {
        mListView.setTag(position);
        PopupMenu popup = new PopupMenu(getActivity(), v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.scenes_item, popup.getMenu());

        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        final int position = (Integer) mListView.getTag();
        OutletCommandGroup og = (OutletCommandGroup) NetpowerctrlActivity._this.adapterUpdateManger.adpGroups.getItem(position);

        switch (menuItem.getItemId()) {
            case R.id.menu_edit_scene: {
                Intent it = new Intent(getActivity(), ShortcutCreatorActivity.class);
                it.putExtra("groups", true);
                it.putExtra("load", og.toString());
                startActivityForResult(it, ACTIVITY_REQUEST_ADDGROUP);
                return true;
            }
            case R.id.menu_remove_scene: {
                NetpowerctrlActivity._this.adapterUpdateManger.adpGroups.removeScene(position);
                return true;
            }
        }
        return false;
    }
}