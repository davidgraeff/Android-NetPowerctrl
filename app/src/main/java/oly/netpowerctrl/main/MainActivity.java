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
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import org.sufficientlysecure.donations.DonationsFragment;

import java.io.IOException;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.listen_service.ListenService;
import oly.netpowerctrl.utils.navigation.NavigationController;
import oly.netpowerctrl.utils.notifications.ChangeLogNotification;
import oly.netpowerctrl.utils.notifications.InAppNotifications;
import oly.netpowerctrl.widget.WidgetUpdateService;

public class MainActivity extends ActionBarActivity {
    private static final long TIME_INTERVAL_MS = 2000;
    public static MainActivity instance = null;
    private final NavigationController navigationController = new NavigationController();
    private long mBackPressed;

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

        // Set theme, call super onCreate and set content view
        if (SharedPrefs.getInstance().isDarkTheme()) {
            setTheme(R.style.Theme_CustomDarkTheme);
        } else {
            setTheme(R.style.Theme_CustomLightTheme);
        }

        assignContentView();

        checkUseHomeButton();

        WidgetUpdateService.ForceUpdateAll(this);

        App.getMainThreadHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (SharedPrefs.getInstance().hasBeenUpdated()) {
                    InAppNotifications.addPermanentNotification(MainActivity.this, new ChangeLogNotification());
                }

            }
        }, 150);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        navigationController.createDrawer(MainActivity.this, NavigationController.RestorePositionEnum.RestoreLastSaved);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void assignContentView() {
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);
        //getSupportActionBar().setCustomView(R.layout.notification_container);

        if (SharedPrefs.getInstance().isBackground() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            View v = findViewById(R.id.content_frame);
            Drawable d = LoadStoreIconData.loadDrawable(this, LoadStoreIconData.uuidForBackground(),
                    LoadStoreIconData.IconType.BackgroundImage, LoadStoreIconData.IconState.StateNotApplicable);
            if (d == null)
                try {
                    d = new BitmapDrawable(getResources(), BitmapFactory.decodeStream(getAssets().open("backgrounds/bg.jpg")));
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            v.setBackground(d);
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
    public boolean onKeyUp(int keyCode, @SuppressWarnings("NullableProblems") KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            navigationController.menuKeyPressed();
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();

        navigationController.saveSelection();

        // Stop listener
        ListenService.stopUseService();
    }

    @Override
    public void onResume() {
        AppData.useAppData();
        ListenService.useService(getApplicationContext(), true, false);
        super.onResume();
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
            checkUseHomeButton();
            navigationController.createDrawer(MainActivity.this,
                    NavigationController.RestorePositionEnum.RestoreAfterConfigurationChanged);

        }
        // now the fragments
        super.onConfigurationChanged(newConfig);
    }

    private void checkUseHomeButton() {
        // enable ActionBar app icon to behave as action to toggle nav drawer
        boolean has_two_panes = getResources().getBoolean(R.bool.has_two_panes);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(!has_two_panes);
            actionBar.setHomeButtonEnabled(!has_two_panes);
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        navigationController.setTitle(title);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
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