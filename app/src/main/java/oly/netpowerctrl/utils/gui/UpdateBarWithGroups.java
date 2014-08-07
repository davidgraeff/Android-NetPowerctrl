package oly.netpowerctrl.utils.gui;

import android.app.ActionBar;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.groups.GroupCollection;

/**
 * Created by david on 04.08.14.
 */
public class UpdateBarWithGroups implements GroupCollection.IGroupsUpdated, ActionBar.OnNavigationListener, NavigationController.DrawerStateChanged {
    private ActionBar actionBar;
    private NavigationController navigationController;
    private int lastNavigationMode = -1;
    private int lastDisplayOptions = -1;
    private ArrayAdapter<String> adapter;
    private boolean updateLock = false;

    public void initNavigation(ActionBar actionBar, NavigationController navigationController) {
        this.actionBar = actionBar;
        this.navigationController = navigationController;
        adapter = new ArrayAdapter<>(actionBar.getThemedContext(), android.R.layout.simple_spinner_dropdown_item);
        actionBar.setListNavigationCallbacks(adapter, this);
        navigationController.registerOpenStateChanged(this);
        NetpowerctrlApplication.getDataController().groupCollection.registerObserver(this);
    }

    public void showNavigation() {
        groupsUpdated(true);
    }

    private void hideNavigation() {
        if (lastNavigationMode == -1)
            return;

        //noinspection ResourceType
        actionBar.setNavigationMode(lastNavigationMode);
        actionBar.setDisplayOptions(lastDisplayOptions);
        lastNavigationMode = -1;
        lastDisplayOptions = -1;
    }

    public void finishNavigation() {
        hideNavigation();
        NetpowerctrlApplication.getDataController().groupCollection.unregisterObserver(this);
        if (navigationController != null)
            navigationController.unregisterOpenStateChanged(this);
        actionBar = null;
        navigationController = null;
        adapter = null;
    }


    @Override
    public void groupsUpdated(boolean addedOrRemoved) {
        if (actionBar == null) {
            NetpowerctrlApplication.getDataController().groupCollection.unregisterObserver(this);
            return;
        }

        GroupCollection g = NetpowerctrlApplication.getDataController().groupCollection;
        if (g.length() == 0) {
            hideNavigation();
            return;
        }

        if (lastNavigationMode == -1) {
            lastNavigationMode = actionBar.getNavigationMode();
            lastDisplayOptions = actionBar.getDisplayOptions();
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        }

        updateLock = true;
        adapter.clear();
        adapter.add(actionBar.getThemedContext().getString(R.string.drawer_overview));
        adapter.addAll(g.getGroupsArray());
        updateLock = false;
    }

    @Override
    public boolean onNavigationItemSelected(int i, long l) {
        if (updateLock)
            return false;

        if (i == 0) {
            navigationController.changeArgumentsOfCurrentFragment(null);
            return false;
        }
        --i; // the first item is added manually and not part of groupCollection

        GroupCollection g = NetpowerctrlApplication.getDataController().groupCollection;
        GroupCollection.GroupItem groupItem = g.groups.get(i);

        Bundle extra = new Bundle();
        extra.putString("filter", groupItem.uuid.toString());

        navigationController.changeArgumentsOfCurrentFragment(extra);
        return false;
    }

    @Override
    public void drawerState(boolean open) {
        if (open) {
            hideNavigation();
        } else {
            groupsUpdated(true);
        }
    }
}
