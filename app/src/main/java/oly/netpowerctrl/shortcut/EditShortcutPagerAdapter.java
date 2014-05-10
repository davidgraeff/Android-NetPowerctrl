package oly.netpowerctrl.shortcut;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;

/**
 * This adapter is for the FragmentPager to show two OutletsFragments
 * (Available actions, scene included actions)
 */
class EditShortcutPagerAdapter extends FragmentPagerAdapter {
    private final SceneEditFragment[] frag;

    public EditShortcutPagerAdapter(FragmentManager fm) {
        super(fm);

        SceneEditFragment f1 = new SceneEditFragment();
        SceneEditFragment f2 = new SceneEditFragment();
        frag = new SceneEditFragment[]{f1, f2};
    }

    @Override
    public Fragment getItem(int i) {
        return frag[i];
    }

    @Override
    public int getCount() {
        return frag.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return NetpowerctrlApplication.instance.getString(R.string.scene_create_added);
            case 1:
                return NetpowerctrlApplication.instance.getString(R.string.scene_create_available);
        }
        return "";
    }

    public SceneEditFragment getFragmentIncluded() {
        return frag[0];
    }

    public SceneEditFragment getFragmentAvailable() {
        return frag[1];
    }
}
