package oly.netpowerctrl.utils_gui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import org.acra.ACRA;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.backup.drive.GDriveFragment;
import oly.netpowerctrl.backup.neighbours.NeighbourFragment;
import oly.netpowerctrl.devices.DevicesFragment;
import oly.netpowerctrl.main.FeedbackDialog;
import oly.netpowerctrl.main.OutletsFragment;
import oly.netpowerctrl.preferences.PreferencesFragment;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.scenes.ScenesFragment;
import oly.netpowerctrl.timer.TimerFragment;

/**
 * All navigation related functionality used by the main activity
 */
public class NavigationController {
    private final ArrayList<DrawerStateChanged> observers = new ArrayList<>();
    public int drawerLastItemPosition = -1;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerAdapter mDrawerAdapter;
    //private CharSequence mTitle;
    private boolean drawerControllableByMenuKey = false;
    private Fragment currentFragment;
    private String currentFragmentClass;
    private WeakReference<Activity> mDrawerActivity;
    private Bundle currentExtra;
    private List<BackStackEntry> backstack = new ArrayList<>();

    public Fragment getCurrentFragment() {
        return currentFragment;
    }

    public boolean isLoading() {
        return (mDrawerList == null);
    }

    public void setTitle(CharSequence mTitle) {
        //this.mTitle = mTitle;
        Activity context = mDrawerActivity.get();
        if (context == null) // should never happen
            return;
        //noinspection ConstantConditions
        context.getActionBar().setTitle(mTitle);
    }

    public void detachCurrentFragment() {
        Activity context = mDrawerActivity.get();
        if (context == null || currentFragment == null) // should never happen
            return;
        context.getFragmentManager().beginTransaction().detach(currentFragment).commitAllowingStateLoss();
        context.getFragmentManager().executePendingTransactions();
    }

    @SuppressWarnings("unused")
    public void registerOpenStateChanged(DrawerStateChanged o) {
        if (!observers.contains(o)) {
            observers.add(o);
        }
    }

    @SuppressWarnings("unused")
    public void unregisterOpenStateChanged(DrawerStateChanged o) {
        observers.remove(o);
    }

    private void notifyOpenState(boolean state) {
        for (DrawerStateChanged anObserversStartStopRefresh : observers) {
            anObserversStartStopRefresh.drawerState(state);
        }
    }

    public void createDrawer(final Activity context, RestorePositionEnum restore) {
        mDrawerActivity = new WeakReference<>(context);
        // References for the drawer
        mDrawerLayout = (DrawerLayout) context.findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) context.findViewById(R.id.left_drawer_list);

        // set a custom shadow that overlays the main content when the drawer opens
        if (mDrawerLayout != null)
            mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener

        mDrawerAdapter = new DrawerAdapter(context);
        mDrawerAdapter.addHeader(context.getString(R.string.drawer_switch_title));
        mDrawerAdapter.addItem(context.getString(R.string.drawer_overview), "",
                OutletsFragment.class.getName(), false);

        mDrawerAdapter.addItem(context.getString(R.string.drawer_scenes), "",
                ScenesFragment.class.getName(), false);
        mDrawerAdapter.usePositionForScenes();

        mDrawerAdapter.addItem(context.getString(R.string.drawer_timer), "",
                TimerFragment.class.getName(), false);

        mDrawerAdapter.add(context.getResources().getStringArray(R.array.drawer_titles_app),
                context.getResources().getStringArray(R.array.drawer_descriptions_app),
                new String[]{"", DevicesFragment.class.getName(),
                        PreferencesFragment.class.getName(), GDriveFragment.class.getName(),
                        NeighbourFragment.class.getName()}
        );

        String version = context.getString(R.string.Version) + " ";
        try {
            //noinspection ConstantConditions
            version += context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        mDrawerAdapter.addItem(context.getString(R.string.drawer_feedback), version,
                FeedbackDialog.class.getName(), true);

        mDrawerAdapter.setAccompanyFragment(OutletsFragment.class.getName(),
                ScenesFragment.class.getName());

        mDrawerList.setAdapter(mDrawerAdapter);
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());


