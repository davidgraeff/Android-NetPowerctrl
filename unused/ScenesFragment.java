package oly.netpowerctrl.scenes;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.BounceInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import oly.netpowerctrl.R;
;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.devices.DevicesFragment;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.utils.AnimationController;
import oly.netpowerctrl.utils.SortCriteriaDialog;
import oly.netpowerctrl.utils.controls.ActivityWithIconCache;
import oly.netpowerctrl.utils.controls.FloatingActionButton;
import oly.netpowerctrl.utils.controls.onListItemElementClicked;
import oly.netpowerctrl.ui.notifications.InAppNotifications;

/**
 */
public class ScenesFragment extends Fragment implements
        PopupMenu.OnMenuItemClickListener, AdapterView.OnItemClickListener, onListItemElementClicked {
    int requestedColumnWidth;
    private SceneCollection scenes;
    private GridView mListView;
    private ScenesAdapter adapter;

    public ScenesFragment() {
    }

    private void setViewType(int viewType) {
        SharedPrefs.getInstance().setScenesViewType(viewType);

        switch (viewType) {
            case 2:
                adapter.setLayoutRes(R.layout.grid_item_icon_center);
                requestedColumnWidth = (int) getResources().getDimension(R.dimen.min_grid_item_width);
                break;
            case 1:
                adapter.setLayoutRes(R.layout.grid_item_icon);
                requestedColumnWidth = (int) getResources().getDimension(R.dimen.min_grid_item_width);
                break;
            case 0:
            default:
                adapter.setLayoutRes(R.layout.list_item_icon);
                requestedColumnWidth = (int) getResources().getDimension(R.dimen.min_list_item_width);
                break;
        }

        mListView.setColumnWidth(requestedColumnWidth);
        mListView.setNumColumns(GridView.AUTO_FIT);
        adapter.setEnableEditing(SharedPrefs.getInstance().isOutletEditingEnabled());
        mListView.setAdapter(adapter);
    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.scenes, menu);

        //noinspection ConstantConditions
        menu.findItem(R.id.menu_add_scene).setVisible(PluginService.getInstance().deviceCollection.hasDevices());

        if (adapter == null || adapter.getCount() == 0) {
            //noinspection ConstantConditions
            menu.findItem(R.id.menu_remove_scene).setVisible(false);
            //noinspection ConstantConditions
            menu.findItem(R.id.menu_view_change).setVisible(false);
            //noinspection ConstantConditions
            menu.findItem(R.id.menu_sort).setVisible(false);
            return;
        }

        menu.findItem(R.id.menu_sort).setVisible(true);
        menu.findItem(R.id.menu_view_change).setVisible(true);
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

            case R.id.menu_view_change: {
                setViewType(SharedPrefs.getInstance().getNextScenesViewType());
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

        InAppNotifications.showPermanentNotifications(getActivity());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scenes = PluginService.getInstance().sceneCollection;
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

        setViewType(SharedPrefs.getInstance().getScenesViewType());

        FloatingActionButton btnAddScene = (FloatingActionButton) view.findViewById(R.id.btnAdd);
        btnAddScene.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent it = new Intent(getActivity(), EditSceneActivity.class);
                it.putExtra(EditSceneActivity.EDIT_SCENE_NOT_SHORTCUT, true);
                startActivity(it);
            }
        });

        if (!PluginService.getInstance().deviceCollection.hasDevices()) {
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
            btnAddScene.setVisibility(View.GONE);
        } else {
            AnimationController.animateFloatingButton(btnAddScene);

            //noinspection ConstantConditions
            ((TextView) view.findViewById(R.id.empty_text)).setText(getString(R.string.empty_no_scenes));
        }

        mListView.setEmptyView(view.findViewById(android.R.id.empty));
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
    public void onListItemElementClicked(View view, int position) {
        // Animate press
        Animation a = new ScaleAnimation(1.0f, 0.8f, 1.0f, 0.8f, view.getWidth() / 2, view.getHeight() / 2);
        a.setRepeatMode(Animation.REVERSE);
        a.setRepeatCount(1);
        a.setDuration(300);
        view.startAnimation(a);

        mListView.setTag(position);

        @SuppressWarnings("ConstantConditions")
        PopupMenu popup = new PopupMenu(getActivity(), view);
        MenuInflater inflater = popup.getMenuInflater();
        Menu menu = popup.getMenu();
        inflater.inflate(R.menu.scenes_item, menu);

        popup.setOnMenuItemClickListener(this);
        popup.show();
    }
}