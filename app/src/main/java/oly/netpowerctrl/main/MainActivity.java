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
import android.view.ViewConfiguration;
import android.widget.Toast;

import java.lang.reflect.Field;

import de.cketti.library.changelog.ChangeLog;
import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.application_state.NetpowerctrlService;
import oly.netpowerctrl.application_state.ServiceReady;
import oly.netpowerctrl.backup.drive.GDrive;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.Donate;
import oly.netpowerctrl.utils.NFC;
import oly.netpowerctrl.utils.gui.DrawerController;

public class MainActivity extends Activity implements NfcAdapter.CreateNdefMessageCallback {
    private static final long TIME_INTERVAL_MS = 2000;
    public static MainActivity instance = null;
    public final Donate donate = new Donate();
    public final GDrive gDrive = new GDrive();
    // Drawer
    private final DrawerController mDrawer = new DrawerController();
    private long mBackPressed;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        donate.onDestroy(this);
        instance = null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        gDrive.onSaveInstanceState(outState);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        gDrive.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
        donate.onActivityResult(this, requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        instance = this;
        super.onCreate(savedInstanceState);
        gDrive.onCreate(savedInstanceState);
        // Set theme, call super onCreate and set content view
        if (SharedPrefs.isDarkTheme()) {
            setTheme(R.style.Theme_CustomDarkTheme);
        } else {
            setTheme(R.style.Theme_CustomLightTheme);
        }

        setContentView(R.layout.activity_main);

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

        // Delayed loading of drawer and nfc
        NetpowerctrlApplication.getMainThreadHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mDrawer.createDrawer(MainActivity.this, true);

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
                if (!SharedPrefs.isFirstRun()) {
                    ChangeLog cl = new ChangeLog(MainActivity.this);
                    if (cl.isFirstRun())
                        cl.getLogDialog().show();
                }
            }
        }, 150);
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mDrawer.isLoading())
            return super.onPrepareOptionsMenu(menu);

        mDrawer.onPrepareOptionsMenu(menu);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onKeyUp(int keyCode, @SuppressWarnings("NullableProblems") KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            mDrawer.menuKeyPressed();
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mDrawer.saveSelection();

        // Stop listener
        NetpowerctrlService.stopUseListener();
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        return NFC.createNdefMessage();
    }

    @Override
    public void onResume() {
        super.onResume();
        NFC.checkIntentForNFC(this, getIntent());
        NetpowerctrlService.useListener();
        NetpowerctrlService.registerServiceReadyObserver(new ServiceReady() {
            @Override
            public boolean onServiceReady(NetpowerctrlService service) {
                service.findDevices(null);
                return false;
            }

            @Override
            public void onServiceFinished() {

            }
        });
    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawer.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.activity_main);
            int lastPos = mDrawer.drawerLastItemPosition;
            mDrawer.createDrawer(MainActivity.this, false);
            mDrawer.selectItem(lastPos, true);
            checkUseHomeButton();
        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setContentView(R.layout.activity_main);
            int lastPos = mDrawer.drawerLastItemPosition;
            mDrawer.createDrawer(MainActivity.this, false);
            mDrawer.selectItem(lastPos, true);
            checkUseHomeButton();
        }
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
        mDrawer.setTitle(title);
        //noinspection ConstantConditions
        getActionBar().setTitle(title);
    }

    public void changeToFragment(String fragmentClassName) {
        mDrawer.changeToFragment(fragmentClassName);
    }

    @Override
    public void onBackPressed() {
        if (mDrawer.onBackPressed())
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