        if (mDrawerLayout != null) {
            // ActionBarDrawerToggle ties together the the proper interactions
            // between the sliding drawer and the action bar app icon
            mDrawerToggle = new ActionBarDrawerToggle(
                    context,                  /* host Activity */
                    mDrawerLayout,         /* DrawerLayout object */
                    R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
                    R.string.drawer_open,  /* "open drawer" description for accessibility */
                    R.string.drawer_close  /* "close drawer" description for accessibility */
            ) {
                public void onDrawerClosed(View view) {
                    context.invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                }

                public void onDrawerOpened(View drawerView) {
                    //getActionBar().setTitle(mDrawerTitle);
                    context.invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                    if (currentFragment != null && currentFragment instanceof OnBackButton)
                        ((OnBackButton) currentFragment).onBackButton();
                }
            };
            mDrawerLayout.setDrawerListener(mDrawerToggle);
        }

        if (restore == RestorePositionEnum.RestoreLastSaved) {
            // Restore the last visited screen
            String className = SharedPrefs.getFirstTab();
            if (className == null || className.isEmpty()) {
                className = OutletsFragment.class.getName();
            }
            backstack.add(new BackStackEntry(className, null));
            changeToFragment(className);
        } else if (restore == RestorePositionEnum.RestoreAfterConfigurationChanged && currentFragmentClass != null) {
            context.getFragmentManager().beginTransaction().attach(currentFragment).commitAllowingStateLoss();
        }


