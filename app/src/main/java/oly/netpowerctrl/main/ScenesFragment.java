package oly.netpowerctrl.main;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.nhaarman.listviewanimations.swinginadapters.prepared.ScaleInAnimationAdapter;

import java.io.IOException;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.datastructure.SceneCollection;
import oly.netpowerctrl.dynamicgid.DynamicGridView;
import oly.netpowerctrl.listadapter.ScenesAdapter;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.shortcut.EditShortcutActivity;
import oly.netpowerctrl.shortcut.Shortcuts;
import oly.netpowerctrl.utils.Icons;
import oly.netpowerctrl.utils.JSONHelper;
import oly.netpowerctrl.utils.ListItemMenu;
import oly.netpowerctrl.utils.OnBackButton;

/**
 */
public class ScenesFragment extends Fragment implements
        PopupMenu.OnMenuItemClickListener, AdapterView.OnItemClickListener, OnBackButton, Icons.IconSelected, ListItemMenu {
    private SceneCollection scenes;
    private DynamicGridView mListView;
    private ScenesAdapter scenesAdapter;

    public ScenesFragment() {
    }

    private void assignAdapter() {
        if (SharedPrefs.getAnimationEnabled()) {
            // Add animation to the list
            ScaleInAnimationAdapter animatedAdapter = new ScaleInAnimationAdapter(scenesAdapter);
            animatedAdapter.setAbsListView(mListView);
            mListView.setAbstractDynamicGridAdapter(scenesAdapter);
            mListView.setAdapter(animatedAdapter);
        } else {
            mListView.setAdapter(scenesAdapter);
        }
    }

    private void setListOrGrid(boolean grid) {
        if (!grid) {
            scenesAdapter.setLayoutRes(R.layout.list_icon_item);
            mListView.setMinimumColumnWidth(280);
            mListView.setNumColumns(GridView.AUTO_FIT, mListView.getWidth());
            assignAdapter();
        } else {
            scenesAdapter.setLayoutRes(R.layout.grid_icon_item);
            mListView.setMinimumColumnWidth(150);
            mListView.setNumColumns(GridView.AUTO_FIT, mListView.getWidth());
            assignAdapter();
        }
        SharedPrefs.setScenesList(grid);
    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.scenes, menu);

        //noinspection ConstantConditions
        menu.findItem(R.id.menu_add_scene).setVisible(NetpowerctrlApplication.getDataController().deviceCollection.hasDevices());

        if (scenesAdapter == null || scenesAdapter.getCount() == 0) {
            //noinspection ConstantConditions
            menu.findItem(R.id.menu_remove_scene).setVisible(false);
            //noinspection ConstantConditions
            menu.findItem(R.id.menu_view_list).setVisible(false);
            //noinspection ConstantConditions
            menu.findItem(R.id.menu_view_grid).setVisible(false);
            return;
        }

        boolean isList = scenesAdapter.getLayoutRes() == R.layout.list_icon_item;
        //noinspection ConstantConditions
        menu.findItem(R.id.menu_view_list).setVisible(!isList);
        //noinspection ConstantConditions
        menu.findItem(R.id.menu_view_grid).setVisible(isList);
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
                                scenes.deleteAll();
                                mListView.stopEditMode();
                                getActivity().invalidateOptionsMenu();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();
                return true;
            }

            case R.id.menu_add_scene: {
                Intent it = new Intent(getActivity(), EditShortcutActivity.class);
                it.putExtra(EditShortcutActivity.EDIT_SCENE_NOT_SHORTCUT, true);
                startActivity(it);
                return true;
            }

            case R.id.menu_view_list: {
                setListOrGrid(false);
                //noinspection ConstantConditions
                getActivity().invalidateOptionsMenu();
                return true;
            }

            case R.id.menu_view_grid: {
                setListOrGrid(true);
                //noinspection ConstantConditions
                getActivity().invalidateOptionsMenu();
                return true;
            }

            case R.id.menu_help: {
                //noinspection ConstantConditions
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.menu_help)
                        .setMessage(R.string.help_scene)
                        .setIcon(android.R.drawable.ic_dialog_alert).show();
                return true;
            }
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        //noinspection ConstantConditions
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scenes = NetpowerctrlApplication.getDataController().sceneCollection;
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scenes, container, false);
        assert view != null;
        mListView = (DynamicGridView) view.findViewById(android.R.id.list);
        mListView.setOnItemClickListener(this);
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                //noinspection ConstantConditions
                Toast.makeText(getActivity(), getActivity().getString(R.string.hint_stop_edit), Toast.LENGTH_SHORT).show();
                scenesAdapter.setDisableEditing(true);
                mListView.startEditMode();
                return false;
            }
        });
        scenesAdapter = new ScenesAdapter(getActivity(), scenes);
        scenesAdapter.setListContextMenu(this);
        setListOrGrid(SharedPrefs.getScenesList());
        if (!NetpowerctrlApplication.getDataController().deviceCollection.hasDevices()) {
            //noinspection ConstantConditions
            ((TextView) view.findViewById(R.id.empty_text)).setText(getString(R.string.empty_no_scenes_no_devices));
            Button btnEmpty = ((Button) view.findViewById(R.id.btnChangeToDevices));
            btnEmpty.setVisibility(View.VISIBLE);
            btnEmpty.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    NetpowerctrlActivity.instance.changeToFragment(DevicesFragment.class.getName());
                }
            });
        } else {
            //noinspection ConstantConditions
            ((TextView) view.findViewById(R.id.empty_text)).setText(getString(R.string.empty_no_scenes));
        }
        mListView.setEmptyView(view.findViewById(android.R.id.empty));
        onConfigurationChanged(getResources().getConfiguration());
        return view;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        final int position = (Integer) mListView.getTag();
        Scene scene = scenes.getScene(position);

        switch (menuItem.getItemId()) {
            case R.id.menu_edit_scene: {
                JSONHelper h = new JSONHelper();
                try {
                    Intent it = new Intent(getActivity(), EditShortcutActivity.class);
                    it.putExtra(EditShortcutActivity.EDIT_SCENE_NOT_SHORTCUT, true);
                    scene.toJSON(h.createWriter());
                    it.putExtra(EditShortcutActivity.LOAD_SCENE, h.getString());
                    startActivity(it);
                } catch (IOException ignored) {
                }
                return true;
            }
            case R.id.menu_remove_scene: {
                scenes.removeScene(position);
                if (scenes.length() == 0)
                    mListView.stopEditMode();
                //noinspection ConstantConditions
                getActivity().invalidateOptionsMenu();
                return true;
            }
            case R.id.menu_remove_favourite: {
                scenes.setFavourite(scene, false);
                scenes.saveScenes();
                return true;
            }
            case R.id.menu_set_favourite: {
                scenes.setFavourite(scene, true);
                scenes.saveScenes();
                return true;
            }

            case R.id.menu_icon:
                Icons.show_select_icon_dialog(getActivity(), "scene_icons", this, scene);
                return true;

            case R.id.menu_add_homescreen: {
                //noinspection ConstantConditions
                Shortcuts.createHomeIcon(getActivity().getApplicationContext(), scene);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        if (!mListView.isEditMode()) {
            scenes.executeScene(position);
            //noinspection ConstantConditions
            Toast.makeText(getActivity(),
                    getActivity().getString(R.string.scene_executed, scenes.getScene(position).sceneName),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onBackButton() {
        if (mListView.isEditMode()) {
            mListView.stopEditMode();
            scenesAdapter.setDisableEditing(false);
            return true;
        }
        return false;
    }

    @Override
    public void setIcon(Object context_object, Bitmap bitmap) {
        if (context_object == null)
            return;
        NetpowerctrlApplication.getDataController().sceneCollection.setBitmap(getActivity(),
                (Scene) context_object, bitmap);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        Icons.activityCheckForPickedImage(getActivity(), this, requestCode, resultCode, imageReturnedIntent);
    }

    @Override
    public void onMenuItemClicked(View v, int position) {
        mListView.setTag(position);
        Scene scene = scenes.getScene(position);

        @SuppressWarnings("ConstantConditions")
        PopupMenu popup = new PopupMenu(getActivity(), v);
        MenuInflater inflater = popup.getMenuInflater();
        Menu menu = popup.getMenu();
        inflater.inflate(R.menu.scenes_item, menu);

        boolean isFav = scene.isFavourite();
        //noinspection ConstantConditions
        menu.findItem(R.id.menu_set_favourite).setVisible(!isFav);
        //noinspection ConstantConditions
        menu.findItem(R.id.menu_remove_favourite).setVisible(isFav);

        popup.setOnMenuItemClickListener(this);
        popup.show();
    }
}