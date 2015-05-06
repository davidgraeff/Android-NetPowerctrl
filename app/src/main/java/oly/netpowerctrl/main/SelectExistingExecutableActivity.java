package oly.netpowerctrl.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import oly.netpowerctrl.R;
import oly.netpowerctrl.executables.adapter.InputExecutables;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.ui.SelectFromListFragment;
import oly.netpowerctrl.utils.AndroidShortcuts;

public class SelectExistingExecutableActivity extends Activity implements SelectFromListFragment.onItemClicked {
    SelectFromListFragment s;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);

        // Set theme, call super onCreate and set content view
        if (SharedPrefs.getInstance().isDarkTheme()) {
            setTheme(R.style.Theme_CustomDarkTheme);
        } else {
            setTheme(R.style.Theme_CustomLightTheme);
        }
        setContentView(R.layout.activity_content_only);

        // Default result
        setResult(RESULT_CANCELED, null);

        s = new SelectFromListFragment(this, new InputExecutables());
        getFragmentManager().beginTransaction().replace(R.id.content_frame, s).commit();
    }

    @Override
    public void onExecutableSelected(String uid, int position) {
        Intent extra = AndroidShortcuts.createShortcutExecutionIntent(this, uid, false, false);
        s.getAdapterSource().getItem(position).getExecutable();
        setResult(RESULT_OK, AndroidShortcuts.createShortcut(extra, s.getAdapterSource().getItem(position).getExecutable().getTitle(), this));
        finish();
    }

    @Override
    public void onGroupSelected(String uid, int position) {

    }
}
