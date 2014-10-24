package oly.netpowerctrl.utils.actionbar;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.widget.ArrayAdapter;

import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.groups.Group;
import oly.netpowerctrl.groups.GroupCollection;
import oly.netpowerctrl.groups.GroupUtilities;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.utils.navigation.NavigationController;

/**
 * Add a comboBox with all groups to the action bar. The list of groups is updated automatically
 * and hides if the navigation drawer opens.
 */
public class ActionBarWithGroups implements onCollectionUpdated<GroupCollection, Group>,
        ActionBar.OnNavigationListener, NavigationController.DrawerStateChanged {
    private ActionBar actionBar = null;
    private NavigationController navigationController;
    private int lastNavigationMode = -1;
    private int lastDisplayOptions = -1;
    private ArrayAdapter<String> adapter;
    private boolean updateLock = false;
    private boolean synthetic = true;
    private UUID groupID;
    private int lastSelectedGroup = 0;

    public void initNavigation(ActionBar actionBar, NavigationController navigationController) {
        this.actionBar = actionBar;
        if (actionBar == null) {
            synthetic = false;
            adapter = new ArrayAdapter<>(App.instance, android.R.layout.simple_spinner_dropdown_item);
        } else {
            adapter = new ArrayAdapter<>(actionBar.getThemedContext(), android.R.layout.simple_spinner_dropdown_item);
            actionBar.setListNavigationCallbacks(adapter, this);
        }

        this.navigationController = navigationController;
        navigationController.registerOpenStateChanged(this);
        AppData.getInstance().groupCollection.registerObserver(this);
    }

    public void showNavigation() {
        updated(AppData.getInstance().groupCollection, null, null, -1);
    }

    private void hideNavigation() {
        if (actionBar == null || lastNavigationMode == -1)
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

        lastSelectedGroup = i;

        if (i == 0) {
            navigationController.changeArgumentsOfCurrentFragment(null);
            return true;
        }
        --i; // the first item is added manually and not part of groupCollection

        GroupCollection g = AppData.getInstance().groupCollection;

        if (i == g.size()) { // the last item is added manually
            GroupUtilities.createGroupForDevicePort(actionBar.getThemedContext(), null);
            return true;
        }

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
            updated(AppData.getInstance().groupCollection, null, null, -1);
        }
    }

    @Override
    public boolean updated(GroupCollection groupCollection, Group group, ObserverUpdateActions action, int position) {
        if (groupCollection.length() == 0) {
            hideNavigation();
            return true;
        }

        if (lastNavigationMode == -1 && actionBar != null) {
            lastNavigationMode = actionBar.getNavigationMode();
            lastDisplayOptions = actionBar.getDisplayOptions();
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        }

        updateLock = true;
        adapter.clear();
        adapter.add(App.getAppString(R.string.drawer_overview));
        adapter.addAll(groupCollection.getGroupsArray());
        if (actionBar != null) {
            if (groupID == null)
                actionBar.setSelectedNavigationItem(0);
            else
                actionBar.setSelectedNavigationItem(AppData.getInstance().groupCollection.indexOf(groupID) + 1);
        }
        adapter.add(App.getAppString(R.string.createGroup));
        updateLock = false;

        return true;
    }

    public void setCurrentIndex(UUID groupID) {
        this.groupID = groupID;

        if (actionBar == null) {
            return;
        }

        updateLock = true;
        try {
            if (groupID == null)
                actionBar.setSelectedNavigationItem(0);
            else
                actionBar.setSelectedNavigationItem(AppData.getInstance().groupCollection.indexOf(groupID) + 1);
        } catch (java.lang.IllegalStateException ignored) {
            // setSelectedNavigationIndex not valid for current navigation mode
        }
        updateLock = false;
    }

    public void previous() {
        onNavigationItemSelected(lastSelectedGroup - 1 < 0 ? adapter.getCount() - 1 : lastSelectedGroup - 1, 0);
    }

    public void next() {
        onNavigationItemSelected(lastSelectedGroup + 1 >= adapter.getCount() ? 0 : lastSelectedGroup + 1, 0);
    }
}
