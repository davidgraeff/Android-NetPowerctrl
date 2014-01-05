package oly.netpowerctrl.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.datastructure.SceneCollection;
import oly.netpowerctrl.listadapter.ScenesListAdapter;
import oly.netpowerctrl.shortcut.ShortcutCreatorActivity;
import oly.netpowerctrl.shortcut.Shortcuts;
import oly.netpowerctrl.utils.GridOrListFragment;
import oly.netpowerctrl.utils.JSONHelper;
import oly.netpowerctrl.utils.ListItemMenu;

/**
 */
public class ScenesFragment extends GridOrListFragment implements ListItemMenu, PopupMenu.OnMenuItemClickListener {
    private final static int ACTIVITY_REQUEST_ADDGROUP = 12;
    private ScenesListAdapter adapter;

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
                                NetpowerctrlActivity.instance.adpScenes.deleteAll();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();
                return true;
            }

            case R.id.menu_add_scene: {
                Intent it = new Intent(getActivity(), ShortcutCreatorActivity.class);
                it.putExtra(ShortcutCreatorActivity.CREATE_SCENE, true);
                startActivityForResult(it, ACTIVITY_REQUEST_ADDGROUP);
                return true;
            }

            case R.id.menu_backup_scenes: {
                SceneCollection sc = SceneCollection.fromScenes(adapter.getScenes());
                JSONHelper jh = new JSONHelper();
                Context context = getActivity();
                try {
                    sc.toJSON(jh.createWriter());
                    Calendar t = Calendar.getInstance();
                    String default_name = DateFormat.getMediumDateFormat(context).format(t.getTime()) + " - " + DateFormat.getTimeFormat(context).format(t.getTime());
                    File file = new File(getActivity().getExternalFilesDir("backup"), default_name + ".json");
                    OutputStream os = new FileOutputStream(file);
                    String backupString = jh.getString();
                    os.write(backupString.getBytes());
                    os.close();
                    Toast.makeText(context, context.getString(R.string.scene_backup_created) + ": " + default_name, Toast.LENGTH_SHORT).show();
                } catch (IOException ignored) {
                    Toast.makeText(context, context.getString(R.string.scene_backup_failed) + " " + ignored.getMessage(), Toast.LENGTH_LONG).show();
                }
                return true;
            }

            case R.id.menu_restore_scenes: {
                Toast.makeText(getActivity(), getActivity().getString(R.string.scene_backup_restored), Toast.LENGTH_SHORT).show();
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
            try {
                Scene og = Scene.fromJSON(JSONHelper.getReader(shortcut_bundle.getString(ShortcutCreatorActivity.RESULT_SCENE)));
                NetpowerctrlActivity.instance.adpScenes.addScene(og);
            } catch (IOException ignored) {
                Toast.makeText(getActivity(), R.string.error_saving_scenes, Toast.LENGTH_SHORT);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = NetpowerctrlActivity.instance.adpScenes;
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

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
        Scene og = (Scene) adapter.getItem(position);

        switch (menuItem.getItemId()) {
            case R.id.menu_edit_scene: {
                JSONHelper h = new JSONHelper();
                try {
                    Intent it = new Intent(getActivity(), ShortcutCreatorActivity.class);
                    it.putExtra(ShortcutCreatorActivity.CREATE_SCENE, true);
                    og.toJSON(h.createWriter());
                    it.putExtra(ShortcutCreatorActivity.LOAD_SCENE, h.getString());
                    startActivityForResult(it, ACTIVITY_REQUEST_ADDGROUP);
                } catch (IOException ignored) {
                }
                return true;
            }
            case R.id.menu_remove_scene: {
                NetpowerctrlActivity.instance.adpScenes.removeScene(position);
                return true;
            }
            case R.id.menu_add_homescreen: {
                Context context = getActivity();
                Intent extra = Shortcuts.createShortcutExecutionIntent(getActivity(), og, false);
                Intent shortcutIntent = Shortcuts.createShortcut(getActivity(), extra, og.sceneName);
                shortcutIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                context.getApplicationContext().sendBroadcast(shortcutIntent);
                Toast.makeText(context, context.getString(R.string.shortcut_created), Toast.LENGTH_SHORT).show();
                return true;
            }
        }
        return false;
    }
}