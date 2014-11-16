package oly.netpowerctrl.ui.navigation;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.graphics.BitmapFactory;
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
import android.util.TypedValue;
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
import oly.netpowerctrl.main.FeedbackFragment;
import oly.netpowerctrl.outletsview.OutletsViewFragment;
import oly.netpowerctrl.outletsview.OutletsViewTypeDialog;
import oly.netpowerctrl.preferences.PreferencesFragment;
import oly.netpowerctrl.timer.TimerFragment;
import oly.netpowerctrl.ui.RecyclerItemClickListener;
import oly.netpowerctrl.utils.DonateData;
import oly.netpowerctrl.utils.fragments.onFragmentBackButton;
import oly.netpowerctrl.utils.fragments.onFragmentChangeArguments;

/**
 * All navigation related functionality used by the main activity
 */
public class NavigationController implements RecyclerItemClickListener.OnItemClickListener {
    private final ArrayList<DrawerStateChanged> observers = new ArrayList<>();
    private final List<BackStackEntry> backstack = new ArrayList<>();
    OnNavigationBackButtonPressed onNavigationBackButtonPressed;
    private DrawerLayout mDrawerLayout;
    private RecyclerView mRecyclerView;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerAdapter mDrawerAdapter;
    private WeakReference<ActionBarActivity> mDrawerActivity;
    // Current
    private int drawerLastItemPosition = -1;
    private Fragment currentFragment;
    private String currentFragmentClass;
    private Bundle currentExtra;

    public DrawerLayout getDrawerLayout() {
        return mDrawerLayout;
    }

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
        mRecyclerView.setHasFixedSize(false);

        // set a custom shadow that overlays the main content when the drawer opens
        if (mDrawerLayout != null)
            mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener

        mDrawerAdapter = new DrawerAdapter(context);
        mDrawerAdapter.addItem(context.getString(R.string.drawer_overview),
                OutletsViewFragment.class.getName(), 0, true).bitmap = BitmapFactory.decodeResource(context.getResources(), android.R.drawable.ic_menu_send);

