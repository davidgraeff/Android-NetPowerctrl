package oly.netpowerctrl.utils.actionbar;

import android.app.ActionBar;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.groups.Group;
import oly.netpowerctrl.groups.GroupCollection;
import oly.netpowerctrl.utils.navigation.NavigationController;

/**
 * Add a comboBox with all groups to the action bar. The list of groups is updated automatically
 * and hides if the navigation drawer opens.
 */
public class ActionBarWithGroups implements onCollectionUpdated<GroupCollection, Group>,
        ActionBar.OnNavigationListener, NavigationController.DrawerStateChanged {
    private ActionBar actionBar;
    private NavigationController navigationController;
    private int lastNavigationMode = -1;
    private int lastDisplayOptions = -1;
    private ArrayAdapter<String> adapter;
    private boolean updateLock = false;
    private boolean synthetic = true;
    private UUID groupID;

    public void initNavigation(ActionBar actionBar, NavigationController navigationController) {
        this.actionBar = actionBar;
        this.navigationController = navigationController;
        adapter = new ArrayAdapter<>(actionBar.getThemedContext(), android.R.layout.simple_spinner_dropdown_item);
        actionBar.setListNavigationCallbacks(adapter, this);
        navigationController.registerOpenStateChanged(this);
        AppData.getInstance().groupCollection.registerObserver(this);
    }

    public void showNavigation() {
        updated(AppData.getInstance().groupCollection, null, null);
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
        AppData.getInstance().groupCollection.unregisterObserver(this);
        if (navigationController != null)
            navigationController.unregisterOpenStateChanged(this);
        actionBar = null;
        navigationController = null;
        adapter = null;
    }

    @Override
    public boolean onNavigationItemSelected(int i, long l) {
        if (synthetic) {
            synthetic = false;
            return true;
        }

        if (updateLock)
            return true;

        if (i == 0) {
            navigationController.changeArgumentsOfCurrentFragment(null);
            return true;
        }
        --i; // the first item is added manually and not part of groupCollection

        GroupCollection g = AppData.getInstance().groupCollection;
        Group group = g.get(i);

        Bundle extra = new Bundle();
        extra.putString("filter", group.uuid.toString());

        navigationController.changeArgumentsOfCurrentFragment(extra);
        return true;
    }

    @Override
    public void drawerState(boolean open) {
        if (open) {
            hideNavigation();
        } else {
            updated(AppData.getInstance().groupCollection, null, null);
        }
    }

    @Override
    public boolean updated(GroupCollection groupCollection, Group group, ObserverUpdateActions action) {
        if (actionBar == null) {
            return false;
        }

        if (groupCollection.length() == 0) {
            hideNavigation();
            return true;
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
        adapter.addAll(groupCollection.getGroupsArray());
        if (groupID == null)
            actionBar.setSelectedNavigationItem(0);
        else
            actionBar.setSelectedNavigationItem(AppData.getInstance().groupCollection.indexOf(groupID) + 1);
        updateLock = false;

        return true;
    }

    public void setCurrentIndex(UUID groupID) {
        this.groupID = groupID;

        if (actionBar == null) {
            return;
        }

        updateLock = true;
        if (groupID == null)
            actionBar.setSelectedNavigationItem(0);
        else
            actionBar.setSelectedNavigationItem(AppData.getInstance().groupCollection.indexOf(groupID) + 1);
        updateLock = false;
    }
}
