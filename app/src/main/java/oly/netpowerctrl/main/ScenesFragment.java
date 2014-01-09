package oly.netpowerctrl.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import java.io.IOException;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.dragdrop.DragDropEnabled;
import oly.netpowerctrl.dragdrop.DragListener;
import oly.netpowerctrl.dragdrop.DragNDropListView;
import oly.netpowerctrl.dragdrop.DropListener;
import oly.netpowerctrl.listadapter.ScenesListAdapter;
import oly.netpowerctrl.shortcut.ShortcutCreatorActivity;
import oly.netpowerctrl.shortcut.Shortcuts;
import oly.netpowerctrl.utils.Backup;
import oly.netpowerctrl.utils.GridOrListFragment;
import oly.netpowerctrl.utils.JSONHelper;
import oly.netpowerctrl.utils.ListItemMenu;

/**
 */
public class ScenesFragment extends GridOrListFragment implements ListItemMenu, PopupMenu.OnMenuItemClickListener, AdapterView.OnItemClickListener {
    private final static int ACTIVITY_REQUEST_ADD_GROUP = 12;
    private ScenesListAdapter adapter;

    public ScenesFragment() {
    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.scenes, menu);

        boolean dragDropEnabled = false;
        if (adapter != null) {
            if (mListView instanceof DragDropEnabled) {
                dragDropEnabled = ((DragDropEnabled) mListView).isDragDropEnabled();
            }
        }

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
            case R.id.menu_remove_scene: {
                //noinspection ConstantConditions
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.delete_all_groups)
                        .setMessage(R.string.confirmation_delete_all_groups)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Delete all scenes
                                NetpowerctrlActivity.instance.getScenesAdapter().deleteAll();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();
                return true;
            }

            case R.id.menu_add_scene: {
                Intent it = new Intent(getActivity(), ShortcutCreatorActivity.class);
                it.putExtra(ShortcutCreatorActivity.CREATE_SCENE, true);
                startActivityForResult(it, ACTIVITY_REQUEST_ADD_GROUP);
                return true;
            }

            case R.id.menu_backup_scenes: {
                Backup.createScenesBackup(getActivity(), adapter.getScenes());
                return true;
            }

            case R.id.menu_restore_scenes: {
                Backup.restoreScenesBackup(getActivity(), adapter);
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

        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACTIVITY_REQUEST_ADD_GROUP && resultCode == Activity.RESULT_OK) {
            Bundle shortcut_bundle = data.getExtras();
            assert shortcut_bundle != null;
            Intent groupIntent = shortcut_bundle.getParcelable(Intent.EXTRA_SHORTCUT_INTENT);
            assert groupIntent != null;
            shortcut_bundle = groupIntent.getExtras();
            try {
                assert shortcut_bundle != null;
                Scene og = Scene.fromJSON(JSONHelper.getReader(shortcut_bundle.getString(ShortcutCreatorActivity.RESULT_SCENE)));
                NetpowerctrlActivity.instance.getScenesAdapter().addScene(og);
            } catch (IOException ignored) {
                //noinspection ConstantConditions
                Toast.makeText(getActivity(), R.string.error_saving_scenes, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = NetpowerctrlActivity.instance.getScenesAdapter();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        emptyText.setText(R.string.empty_no_scenes);

        adapter.setListItemMenu(this);

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
    public void onMenuItemClicked(View v, int position) {
        mListView.setTag(position);
        @SuppressWarnings("ConstantConditions")
        PopupMenu popup = new PopupMenu(getActivity(), v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.scenes_item, popup.getMenu());

        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        final int position = (Integer) mListView.getTag();
        Scene og = (Scene) adapter.getItem(position);

        switch (menuItem.getItemId()) {
            case R.id.menu_edit_scene: {
                JSONHelper h = new JSONHelper();
                try {
                    Intent it = new Intent(getActivity(), ShortcutCreatorActivity.class);
                    it.putExtra(ShortcutCreatorActivity.CREATE_SCENE, true);
                    og.toJSON(h.createWriter());
                    it.putExtra(ShortcutCreatorActivity.LOAD_SCENE, h.getString());
                    startActivityForResult(it, ACTIVITY_REQUEST_ADD_GROUP);
                } catch (IOException ignored) {
                }
                return true;
            }
            case R.id.menu_remove_scene: {
                NetpowerctrlActivity.instance.getScenesAdapter().removeScene(position);
                return true;
            }
            case R.id.menu_add_homescreen: {
                @SuppressWarnings("ConstantConditions")
                Context context = getActivity().getApplicationContext();
                Intent extra = Shortcuts.createShortcutExecutionIntent(getActivity(), og, false, false);
                Intent shortcutIntent = Shortcuts.createShortcut(getActivity(), extra, og.sceneName);
                shortcutIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                assert context != null;
                context.sendBroadcast(shortcutIntent);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        adapter.executeScene(position);
        //noinspection ConstantConditions
        Toast.makeText(getActivity(),
                getActivity().getString(R.string.scene_executed, adapter.getScenes().get(position).sceneName),
                Toast.LENGTH_SHORT).show();
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
            adapter.saveScenes();
        }

    };
}