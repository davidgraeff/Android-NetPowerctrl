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

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.cengalabs.flatui.FlatUI;

import org.sufficientlysecure.donations.DonationsFragment;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.ui.navigation.NavigationController;
import oly.netpowerctrl.ui.notifications.ChangeLogNotification;
import oly.netpowerctrl.ui.notifications.InAppNotifications;
import oly.netpowerctrl.utils.AndroidStatusBarService;

public class MainActivity extends ActionBarActivity {
    private static final long TIME_INTERVAL_MS = 2000;
    public static MainActivity instance = null;
    private final NavigationController navigationController = new NavigationController();
    private long mBackPressed;

    public MainActivity() {
        instance = this;
    }

    public static NavigationController getNavigationController() {
        return instance.navigationController;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AndroidStatusBarService.REQUEST_CODE) {
            if (AndroidStatusBarService.instance != null)
                AndroidStatusBarService.instance.onActivityResult(resultCode, data);
            return;
        }
        // Work-a-round
        if (DonationsFragment.class.equals(navigationController.getCurrentFragment().getClass()))
            navigationController.getCurrentFragment().onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        instance = this;

        //Remove title bar
        this.supportRequestWindowFeature(Window.FEATURE_NO_TITLE);

        if (SharedPrefs.getInstance().isFullscreen()) {
            //Remove notification bar
            this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        super.onCreate(savedInstanceState);

        // Converts the default values (radius, size, border) to dp to be compatible with different
        // screen sizes. If you skip this there may be problem with different screen densities
        FlatUI.initDefaultValues(this);

        // Set theme, call super onCreate and set content view
        if (SharedPrefs.getInstance().isDarkTheme()) {
            setTheme(R.style.Theme_CustomDarkTheme);
            FlatUI.setDefaultTheme(FlatUI.SEA);
        } else {
            setTheme(R.style.Theme_CustomLightTheme);
            FlatUI.setDefaultTheme(FlatUI.SEA);
        }

        navigationController.createDrawerAdapter(this);
        assignContentView();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        navigationController.restoreLastOpenedFragment();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void assignContentView() {
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);
        if (SharedPrefs.getInstance().isFullscreen()) {
            getSupportActionBar().hide();
        }

        navigationController.setActivity(this);

        if (SharedPrefs.getInstance().isBackground()) {
            View v = findViewById(R.id.content_frame);
            Bitmap b = LoadStoreIconData.loadBackgroundBitmap();
            Drawable d = new BitmapDrawable(getResources(), b);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                v.setBackground(d);
            else
                //noinspection deprecation
                v.setBackgroundDrawable(d);
        }

        // enable ActionBar app icon to behave as action to toggle nav drawer
        boolean has_two_panes = getResources().getBoolean(R.bool.has_two_panes);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(!has_two_panes);
            actionBar.setHomeButtonEnabled(!has_two_panes);
        }
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (navigationController.isLoading())
            return super.onPrepareOptionsMenu(menu);

        navigationController.onPrepareOptionsMenu(menu);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!AppData.getInstance().deviceCollection.hasDevices() && SharedPrefs.getInstance().getFirstTabPosition() == -1) {
            navigationController.changeToFragment(IntroductionFragment.class.getName());
        } else {
            App.getMainThreadHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (SharedPrefs.getInstance().hasBeenUpdated()) {
                        InAppNotifications.updatePermanentNotification(MainActivity.this, new ChangeLogNotification());
                    }
                }
            }, 1500);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        navigationController.saveSelection();
        navigationController.detachCurrentFragment();
    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return navigationController.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT ||
                newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {

            navigationController.detachCurrentFragment();
            assignContentView();
            navigationController.restoreLastOpenedFragment();

        }
        // now the fragments
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void setTitle(CharSequence title) {
        navigationController.setTitle(title);
    }

    @Override
    public void onBackPressed() {
        if (navigationController.onBackPressed())
            return;

        if (mBackPressed + TIME_INTERVAL_MS > System.currentTimeMillis()) {
            finish();
            return;
        } else if (getFragmentManager().getBackStackEntryCount() == 0) {
            Toast.makeText(this, getString(R.string.press_back_to_exit), Toast.LENGTH_SHORT).show();
            mBackPressed = System.currentTimeMillis();
            return;
        }

        super.onBackPressed();
    }
}