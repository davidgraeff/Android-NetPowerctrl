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
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Configuration;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.listadapter.AdapterUpdateManager;
import oly.netpowerctrl.listadapter.DrawerAdapter;
import oly.netpowerctrl.preferences.PreferencesFragment;
import oly.netpowerctrl.service.DeviceQuery;
import oly.netpowerctrl.service.NetpowerctrlService;
import oly.netpowerctrl.utils.OutletCommandGroup;
import oly.netpowerctrl.utils.SharedPrefs;

public class NetpowerctrlActivity extends Activity implements NfcAdapter.CreateNdefMessageCallback {
    public static NetpowerctrlActivity _this = null;
    final static int ACTIVITY_REQUEST_ADDGROUP = 12;

    // NFC
    private NfcAdapter mNfcAdapter;

    // Drawer
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerAdapter mDrawerAdapter;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;

    // Core
    public AdapterUpdateManager adapterUpdateManger;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _this = this;
        setContentView(R.layout.activity_main);

        mTitle = mDrawerTitle = getTitle();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener
        String[] mFragmentNames = getResources().getStringArray(R.array.drawer_titles);
        String[] mFragmentDesc = getResources().getStringArray(R.array.drawer_descriptions);
        String[] mFragmentClasses = {
                "",
                OutletsListFragment.class.getName(),
                GroupListFragment.class.getName(),
                "",
                NewDevicesListFragment.class.getName(),
                ConfiguredDevicesListFragment.class.getName(),
                "",
                PreferencesFragment.class.getName(),
                HelpFragment.class.getName(),
                AboutDialog.class.getName()};

        mDrawerAdapter = new DrawerAdapter(this);
        for (int i=0;i< mFragmentNames.length;++i) {
            if (mFragmentDesc[i].equals("-")) {
                mDrawerAdapter.addHeader(mFragmentNames[i]);
            } else {
                mDrawerAdapter.addItem(mFragmentNames[i], mFragmentDesc[i], mFragmentClasses[i], mFragmentClasses[i].contains("Dialog"));
            }
        }

        mDrawerList.setAdapter(mDrawerAdapter);
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        // enable ActionBar app icon to behave as action to toggle nav drawer
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
                ) {
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // Restore the last visited screen
        if (savedInstanceState == null) {
            int pos = SharedPrefs.getFirstTab(this);
            if (pos==-1)
                pos = indexOf(HelpFragment.class.getName(), mFragmentClasses);
            selectItem(pos);
        } else {
            selectItem(savedInstanceState.getInt("navigation"));
        }

        // NFC
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter != null) {
            // Register callback
            mNfcAdapter.setNdefPushMessageCallback(this, this);
        }

        // Core
        adapterUpdateManger = new AdapterUpdateManager(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle icicle) {
        super.onSaveInstanceState(icicle);
        icicle.putInt("navigation", mDrawerList.getCheckedItemPosition());
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPrefs.setFirstTab(this, mDrawerList.getCheckedItemPosition());
        adapterUpdateManger.stop();
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        String text = ("Beam me up, Android!\n\n" +
                "Beam Time: " + System.currentTimeMillis());
        NdefMessage msg = new NdefMessage(
                        NdefRecord.createApplicationRecord("oly.netpowerctrl"),
                        NdefRecord.createMime("application/oly.netpowerctrl", text.getBytes())
                );
        return msg;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check to see that the Activity started due to an Android Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            Intent intent = getIntent();
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                    NfcAdapter.EXTRA_NDEF_MESSAGES);
            // only one message sent during the beam
            NdefMessage msg = (NdefMessage) rawMsgs[0];
            String beamedDeviceConfigurations = new String(msg.getRecords()[1].getPayload());
        }

        // we may be returning from a configure dialog
        adapterUpdateManger.updateConfiguredDevices();
        adapterUpdateManger.start();

        Intent it = new Intent(this, NetpowerctrlService.class);
        it.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startService(it);

        DeviceQuery.sendBroadcastQuery(this);
    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //MenuInflater inflater = getMenuInflater();
        //inflater.inflate(R.menu.outlets, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        //boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        //menu.findItem(R.id.menu_test11).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
         // The action bar home/up action should open or close the drawer.
         // ActionBarDrawerToggle will take care of this.
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle action buttons
        switch(item.getItemId()) {

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /* The click listener for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {


        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private void selectItem(int position) {
        if (mDrawerAdapter.getItemViewType(position)==0)
            return;
        DrawerAdapter.DrawerItem item = (DrawerAdapter.DrawerItem) mDrawerAdapter.getItem(position);
        if (item.mClazz.equals(""))
            return;

        Fragment fragment = Fragment.instantiate(this, item.mClazz);
        FragmentManager fragmentManager = getFragmentManager();
        if (item.mdialog) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            Fragment prev = getFragmentManager().findFragmentByTag("dialog");
            if (prev != null) {
                ft.remove(prev);
            }
            ft.addToBackStack(null);
            ((DialogFragment)fragment).show(ft, "dialog");
        } else {
            fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
            // update selected item and title, then close the drawer
            mDrawerList.setItemChecked(position, true);
            setTitle(item.mTitle);
        }

        mDrawerLayout.closeDrawer(mDrawerList);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getActionBar().setTitle(mTitle);
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACTIVITY_REQUEST_ADDGROUP && resultCode == RESULT_OK) {
            Bundle shortcut_bundle = data.getExtras();
            Intent groupIntent = shortcut_bundle.getParcelable(Intent.EXTRA_SHORTCUT_INTENT);
            shortcut_bundle = groupIntent.getExtras();
            OutletCommandGroup og = OutletCommandGroup.fromString(shortcut_bundle.getString("commands"), this);
            adapterUpdateManger.adpGroups.addGroup(og);
        }
    }

    public static <T> int indexOf(T needle, T[] haystack)
    {
        for (int i=0; i<haystack.length; i++)
        {
            if (haystack[i] != null && haystack[i].equals(needle)
                    || needle == null && haystack[i] == null) return i;
        }

        return -1;
    }
}