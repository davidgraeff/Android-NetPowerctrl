package oly.netpowerctrl.utils.navigation;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

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
import oly.netpowerctrl.timer.TimerFragment;
import oly.netpowerctrl.utils.DonateData;
import oly.netpowerctrl.utils.RecyclerItemClickListener;
import oly.netpowerctrl.utils.fragments.onFragmentBackButton;
import oly.netpowerctrl.utils.fragments.onFragmentChangeArguments;
import oly.netpowerctrl.utils.notifications.InAppNotifications;

/**
 * All navigation related functionality used by the main activity
 */
public class NavigationController implements RecyclerItemClickListener.OnItemClickListener {
    private final ArrayList<DrawerStateChanged> observers = new ArrayList<>();
    private final List<BackStackEntry> backstack = new ArrayList<>();
    private DrawerLayout mDrawerLayout;
    private RecyclerView mRecyclerView;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerAdapter mDrawerAdapter;
    //private CharSequence mTitle;
    private boolean drawerControllableByMenuKey = false;
    private WeakReference<ActionBarActivity> mDrawerActivity;

    // Current
    private int drawerLastItemPosition = -1;
    private Fragment currentFragment;
    private String currentFragmentClass;
    private Bundle currentExtra;

    public Fragment getCurrentFragment() {
        return currentFragment;
    }

    public boolean isLoading() {
        return (mRecyclerView == null);
    }

    public void setTitle(CharSequence mTitle) {
        //this.mTitle = mTitle;
        ActionBarActivity context = mDrawerActivity.get();
        if (context == null) // should never happen
            return;
        ActionBar actionBar = context.getSupportActionBar();
        if (actionBar == null) // should never happen
            return;
        actionBar.setTitle(mTitle);
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

    public void createDrawer(final ActionBarActivity context, RestorePositionEnum restore) {
        // Do restore only once
        if (restore == RestorePositionEnum.RestoreLastSaved && mDrawerLayout != null)
            return;

        mDrawerActivity = new WeakReference<>(context);
        // References for the drawer
        mDrawerLayout = (DrawerLayout) context.findViewById(R.id.drawer_layout);
        mRecyclerView = (RecyclerView) context.findViewById(R.id.left_drawer_list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        // set a custom shadow that overlays the main content when the drawer opens
        if (mDrawerLayout != null)
            mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener

        mDrawerAdapter = new DrawerAdapter();
        mDrawerAdapter.addHeader(context.getString(R.string.drawer_switch_title));
        mDrawerAdapter.addItem(context.getString(R.string.drawer_overview_list), "",
                OutletsFragment.class.getName(), false).mExtra = OutletsFragment.createBundleForView(OutletsFragment.VIEW_AS_LIST);
        mDrawerAdapter.addItem(context.getString(R.string.drawer_overview_grid), "",
                OutletsFragment.class.getName(), false).mExtra = OutletsFragment.createBundleForView(OutletsFragment.VIEW_AS_GRID);
        mDrawerAdapter.addItem(context.getString(R.string.drawer_overview_compact), "",
                OutletsFragment.class.getName(), false).mExtra = OutletsFragment.createBundleForView(OutletsFragment.VIEW_AS_COMPACT);

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

        mRecyclerView.setAdapter(mDrawerAdapter);
        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(mRecyclerView.getContext(), this, null));


        if (mDrawerLayout != null) {
            createDrawerToggle(context);
        }

        if (restore == RestorePositionEnum.RestoreLastSaved) {
            // Restore the last visited screen
            Bundle extra = SharedPrefs.getInstance().getFirstTabExtra();
            String className = SharedPrefs.getInstance().getFirstTab();
            int pos = SharedPrefs.getInstance().getFirstTabPosition();

            if (className == null || className.isEmpty() || pos == -1) {
                className = OutletsFragment.class.getName();
                pos = mDrawerAdapter.indexOf(className);
                extra = null;
            }
            //backstack.add(new BackStackEntry(className, null));
            changeToFragment(className, extra, false, pos);
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
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mRecyclerView);
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
            if (mDrawerLayout.isDrawerOpen(mRecyclerView))
                mDrawerLayout.closeDrawer(mRecyclerView);
            else
                mDrawerLayout.openDrawer(mRecyclerView);
        }
    }

    public void saveSelection() {
        if (isLoading())
            return;

        if (drawerLastItemPosition != -1 && drawerLastItemPosition < mDrawerAdapter.getItemCount() &&
                mDrawerAdapter.getItemViewType(drawerLastItemPosition) != 0) {
            DrawerAdapter.DrawerItem item = mDrawerAdapter.getItem(drawerLastItemPosition);
            if (!item.fragmentClassName.isEmpty() && !item.fragmentClassName.contains("Dialog")) {
                SharedPrefs.getInstance().setFirstTab(item.fragmentClassName, currentExtra, drawerLastItemPosition);
            }
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
            changeToFragment(entry.fragmentClass, entry.extra, false, entry.position);
            return true;
        }
        return false;
    }

    public void changeToFragment(String fragmentClassName) {
        changeToFragment(fragmentClassName, null, true, mDrawerAdapter.indexOf(fragmentClassName));
    }

    public void changeToFragment(String fragmentClassName, final Bundle extra) {
        changeToFragment(fragmentClassName, extra, true, mDrawerAdapter.indexOf(fragmentClassName));
    }

    private void changeToFragment(String fragmentClassName, final Bundle extra, boolean addToBackstack, int position) {
        final Activity context = mDrawerActivity.get();
        if (context == null || fragmentClassName == null) // should never happen
            return;

        if (addToBackstack && currentFragmentClass != null) {
            int index = backstack.indexOf(fragmentClassName);
            if (index != -1)
                backstack.remove(index);
            backstack.add(new BackStackEntry(currentFragmentClass, currentExtra, position));
            if (backstack.size() > 3)
                backstack.remove(0);
        }

        boolean fragmentClassNameEquals = fragmentClassName.equals(currentFragmentClass);

        if (position != -1 && position != drawerLastItemPosition) {
            drawerLastItemPosition = position;
            // update selected item and title
            mDrawerAdapter.setSelectedItem(drawerLastItemPosition);
            setTitle(mDrawerAdapter.getItem(drawerLastItemPosition).mTitle);
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
    }

    @Override
    public void onItemClick(View view, int position) {
        if (mDrawerAdapter.getItemViewType(position) == 0)
            return;

        Activity context = mDrawerActivity.get();
        if (context == null) // should never happen
            return;

        DrawerAdapter.DrawerItem item = mDrawerAdapter.get(position);

        // close the drawer
        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mRecyclerView)) {
            Handler mHandler = new Handler();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mDrawerLayout.closeDrawer(mRecyclerView);
                }
            }, 150);
        }

        // First look at a click handler for that specific item
        if (item.clickHandler != null) {
            item.clickHandler.onClick(null);
            return;
        }

        // No click handler: Should be a fragment change request
        if (item.fragmentClassName == null || item.fragmentClassName.isEmpty())
            return;

        if (item.mDialog) {
            changeToDialog(context, item.fragmentClassName);
            return;
        }

        changeToFragment(item.fragmentClassName, item.mExtra, true, position);
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
        public int position;

        public BackStackEntry(String fragmentClassName, Bundle extra, int position) {
            this.fragmentClass = fragmentClassName;
            this.extra = extra;
            this.position = position;
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
}