        {
            DrawerAdapter.DrawerItem item = mDrawerAdapter.addItem(context.getString(R.string.drawer_change_view_type),
                    null, 1, false);
            TypedValue value = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.ic_action_view_as_grid, value, true);
            item.bitmap = BitmapFactory.decodeResource(context.getResources(), value.resourceId);
            item.clickHandler = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    changeToDialog(context, OutletsViewTypeDialog.class.getName());
                }
            };
        }

        mDrawerAdapter.addItem(context.getString(R.string.drawer_timer),
                TimerFragment.class.getName(), 0, true).bitmap = BitmapFactory.decodeResource(context.getResources(), android.R.drawable.ic_menu_recent_history);

        mDrawerAdapter.addItem(context.getString(R.string.drawer_devices),
                DevicesFragment.class.getName(), 0, true).bitmap = BitmapFactory.decodeResource(context.getResources(), android.R.drawable.ic_menu_preferences);

        mDrawerAdapter.addSeparator();

        mDrawerAdapter.addItem(context.getString(R.string.drawer_preferences),
                PreferencesFragment.class.getName(), 0, false);

        mDrawerAdapter.addItem(context.getString(R.string.drawer_donate),
                DonationsFragment.class.getName(), 0, false).mExtra = DonationsFragment.createExtra(
                BuildConfig.DEBUG, true, DonateData.GOOGLE_PUBKEY, DonateData.GOOGLE_CATALOG,
                context.getResources().getStringArray(R.array.donation_google_catalog_values), true,
                DonateData.PP_USER, DonateData.PP_CURRENCY_CODE, DonateData.PP_DECODED_URI,
                true, DonateData.FLATTR_PROJECT_URL, DonateData.FLATTR_URL, false, null);

        mDrawerAdapter.addItem(context.getString(R.string.drawer_feedback),
                FeedbackFragment.class.getName(), 0, false);

        mRecyclerView.setAdapter(mDrawerAdapter);
        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(mRecyclerView.getContext(), this, null));

        if (restore == RestorePositionEnum.RestoreLastSaved) {
            // Restore the last visited screen
            Bundle extra = SharedPrefs.getInstance().getFirstTabExtra();
            String className = SharedPrefs.getInstance().getFirstTab();
            int pos = SharedPrefs.getInstance().getFirstTabPosition();

            if (className == null || className.isEmpty() || pos == -1) {
                className = OutletsViewFragment.class.getName();
                pos = mDrawerAdapter.indexOf(className);
                extra = null;
            }
            //backstack.add(new BackStackEntry(className, null));
            changeToFragment(className, extra, false, pos);
        } else if (restore == RestorePositionEnum.RestoreAfterConfigurationChanged && currentFragmentClass != null) {
            context.getFragmentManager().beginTransaction().attach(currentFragment).commitAllowingStateLoss();
        }

        if (mDrawerLayout != null) {
            createDrawerToggle();
        }
    }

    public void createBackButton(OnNavigationBackButtonPressed onNavigationBackButtonPressed) {
        ActionBarActivity context = mDrawerActivity.get();
        if (context == null) // should never happen
            return;
        this.onNavigationBackButtonPressed = onNavigationBackButtonPressed;
        mDrawerToggle = null;
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        context.getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
        context.getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_USE_LOGO |
                ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE);
    }

    public void createDrawerToggle() {
        onNavigationBackButtonPressed = null;
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        ActionBarActivity context = mDrawerActivity.get();
        if (context == null) // should never happen
            return;
        context.getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
        context.getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_USE_LOGO |
                ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(
                context,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
        ) {
            public void onDrawerClosed(View view) {
            }

            public void onDrawerOpened(View drawerView) {
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();
    }

    public void onPrepareOptionsMenu(Menu menu) {
        if (mDrawerLayout == null)
            return;

        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mRecyclerView);
        for (DrawerStateChanged drawerStateChanged : observers) {
            drawerStateChanged.drawerState(drawerOpen);
        }
    }

    public void menuKeyPressed() {
        if (isLoading() || mDrawerLayout == null)
            return;

        boolean drawerControllableByMenuKey = false;
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
        if (mDrawerToggle == null) {
            onNavigationBackButtonPressed.onNavigationBackButtonPressed();
            return true;
        } else
            return mDrawerToggle.onOptionsItemSelected(item);
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

    public void changeToFragment(Fragment fragment, String fragmentClassName) {
        final Activity context = mDrawerActivity.get();
        if (context == null || fragmentClassName == null) // should never happen
            return;
        currentFragmentClass = fragmentClassName;
        currentFragment = fragment;
        FragmentTransaction ft = context.getFragmentManager().beginTransaction();
        ft.replace(R.id.content_frame, currentFragment);
        try {
            ft.commit();
        } catch (IllegalStateException exception) {
            ft.commitAllowingStateLoss();
        }
    }

    private void changeToFragment(String fragmentClassName, Bundle extra, boolean addToBackstack, int position) {
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
            } catch (Fragment.InstantiationException ignored) {
                extra = null;
                fragmentClassName = OutletsViewFragment.class.getName();
                currentFragment = Fragment.instantiate(context, fragmentClassName);
            }

            currentFragmentClass = fragmentClassName;
            changeArgumentsOfCurrentFragment(extra);

            FragmentTransaction ft = context.getFragmentManager().beginTransaction();
            ft.replace(R.id.content_frame, currentFragment);
            try {
                ft.commit();
            } catch (IllegalStateException exception) {
                ft.commitAllowingStateLoss();
            }
//            try {
//            } catch (Exception exception) {
//                if (!App.isDebug()) {
//                    InAppNotifications.showException(context, exception, "fragmentName: " + currentFragmentClass);
//                }
//                exception.printStackTrace();
//
//                App.getMainThreadHandler().post(new Runnable() {
//                    @Override
//                    public void run() {
//                        FragmentTransaction ft = context.getFragmentManager().beginTransaction();
//                        ft.replace(R.id.content_frame, currentFragment);
//                        try {
//                            ft.commit();
//                        } catch (IllegalStateException illegalException) {
//                            ft.commitAllowingStateLoss();
//                        }
//                    }
//                });
//            }
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
    public boolean onItemClick(View view, int position, boolean isLongClick) {
        if (!mDrawerAdapter.isClickableType(position))
            return false;

        Activity context = mDrawerActivity.get();
        if (context == null) // should never happen
            return false;

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
            return true;
        }

        // No click handler: Should be a fragment change request
        if (item.fragmentClassName == null || item.fragmentClassName.isEmpty())
            return false;

        changeToFragment(item.fragmentClassName, item.mExtra, true, position);
        return true;
    }

    public enum RestorePositionEnum {
        NoRestore,
        RestoreLastSaved,
        RestoreAfterConfigurationChanged
    }


    public interface OnNavigationBackButtonPressed {
        void onNavigationBackButtonPressed();
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
