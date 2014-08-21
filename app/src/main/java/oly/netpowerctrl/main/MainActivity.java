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

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Toast;

import java.lang.reflect.Field;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.application_state.NetpowerctrlService;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.ActivityWithIconCache;
import oly.netpowerctrl.utils.ChangeLogUtil;
import oly.netpowerctrl.utils.Donate;
import oly.netpowerctrl.utils.IconDeferredLoadingThread;
import oly.netpowerctrl.utils.NFC;
import oly.netpowerctrl.utils_gui.NavigationController;

public class MainActivity extends Activity implements NfcAdapter.CreateNdefMessageCallback, ActivityWithIconCache {
    private static final long TIME_INTERVAL_MS = 2000;
    public static MainActivity instance = null;
    public final Donate donate = new Donate();
    private final IconDeferredLoadingThread mIconCache = new IconDeferredLoadingThread();
    private final NavigationController navigationController = new NavigationController();
    private long mBackPressed;

    public static NavigationController getNavigationController() {
        return instance.navigationController;
    }

    public IconDeferredLoadingThread getIconCache() {
        return mIconCache;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        donate.onDestroy(this);
        instance = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        donate.onActivityResult(this, requestCode, resultCode, data);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        instance = this;
        super.onCreate(savedInstanceState);
        // Set theme, call super onCreate and set content view
        if (SharedPrefs.isDarkTheme()) {
            setTheme(R.style.Theme_CustomDarkTheme);
        } else {
            setTheme(R.style.Theme_CustomLightTheme);
        }

        assignContentView();

        // Clear the backstack on entering this activity
        getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        // Hack to always show the overflow of the actionbar instead of
        // relying on the menu button that is only present on some devices
        // and may cause confusion.
        try {
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(ViewConfiguration.get(this), false);
            }
        } catch (Exception ex) {
            // Ignore
        }

        checkUseHomeButton();

        mIconCache.start();

        // Delayed loading of drawer and nfc
        NetpowerctrlApplication.getMainThreadHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                navigationController.createDrawer(MainActivity.this, NavigationController.RestorePositionEnum.RestoreLastSaved);

                // NFC
                NfcAdapter mNfcAdapter = NfcAdapter.getDefaultAdapter(MainActivity.this);
                if (mNfcAdapter != null) {
                    // Register callback
                    mNfcAdapter.setNdefPushMessageCallback(MainActivity.this,
                            MainActivity.this);
                }

                donate.start(MainActivity.this);
            }
        }, 100);
        NetpowerctrlApplication.getMainThreadHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (SharedPrefs.showChangeLog()) {
                    ChangeLogUtil.showChangeLog(MainActivity.this);
                }
            }
        }, 150);
    }

    private void assignContentView() {
        setContentView(R.layout.activity_main);
        if (SharedPrefs.isBackground()) {
            View v = findViewById(R.id.content_frame);
            v.setBackgroundResource(R.drawable.bg);
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
        NetpowerctrlService.stopUseService();
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        return NFC.createNdefMessage();
    }

    @Override
    public void onResume() {
        NFC.checkIntentForNFC(this, getIntent());
        NetpowerctrlService.useService(true, false);
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
        //noinspection ConstantConditions
        getActionBar().setDisplayHomeAsUpEnabled(!has_two_panes);
        getActionBar().setHomeButtonEnabled(!has_two_panes);
    }

    @Override
    public void setTitle(CharSequence title) {
        navigationController.setTitle(title);
        //noinspection ConstantConditions
        getActionBar().setTitle(title);
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