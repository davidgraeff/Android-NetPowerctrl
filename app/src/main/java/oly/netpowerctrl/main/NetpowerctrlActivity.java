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
import android.content.Intent;
import android.content.res.Configuration;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewConfiguration;

import java.lang.reflect.Field;

import de.cketti.library.changelog.ChangeLog;
import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.navigation_drawer.DrawerController;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.transfer.GDrive;
import oly.netpowerctrl.transfer.NFC;

public class NetpowerctrlActivity extends Activity implements NfcAdapter.CreateNdefMessageCallback {
    public static NetpowerctrlActivity instance = null;

    // Drawer
    private final DrawerController mDrawer = new DrawerController();
    final GDrive gDrive = new GDrive();

    @Override
    protected void onStop() {
        gDrive.onStop();
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        gDrive.onStart(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        gDrive.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        gDrive.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
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

        // enable ActionBar app icon to behave as action to toggle nav drawer
        //noinspection ConstantConditions
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        // Delayed loading of drawer and nfc
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mDrawer.createDrawer(NetpowerctrlActivity.this, true);

                // NFC
                NfcAdapter mNfcAdapter = NfcAdapter.getDefaultAdapter(NetpowerctrlActivity.this);
                if (mNfcAdapter != null) {
                    // Register callback
                    mNfcAdapter.setNdefPushMessageCallback(NetpowerctrlActivity.this,
                            NetpowerctrlActivity.this);
                }

                if (!SharedPrefs.isFirstRun()) {
                    ChangeLog cl = new ChangeLog(NetpowerctrlActivity.this);
                    if (cl.isFirstRun())
                        cl.getLogDialog().show();
                }
            }
        }, 100);
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
        NetpowerctrlApplication.instance.stopUseListener();
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        return NFC.createNdefMessage();
    }

    @Override
    public void onResume() {
        super.onResume();
        NFC.checkIntentForNFC(this, getIntent());
        NetpowerctrlApplication.instance.useListener();
        NetpowerctrlApplication.instance.findDevices(null);
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
            mDrawer.createDrawer(NetpowerctrlActivity.this, false);
            mDrawer.selectItem(lastPos, true);
        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setContentView(R.layout.activity_main);
            int lastPos = mDrawer.drawerLastItemPosition;
            mDrawer.createDrawer(NetpowerctrlActivity.this, false);
            mDrawer.selectItem(lastPos, true);
        }
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
        super.onBackPressed();
    }
}