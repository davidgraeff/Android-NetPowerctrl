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
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

import java.lang.reflect.Method;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.pluginservice.PluginService;
import oly.netpowerctrl.pluginservice.onServiceReady;
import oly.netpowerctrl.preferences.PreferencesFragment;
import oly.netpowerctrl.ui.FragmentUtils;
import oly.netpowerctrl.ui.notifications.ChangeLogNotification;
import oly.netpowerctrl.ui.notifications.InAppNotifications;

public class MainActivity extends ActionBarActivity implements SlidingMenu.OnCloseListener, SlidingMenu.OnOpenListener {
    public static MainActivity instance = null;
    onServiceReady start_after_data_loaded = new onServiceReady() {
        @Override
        public boolean onServiceReady(PluginService service) {
            if (!service.getAppData().deviceCollection.hasDevices() && SharedPrefs.getInstance().getFirstTabPosition() == -1) {
                //navigationController.changeToFragment(IntroductionFragment.class.getName());
                //TODO call introduction activity
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
            return false;
        }

        @Override
        public void onServiceFinished(PluginService service) {

        }
    };
    private boolean firstFragment = true;
    private SlidingMenu menu;

    public MainActivity() {
        instance = this;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
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

        // on android5+ color of system bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            int c = SharedPrefs.getInstance().isDarkTheme() ?
                    getResources().getColor(R.color.colorSecondaryDark) :
                    getResources().getColor(R.color.colorSecondaryLight);
            getWindow().setStatusBarColor(c);
            getWindow().setNavigationBarColor(c);
        }

        super.onCreate(savedInstanceState);

        // Set theme, call super onCreate and set content view
        if (SharedPrefs.getInstance().isDarkTheme()) {
            setTheme(R.style.Theme_CustomDarkTheme);
        } else {
            setTheme(R.style.Theme_CustomLightTheme);
        }

        //navigationController.createDrawerAdapter(this);
        assignContentView();

        FragmentManager fm = getFragmentManager();
        fm.addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                if (firstFragment) {
                    firstFragment = false;
                    return;
                }
                if (getFragmentManager().getBackStackEntryCount() <= 1) {
                    Toast.makeText(MainActivity.this, getString(R.string.press_back_to_exit), Toast.LENGTH_SHORT).show();
                }
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
            case R.id.menu_about: {
                FragmentUtils.changeToFragment(this, FeedbackFragment.class.getName());
                return true;
            }
            case R.id.menu_preferences: {
                FragmentUtils.changeToFragment(this, PreferencesFragment.class.getName());
                return true;
            }
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void assignContentView() {
        setContentView(R.layout.activity_main);

        boolean has_two_panes = getResources().getBoolean(R.bool.has_two_panes);

        menu = (SlidingMenu) findViewById(R.id.slidingmenulayout);
        menu.setMenu(R.layout.devices_fragment);
        menu.setShadowDrawable(R.drawable.shadow);
        if (!has_two_panes) {
            menu.setBehindOffset(150);
            menu.setSecondaryMenu(R.layout.group_list_fragment);
            menu.setSecondaryShadowDrawable(R.drawable.shadowright);
            menu.setContent(R.layout.content_frame);
            menu.setMode(SlidingMenu.LEFT_RIGHT);
        } else {
            int width;
            Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay();
            try {
                Class<?> cls = Display.class;
                Class<?>[] parameterTypes = {Point.class};
                Point parameter = new Point();
                Method method = cls.getMethod("getSize", parameterTypes);
                method.invoke(display, parameter);
                width = parameter.x;
            } catch (Exception e) {
                width = display.getWidth();
            }

            menu.setBehindOffset(width / 2);
            menu.setSecondaryMenu(null);
            menu.setContent(R.layout.content_frame_with_group_list);
            menu.setMode(SlidingMenu.LEFT);
        }
        FragmentUtils.changeToFragment(this, OutletsFragment.class.getName(), "outlets");

        //menu.setStatic(has_two_vrr.anes);
        menu.setBehindCanvasTransformer(new SlidingMenu.CanvasTransformer() {
            @Override
            public void transformCanvas(Canvas canvas, float percentOpen, float xOffset) {
                if (xOffset == 0)
                    canvas.scale(percentOpen, 1, 0, 0);
                else
                    canvas.scale(percentOpen, 1, 0, 0);
                //canvas.translate(xOffset, 0);
            }
        });
        menu.setOnOpenListener(this);
        menu.setSecondaryOnOpenListner(this);
        menu.setOnCloseListener(this);

        if (SharedPrefs.getInstance().isBackground()) {
            View v = findViewById(R.id.content_frame);
            Drawable d = LoadStoreIconData.loadBackgroundBitmap();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                v.setBackground(d);
            else
                //noinspection deprecation
                v.setBackgroundDrawable(d);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);
    }
//
//    @Override
//    public boolean onPrepareOptionsMenu(Menu menu) {
//        if (navigationController.isLoading())
//            return super.onPrepareOptionsMenu(menu);
//
//        navigationController.onPrepareOptionsMenu(menu);
//        return super.onPrepareOptionsMenu(menu);
//    }

    @Override
    protected void onStart() {
        super.onStart();
        PluginService.observersServiceReady.register(start_after_data_loaded);
    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
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


    @Override
    public void onBackPressed() {
        if (menu.isMenuShowing()) {
            menu.showContent();
            return;
        }

        if (getFragmentManager().getBackStackEntryCount() > 1) {
            getFragmentManager().popBackStack(null, 0);
            return;
        }

        super.onBackPressed();
    }

    @Override
    public void onOpen() {
        invalidateOptionsMenu();
    }

    @Override
    public void onClose() {
        invalidateOptionsMenu();
    }
}