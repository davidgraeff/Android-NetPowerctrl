package oly.netpowerctrl.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
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
import android.widget.PopupMenu;
import android.widget.Toast;

import java.io.IOException;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.dynamicgid.DynamicGridView;
import oly.netpowerctrl.listadapter.ScenesListAdapter;
import oly.netpowerctrl.shortcut.ShortcutCreatorActivity;
import oly.netpowerctrl.shortcut.Shortcuts;
import oly.netpowerctrl.utils.JSONHelper;
import oly.netpowerctrl.utils.OnBackButton;

/**
 */
public class ScenesFragment extends Fragment implements
        PopupMenu.OnMenuItemClickListener, AdapterView.OnItemClickListener, OnBackButton {
    private final static int ACTIVITY_REQUEST_ADD_GROUP = 12;
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
                //noinspection ConstantConditions
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.delete_all_groups)
                        .setMessage(R.string.confirmation_delete_all_groups)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Delete all scenes
                                adapter.deleteAll();
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
                adapter.addScene(og);
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

    private DynamicGridView mListView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scenes, container, false);
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
        mListView.setAutomaticNumColumns(true, 200);
        mListView.setEmptyView(view.findViewById(android.R.id.empty));
        onConfigurationChanged(getResources().getConfiguration());
        return view;
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
                adapter.removeScene(position);
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
        if (mListView.isEditMode()) {
            mListView.setTag(position);
            @SuppressWarnings("ConstantConditions")
            PopupMenu popup = new PopupMenu(getActivity(), view);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.scenes_item, popup.getMenu());

            popup.setOnMenuItemClickListener(this);
            popup.show();
        } else {
            adapter.executeScene(position);
            //noinspection ConstantConditions
            Toast.makeText(getActivity(),
                    getActivity().getString(R.string.scene_executed, adapter.getScenes().get(position).sceneName),
                    Toast.LENGTH_SHORT).show();
        }
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