package oly.netpowerctrl.utils.navigation;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
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

import org.sufficientlysecure.donations.DonationsFragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.BuildConfig;
import oly.netpowerctrl.R;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.devices.DevicesFragment;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.main.FeedbackDialog;
import oly.netpowerctrl.outletsview.OutletsFragment;
import oly.netpowerctrl.preferences.PreferencesFragment;
import oly.netpowerctrl.scenes.ScenesFragment;
import oly.netpowerctrl.timer.TimerFragment;
import oly.netpowerctrl.utils.DonateData;
import oly.netpowerctrl.utils.fragments.onFragmentBackButton;
import oly.netpowerctrl.utils.fragments.onFragmentChangeArguments;
import oly.netpowerctrl.utils.notifications.InAppNotifications;

/**
 * All navigation related functionality used by the main activity
 */
public class NavigationController {
    private final ArrayList<DrawerStateChanged> observers = new ArrayList<>();
    private final List<BackStackEntry> backstack = new ArrayList<>();
    private int drawerLastItemPosition = -1;
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
        // Do restore only once
        if (restore == RestorePositionEnum.RestoreLastSaved && mDrawerLayout != null)
            return;

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

        mDrawerAdapter.addHeader(context.getString(R.string.drawer_app_title));

        mDrawerAdapter.addItem(context.getString(R.string.drawer_devices), context.getString(R.string.drawer_devices_detail),
                DevicesFragment.class.getName(), false);

        mDrawerAdapter.addItem(context.getString(R.string.drawer_preferences), context.getString(R.string.drawer_preferences_detail),
                PreferencesFragment.class.getName(), false);

        mDrawerAdapter.addItem(context.getString(R.string.drawer_donate), context.getString(R.string.drawer_donate_detail),
                DonationsFragment.class.getName(), false).mExtra = DonationsFragment.createExtra(
                BuildConfig.DEBUG, true, DonateData.GOOGLE_PUBKEY, DonateData.GOOGLE_CATALOG,
                context.getResources().getStringArray(R.array.donation_google_catalog_values), true,
                DonateData.PP_USER, DonateData.PP_CURRENCY_CODE, DonateData.PP_DECODED_URI,
                true, DonateData.FLATTR_PROJECT_URL, DonateData.FLATTR_URL, false, null);


        String version = context.getString(R.string.Version) + " ";
        try {
            //noinspection ConstantConditions
            version += context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        mDrawerAdapter.addItem(context.getString(R.string.drawer_feedback), version,
                FeedbackDialog.class.getName(), true);

        mDrawerList.setAdapter(mDrawerAdapter);
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());


        if (mDrawerLayout != null) {
            createDrawerToggle(context);
        }

        if (restore == RestorePositionEnum.RestoreLastSaved) {
            // Restore the last visited screen
            Bundle extra = SharedPrefs.getInstance().getFirstTabExtra();
            String className = SharedPrefs.getInstance().getFirstTab();
            int pos = mDrawerAdapter.indexOf(className);

            if (className == null || className.isEmpty() || pos == -1) {
                className = OutletsFragment.class.getName();
                extra = null;
            }
            //backstack.add(new BackStackEntry(className, null));
            changeToFragment(className, extra, false);
        } else if (restore == RestorePositionEnum.RestoreAfterConfigurationChanged && currentFragmentClass != null) {
            context.getFragmentManager().beginTransaction().attach(currentFragment).commitAllowingStateLoss();
        }


        if (mDrawerLayout != null) {
            // Sync the toggle state after onRestoreInstanceState has occurred.
            mDrawerToggle.syncState();
        }
    }

    public void createDrawerToggle(final Activity context) {
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
                if (currentFragment != null && currentFragment instanceof onFragmentBackButton)
                    ((onFragmentBackButton) currentFragment).onBackButton();
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
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
                SharedPrefs.getInstance().setFirstTab(item.fragmentClassName, currentExtra);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerLayout != null && mDrawerToggle.onOptionsItemSelected(item);

    }

    public boolean onBackPressed() {
        if (currentFragment != null && currentFragment instanceof onFragmentBackButton)
            if (((onFragmentBackButton) currentFragment).onBackButton())
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

    public void changeToFragment(String fragmentClassName, final Bundle extra, boolean addToBackstack) {
        final Activity context = mDrawerActivity.get();
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

        if (!fragmentClassNameEquals) {
            try {
                currentFragment = Fragment.instantiate(context, fragmentClassName);
                currentFragmentClass = fragmentClassName;
                changeArgumentsOfCurrentFragment(extra);
                FragmentTransaction ft = context.getFragmentManager().beginTransaction();
                ft.replace(R.id.content_frame, currentFragment);
                try {
                    ft.commit();
                } catch (IllegalStateException exception) {
                    ft.commitAllowingStateLoss();
                }
            } catch (Exception exception) {
                if (!App.isDebug()) {
                    InAppNotifications.showException(context, exception, "fragmentName: " + currentFragmentClass);
                }
                exception.printStackTrace();

                App.getMainThreadHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        FragmentTransaction ft = context.getFragmentManager().beginTransaction();
                        ft.replace(R.id.content_frame, currentFragment);
                        try {
                            ft.commit();
                        } catch (IllegalStateException illegalException) {
                            ft.commitAllowingStateLoss();
                        }
                    }
                });
            }
        } else {
            changeArgumentsOfCurrentFragment(extra);
        }
    }

    public void changeArgumentsOfCurrentFragment(Bundle extra) {
        currentExtra = extra;
        // Deliver arguments to fragment via ChangeArgumentsFragment interface
        if (currentFragment instanceof onFragmentChangeArguments) {
            ((onFragmentChangeArguments) currentFragment).changeArguments(extra);
        } else if (extra != null && !currentFragment.isResumed()) // Old school setArguments call
            currentFragment.setArguments(extra);
    }

    public void changeToDialog(Activity context, String fragmentClassName) {
        FragmentManager fragmentManager = context.getFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        Fragment prev = fragmentManager.findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        ((DialogFragment) Fragment.instantiate(context, fragmentClassName)).show(ft, "dialog");

        // Reset focus to last item
        mDrawerList.post(new Runnable() {
            @Override
            public void run() {
                mDrawerList.setItemChecked(drawerLastItemPosition, true);
            }
        });
    }

    public void changeToDialog(Activity context, DialogFragment fragment) {
        FragmentManager fragmentManager = context.getFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        Fragment prev = fragmentManager.findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        fragment.show(ft, "dialog");

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
        final String fragmentClass;
        final Bundle extra;

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
