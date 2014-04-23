package oly.netpowerctrl.navigation_drawer;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.lang.ref.WeakReference;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.main.DevicesFragment;
import oly.netpowerctrl.main.FeedbackDialog;
import oly.netpowerctrl.main.HelpFragment;
import oly.netpowerctrl.main.OutletsFragment;
import oly.netpowerctrl.main.ScenesFragment;
import oly.netpowerctrl.preferences.PreferencesFragment;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.ChangeArgumentsFragment;
import oly.netpowerctrl.utils.OnBackButton;

/**
 * All navigation drawer related functionality used by the main activity
 */
public class DrawerController {
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerAdapter mDrawerAdapter;


    //private CharSequence mTitle;
    private boolean drawerControllableByMenuKey = false;

    private Fragment currentFragment;
    private Fragment currentAccompanyFragment;
    private String currentFragmentClass;
    private String currentAccompanyFragmentClass;
    public int drawerLastItemPosition;

    WeakReference<Activity> mDrawerActivity;

    public boolean isLoading() {
        return (mDrawerLayout == null);
    }

    public void setTitle(CharSequence mTitle) {
        //this.mTitle = mTitle;
        Activity context = mDrawerActivity.get();
        if (context == null) // should never happen
            return;
        //noinspection ConstantConditions
        context.getActionBar().setTitle(mTitle);
    }

    public void createDrawer(final Activity context) {
        mDrawerActivity = new WeakReference<Activity>(context);
        // References for the drawer
        mDrawerLayout = (DrawerLayout) context.findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) context.findViewById(R.id.left_drawer_list);

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener

        mDrawerAdapter = new DrawerAdapter(context);
        mDrawerAdapter.addHeader(context.getString(R.string.drawer_single_title));
        mDrawerAdapter.addItem(context.getString(R.string.drawer_overview), "",
                OutletsFragment.class.getName(), false);

        mDrawerAdapter.usePositionForGroups();

        mDrawerAdapter.addHeader(context.getString(R.string.drawer_multiple_title));
        mDrawerAdapter.addItem(context.getString(R.string.drawer_scenes), "",
                ScenesFragment.class.getName(), false);
        mDrawerAdapter.usePositionForScenes();

        mDrawerAdapter.add(context.getResources().getStringArray(R.array.drawer_titles_app),
                context.getResources().getStringArray(R.array.drawer_descriptions_app),
                new String[]{"", DevicesFragment.class.getName(),
                        PreferencesFragment.class.getName(), HelpFragment.class.getName()}
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
                onBackPressed();
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // Restore the last visited screen
        int pos = mDrawerAdapter.indexOf(SharedPrefs.getFirstTab());
        if (pos == -1) {
            if (NetpowerctrlApplication.getDataController().configuredDevices.size() > 0)
                pos = mDrawerAdapter.indexOf(OutletsFragment.class.getName());
            else
                pos = mDrawerAdapter.indexOf(DevicesFragment.class.getName());
        }
        selectItem(pos);

        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    public void onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        drawerControllableByMenuKey = menu.size() <= 2;
        if (drawerOpen)
            for (int i = 0; i < menu.size(); i++) {
                menu.getItem(i).setVisible(false);
            }
    }

