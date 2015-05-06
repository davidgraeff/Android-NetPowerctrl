/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package oly.netpowerctrl.main;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SlidingPaneLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.graphic.LoadStoreIconData;
import oly.netpowerctrl.executables.ExecutableHideShowDialog;
import oly.netpowerctrl.executables.ExecutablesFragment;
import oly.netpowerctrl.groups.GroupListFragment;
import oly.netpowerctrl.ioconnection.IOConnectionsFragment;
import oly.netpowerctrl.preferences.PreferencesFragment;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.ui.FragmentUtils;
import oly.netpowerctrl.ui.notifications.ChangeLogNotification;
import oly.netpowerctrl.ui.notifications.InAppNotifications;

public class MainActivity extends ActionBarActivity {
    private boolean firstFragment = true;
    private SlidingPaneLayout panes = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Remove title bar
        this.supportRequestWindowFeature(Window.FEATURE_NO_TITLE);

        if (SharedPrefs.getInstance().isFullscreen()) {
            //Remove notification bar
            this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        // on android5+ color of system bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            int c = SharedPrefs.getInstance().isDarkTheme() ?
                    getResources().getColor(R.color.colorSecondaryDark) :
                    getResources().getColor(R.color.colorSecondaryLight);
            getWindow().setStatusBarColor(c);
            getWindow().setNavigationBarColor(c);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow(); // in Activity's onCreate() for instance
            w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        // Set theme, call super onCreate and set content view
        if (SharedPrefs.getInstance().isDarkTheme()) {
            setTheme(R.style.Theme_CustomDarkTheme);
        } else {
            setTheme(R.style.Theme_CustomLightTheme);
        }


        super.onCreate(savedInstanceState);

        assignContentView();

        FragmentManager fm = getFragmentManager();
        fm.addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                if (firstFragment) {
                    firstFragment = false;
                    return;
                }
                getSupportActionBar().setDisplayHomeAsUpEnabled(getFragmentManager().getBackStackEntryCount() > 1);
//                if (getFragmentManager().getBackStackEntryCount() <= 1) {
//                    Toast.makeText(MainActivity.this, getString(R.string.press_back_to_exit), Toast.LENGTH_SHORT).show();
//                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.menu_about: {
                FragmentUtils.changeToFragment(this, FeedbackFragment.class.getName());
                return true;
            }
            case R.id.menu_preferences: {
                FragmentUtils.changeToFragment(this, PreferencesFragment.class.getName());
                return true;
            }
            case R.id.menu_devices: {
                FragmentUtils.changeToFragment(this, IOConnectionsFragment.class.getName());
                return true;
            }
            case R.id.menu_device_hide_items:
                FragmentUtils.changeToDialog(this, ExecutableHideShowDialog.class.getName());
                return true;

        }
        return false;
    }

    private void assignContentView() {
        setContentView(R.layout.activity_main);

        boolean has_two_panes = getResources().getBoolean(R.bool.has_two_panes);
        LayoutInflater l = LayoutInflater.from(this);

        FragmentUtils.unloadFragment(this, "group");

        FrameLayout layout = (FrameLayout) findViewById(R.id.content);
        layout.removeAllViews();
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        View v = l.inflate(!has_two_panes ? R.layout.content_frame_with_sliding_group_list : R.layout.content_frame_with_group_list, null);
        layout.addView(v, lp);

        FragmentUtils.loadFragment(this, GroupListFragment.class.getName(), R.id.group_list_fragment, "group");
        FragmentUtils.changeToFragment(this, ExecutablesFragment.class.getName(), "outlets");

        if (!has_two_panes) {
            panes = (SlidingPaneLayout) findViewById(R.id.drawerLayout);
            Resources r = getResources();
            float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 240, r.getDisplayMetrics());
            panes.setParallaxDistance((int) px / 2);
            panes.setCoveredFadeColor(0);
            panes.setSliderFadeColor(0);
            panes.setShadowResourceLeft(R.drawable.drawer_shadow_left);
            panes.setShadowResourceRight(R.drawable.drawer_shadow);
            //if (panes.isSlideable()) panes.openPane();
        }

        if (SharedPrefs.getInstance().isBackground()) {
            Drawable d = LoadStoreIconData.loadBackgroundBitmap();
            findViewById(android.R.id.content).setBackground(d);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);
    }

    @Override
    protected void onStart() {
        super.onStart();
        App.getMainThreadHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (SharedPrefs.getInstance().hasBeenUpdated()) {
                    InAppNotifications.updatePermanentNotification(MainActivity.this, new ChangeLogNotification());
                }
            }
        }, 1500);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT ||
                newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            assignContentView();

        }
        // now the fragments
        super.onConfigurationChanged(newConfig);
    }

    public void closeGroupMenu() {
        if (panes != null) panes.closePane();
    }

    @Override
    public void onBackPressed() {
        if (panes != null && panes.isOpen()) {
            panes.closePane();
            return;
        }

        // Exit edit mode on back click
        Fragment fragment = getFragmentManager().findFragmentByTag("outlets");
        if (fragment != null && ((ExecutablesFragment) fragment).isEditMode()) {
            ((ExecutablesFragment) fragment).setEditMode(false);
            return;
        }

        // Change fragment on back click or close app
        if (getFragmentManager().getBackStackEntryCount() > 1) {
            getFragmentManager().popBackStack(null, 0);
            return;
        }

        super.onBackPressed();
    }
}