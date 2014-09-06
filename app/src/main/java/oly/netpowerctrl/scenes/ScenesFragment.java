package oly.netpowerctrl.scenes;

import android.app.AlertDialog;
import android.app.DialogFragment;
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
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.devices.DevicesFragment;
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.main.SortCriteriaDialog;
import oly.netpowerctrl.utils.AndroidShortcuts;
import oly.netpowerctrl.utils.AnimationController;
import oly.netpowerctrl.utils.controls.ActivityWithIconCache;
import oly.netpowerctrl.utils.controls.onListItemElementClicked;

/**
 */
public class ScenesFragment extends Fragment implements
        PopupMenu.OnMenuItemClickListener, AdapterView.OnItemClickListener, LoadStoreIconData.IconSelected, onListItemElementClicked {
    private SceneCollection scenes;
    private GridView mListView;
    private ScenesAdapter adapter;

    public ScenesFragment() {
    }

    private void setListOrGrid(boolean grid) {
        SharedPrefs.getInstance().setScenesList(grid);

        float width;

        if (!grid) {
            adapter.setLayoutRes(R.layout.list_item_icon);
            width = getResources().getDimension(R.dimen.min_list_item_width);
        } else {
            adapter.setLayoutRes(R.layout.grid_item_icon);
            width = getResources().getDimension(R.dimen.min_grid_item_width);
        }

        mListView.setColumnWidth((int) width);
        mListView.setNumColumns(GridView.AUTO_FIT);

        mListView.setAdapter(adapter);
    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.scenes, menu);

        //noinspection ConstantConditions
        menu.findItem(R.id.menu_add_scene).setVisible(AppData.getInstance().deviceCollection.hasDevices());

        if (adapter == null || adapter.getCount() == 0) {
            //noinspection ConstantConditions
            menu.findItem(R.id.menu_remove_scene).setVisible(false);
            //noinspection ConstantConditions
            menu.findItem(R.id.menu_view_list).setVisible(false);
            //noinspection ConstantConditions
            menu.findItem(R.id.menu_view_grid).setVisible(false);
            //noinspection ConstantConditions
            menu.findItem(R.id.menu_sort).setVisible(false);
            return;
        }

        menu.findItem(R.id.menu_sort).setVisible(true);

        boolean isList = adapter.getLayoutRes() == R.layout.list_item_icon;
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
                        .setMessage(R.string.confirmation_delete_all_scenes)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Delete all scenes
                                scenes.removeAll();
                                getActivity().invalidateOptionsMenu();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();
                return true;
            }

            case R.id.menu_add_scene: {
                Intent it = new Intent(getActivity(), EditSceneActivity.class);
                it.putExtra(EditSceneActivity.EDIT_SCENE_NOT_SHORTCUT, true);
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
                        .setIcon(android.R.drawable.ic_menu_help).show();
                return true;
            }

            case R.id.menu_sort: {
                DialogFragment fragment = SortCriteriaDialog.instantiate(getActivity(), scenes);
                MainActivity.getNavigationController().changeToDialog(getActivity(), fragment);
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
        scenes = AppData.getInstance().sceneCollection;
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scenes, container, false);
        assert view != null;
        mListView = (GridView) view.findViewById(android.R.id.list);
        mListView.setOnItemClickListener(this);
        adapter = new ScenesAdapter(getActivity(), scenes, ((ActivityWithIconCache) getActivity()).getIconCache());
        adapter.setListContextMenu(this);
        AnimationController animationController = new AnimationController(getActivity());
        animationController.setAdapter(adapter);
        animationController.setListView(mListView);
        adapter.setAnimationController(animationController);

        setListOrGrid(SharedPrefs.getInstance().getScenesList());
        if (!AppData.getInstance().deviceCollection.hasDevices()) {
            //noinspection ConstantConditions
            ((TextView) view.findViewById(R.id.empty_text)).setText(getString(R.string.empty_no_scenes_no_devices));
            Button btnEmpty = ((Button) view.findViewById(R.id.btnChangeToDevices));
            btnEmpty.setVisibility(View.VISIBLE);
            btnEmpty.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MainActivity.getNavigationController().changeToFragment(DevicesFragment.class.getName());
                }
            });
        } else {
            //noinspection ConstantConditions
            ((TextView) view.findViewById(R.id.empty_text)).setText(getString(R.string.empty_no_scenes));
            Button btnEmpty = ((Button) view.findViewById(R.id.btnAddScene));
            btnEmpty.setVisibility(View.VISIBLE);
            btnEmpty.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent it = new Intent(getActivity(), EditSceneActivity.class);
                    it.putExtra(EditSceneActivity.EDIT_SCENE_NOT_SHORTCUT, true);
                    startActivity(it);
                }
            });
        }

        mListView.setEmptyView(view.findViewById(android.R.id.empty));
        //onConfigurationChanged(getResources().getConfiguration());
        return view;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        final int position = (Integer) mListView.getTag();
        Scene scene = scenes.get(position);

        switch (menuItem.getItemId()) {
            case R.id.menu_edit_scene: {
                Intent it = new Intent(getActivity(), EditSceneActivity.class);
                it.putExtra(EditSceneActivity.EDIT_SCENE_NOT_SHORTCUT, true);
                it.putExtra(EditSceneActivity.LOAD_SCENE, scene.toString());
                startActivity(it);
                return true;
            }
            case R.id.menu_remove_scene: {
                scenes.removeScene(position);
                //noinspection ConstantConditions
                getActivity().invalidateOptionsMenu();
                return true;
            }
            case R.id.menu_remove_favourite: {
                scenes.setFavourite(scene, false);
                return true;
            }
            case R.id.menu_set_favourite: {
                scenes.setFavourite(scene, true);
                return true;
            }

            case R.id.menu_icon:
                LoadStoreIconData.show_select_icon_dialog(getActivity(), "scene_icons", this, scene);
                return true;

            case R.id.menu_add_homescreen: {
                //noinspection ConstantConditions
                AndroidShortcuts.createHomeIcon(getActivity().getApplicationContext(), scene);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        scenes.executeScene(position);
        adapter.handleClick(position, view);
        //noinspection ConstantConditions
        Toast.makeText(getActivity(),
                getActivity().getString(R.string.scene_executed, scenes.get(position).sceneName),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void setIcon(Object context_object, Bitmap bitmap) {
        if (context_object == null)
            return;
        AppData.getInstance().sceneCollection.setBitmap(getActivity(),
                (Scene) context_object, bitmap);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        LoadStoreIconData.activityCheckForPickedImage(getActivity(), this, requestCode, resultCode, imageReturnedIntent);
    }

    @Override
    public void onMenuItemClicked(View view, int position) {
        // Animate press
        Animation a = new ScaleAnimation(1.0f, 0.8f, 1.0f, 0.8f, view.getWidth() / 2, view.getHeight() / 2);
        a.setRepeatMode(Animation.REVERSE);
        a.setRepeatCount(1);
        a.setDuration(300);
        view.startAnimation(a);

        mListView.setTag(position);
        Scene scene = scenes.get(position);

        @SuppressWarnings("ConstantConditions")
        PopupMenu popup = new PopupMenu(getActivity(), view);
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