    public void menuKeyPressed() {
        if (isLoading())
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
        if (currentPosition < mDrawerAdapter.getCount() &&
                mDrawerAdapter.getItemViewType(currentPosition) != 0) {
            DrawerAdapter.DrawerItem item = (DrawerAdapter.DrawerItem) mDrawerAdapter.getItem(currentPosition);
            if (!item.fragmentClassName.isEmpty() && !item.fragmentClassName.contains("Dialog"))
                SharedPrefs.setFirstTab(item.fragmentClassName);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerToggle.onOptionsItemSelected(item);
    }

    public boolean onBackPressed() {
        if (currentFragment != null && currentFragment instanceof OnBackButton)
            if (((OnBackButton) currentFragment).onBackButton())
                return true;
        if (currentAccompanyFragment != null && currentAccompanyFragment instanceof OnBackButton)
            if (((OnBackButton) currentAccompanyFragment).onBackButton())
                return true;
        return false;
    }


    /* The click listener for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    public void changeToFragment(String fragmentClassName) {
        int pos = mDrawerAdapter.indexOf(fragmentClassName);
        if (pos == -1) {
            return;
        }
        selectItem(pos);
    }

    public void selectItem(final int position) {
        Activity context = mDrawerActivity.get();
        if (context == null) // should never happen
            return;

        if (mDrawerAdapter.getItemViewType(position) == 0)
            return;

        DrawerAdapter.DrawerItem item = (DrawerAdapter.DrawerItem) mDrawerAdapter.getItem(position);

        // First look at a click handler for that specific item
        if (item.clickHandler != null) {
            item.clickHandler.onClick(null);
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

        if (currentFragment == null || !item.fragmentClassName.equals(currentFragmentClass)) {
            currentFragment = Fragment.instantiate(context, item.fragmentClassName);
            currentFragmentClass = item.fragmentClassName;
        }

        // Deliver arguments to fragment via ChangeArgumentsFragment interface
        if (currentFragment instanceof ChangeArgumentsFragment) {
            ((ChangeArgumentsFragment) currentFragment).changeArguments(item.mExtra);
        }

        FragmentManager fragmentManager = context.getFragmentManager();
        if (item.mDialog) {
            FragmentTransaction ft = context.getFragmentManager().beginTransaction();
            Fragment prev = context.getFragmentManager().findFragmentByTag("dialog");
            if (prev != null) {
                ft.remove(prev);
            }
            ft.addToBackStack(null);
            ((DialogFragment) currentFragment).show(ft, "dialog");
            mDrawerList.post(new Runnable() {
                @Override
                public void run() {
                    mDrawerList.setItemChecked(drawerLastItemPosition, true);
                }
            });
        } else {
            DisplayMetrics m = context.getResources().getDisplayMetrics();

            // If there is enough space, show the accompany fragment, too.
            // Only show scene fragment as accompany fragment, if there are scenes available.
            boolean isAccompanyScene = item.mAccompanyClazz != null &&
                    item.mAccompanyClazz.equals(ScenesFragment.class.getName());

            if (m.widthPixels / m.density >= 800 && item.mAccompanyClazz != null &&
                    (!isAccompanyScene || NetpowerctrlApplication.getDataController().scenes.length() > 0)) {

                context.findViewById(R.id.content_frame2).setVisibility(View.VISIBLE);
                if (currentAccompanyFragment == null || !item.mAccompanyClazz.equals(currentAccompanyFragmentClass)) {
                    currentAccompanyFragment = Fragment.instantiate(context, item.mAccompanyClazz);
                    currentAccompanyFragmentClass = item.mAccompanyClazz;
                }
                fragmentManager.beginTransaction().replace(R.id.content_frame, currentFragment).
                        replace(R.id.content_frame2, currentAccompanyFragment).commit();
            } else {
                if (currentAccompanyFragment != null) {
                    fragmentManager.beginTransaction().replace(R.id.content_frame, currentFragment).
                            remove(currentAccompanyFragment).commit();
                    currentAccompanyFragment = null;
                    currentAccompanyFragmentClass = null;
                    context.findViewById(R.id.content_frame2).setVisibility(View.GONE);
                } else
                    fragmentManager.beginTransaction().replace(R.id.content_frame, currentFragment).commit();
            }
            // update selected item and title
            mDrawerList.setItemChecked(position, true);
            setTitle(item.mTitle);
            drawerLastItemPosition = position;
        }

        // close the drawer
        if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
            Handler mHandler = new Handler();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mDrawerLayout.closeDrawer(mDrawerList);
                }
            }, 150);
        }
    }

}