        if (mDrawerLayout != null) {
            // Sync the toggle state after onRestoreInstanceState has occurred.
            mDrawerToggle.syncState();
        }
    }

    public void onPrepareOptionsMenu(Menu menu) {
        if (mDrawerLayout == null)
            return;

        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        drawerControllableByMenuKey = menu.size() <= 2;
        if (drawerOpen)
            for (int i = 0; i < menu.size(); i++) {
                menu.getItem(i).setVisible(false);
            }

        notifyOpenState(drawerOpen);
    }

    public void menuKeyPressed() {
        if (isLoading() || mDrawerLayout == null)
            return;

        if (drawerControllableByMenuKey) {
            if (mDrawerLayout.isDrawerOpen(mDrawerList))
                mDrawerLayout.closeDrawer(mDrawerList);
            else
                mDrawerLayout.openDrawer(mDrawerList);
        }
    }

    public void saveSelection() {
        if (isLoading())
            return;

        final int currentPosition = mDrawerList.getCheckedItemPosition();
        if (currentPosition != -1 && currentPosition < mDrawerAdapter.getCount() &&
                mDrawerAdapter.getItemViewType(currentPosition) != 0) {
            DrawerAdapter.DrawerItem item = (DrawerAdapter.DrawerItem) mDrawerAdapter.getItem(currentPosition);
            if (!item.fragmentClassName.isEmpty() && !item.fragmentClassName.contains("Dialog"))
                SharedPrefs.setFirstTab(item.fragmentClassName);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerLayout != null && mDrawerToggle.onOptionsItemSelected(item);

    }

    public boolean onBackPressed() {
        if (currentFragment != null && currentFragment instanceof OnBackButton)
            if (((OnBackButton) currentFragment).onBackButton())
                return true;

        if (backstack.size() > 0) {
            BackStackEntry entry = backstack.get(backstack.size() - 1);
            backstack.remove(backstack.size() - 1);
            changeToFragment(entry.fragmentClass, entry.extra, false);
            return true;
        }
        return false;
    }

    public void changeToFragment(String fragmentClassName) {
        changeToFragment(fragmentClassName, null, true);
    }

    public void changeToFragment(String fragmentClassName, Bundle extra, boolean addToBackstack) {
        Activity context = mDrawerActivity.get();
        if (context == null || fragmentClassName == null) // should never happen
            return;

        if (addToBackstack && currentFragmentClass != null) {
            int index = backstack.indexOf(fragmentClassName);
            if (index != -1)
                backstack.remove(index);
            backstack.add(new BackStackEntry(currentFragmentClass, currentExtra));
            if (backstack.size() > 3)
                backstack.remove(0);
        }

        boolean fragmentClassNameEquals = fragmentClassName.equals(currentFragmentClass);

        int pos = mDrawerAdapter.indexOf(fragmentClassName);
        if (pos != -1) {
            DrawerAdapter.DrawerItem item = (DrawerAdapter.DrawerItem) mDrawerAdapter.getItem(pos);
            // update selected item and title
            mDrawerList.setItemChecked(pos, true);
            setTitle(item.mTitle);
            drawerLastItemPosition = pos;
        }

        if (currentFragment == null || !fragmentClassNameEquals) {
            currentFragment = Fragment.instantiate(context, fragmentClassName);
            currentFragmentClass = fragmentClassName;
        }
        if (!fragmentClassNameEquals) {
            try {
                FragmentTransaction ft = context.getFragmentManager().beginTransaction();
                ft.replace(R.id.content_frame, currentFragment);
                // we commit with possible state loss here because applyFragmentTransaction is called from an
                // async callback and the InstanceState of the activity may have been saved already.
                ft.commit();
            } catch (Exception exception) {
                ACRA.getErrorReporter().handleException(exception);
            }
        }

        changeArgumentsOfCurrentFragment(extra);
    }

    public void changeArgumentsOfCurrentFragment(Bundle extra) {
        // Deliver arguments to fragment via ChangeArgumentsFragment interface
        if (currentFragment instanceof ChangeArgumentsFragment) {
            currentExtra = extra;
            ((ChangeArgumentsFragment) currentFragment).changeArguments(extra);
        }
    }

    public void changeToDialog(Activity context, String fragmentClassName) {
        ShowToast.showDialogFragment(context, Fragment.instantiate(context, fragmentClassName));
        // Reset focus to last item
        mDrawerList.post(new Runnable() {
            @Override
            public void run() {
                mDrawerList.setItemChecked(drawerLastItemPosition, true);
            }
        });
    }

    public enum RestorePositionEnum {
        NoRestore,
        RestoreLastSaved,
        RestoreAfterConfigurationChanged
    }


    public interface DrawerStateChanged {
        void drawerState(boolean open);
    }

    private static class BackStackEntry {
        String fragmentClass;
        Bundle extra;

        public BackStackEntry(String fragmentClassName, Bundle extra) {
            this.fragmentClass = fragmentClassName;
            this.extra = extra;
        }

        @Override
        public boolean equals(Object object) {
            if (object != null && object instanceof BackStackEntry) {
                BackStackEntry thing = (BackStackEntry) object;
                return fragmentClass.equals(thing.fragmentClass);
            } else if (object instanceof String) {
                return fragmentClass.equals(object);
            }

            return false;
        }
    }

    /* The click listener for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (mDrawerAdapter.getItemViewType(position) == 0)
                return;

            Activity context = mDrawerActivity.get();
            if (context == null) // should never happen
                return;

            DrawerAdapter.DrawerItem item = mDrawerAdapter.get(position);

            // close the drawer
            if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mDrawerList)) {
                Handler mHandler = new Handler();
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mDrawerLayout.closeDrawer(mDrawerList);
                    }
                }, 150);
            }

            // First look at a click handler for that specific item
            if (item.clickHandler != null) {
                item.clickHandler.onClick(null);
                if (drawerLastItemPosition == -1)
                    return;
                // Reset focus to last item
                mDrawerList.post(new Runnable() {
                    @Override
                    public void run() {
                        mDrawerList.setItemChecked(drawerLastItemPosition, true);
                    }
                });
                return;
            }

            // No click handler: Should be a fragment change request
            if (item.fragmentClassName == null || item.fragmentClassName.isEmpty())
                return;

            if (item.mDialog) {
                changeToDialog(context, item.fragmentClassName);
                return;
            }

            changeToFragment(item.fragmentClassName, item.mExtra, true);
        }
    }

}