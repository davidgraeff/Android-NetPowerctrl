package oly.netpowerctrl.scenes;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;

/**
 * This adapter is for the FragmentPager to show two OutletsFragments
 * (Available actions, scene included actions)
 */
class EditScenePagerAdapter extends FragmentPagerAdapter {
    private final EditSceneFragment[] frag;

    public EditScenePagerAdapter(FragmentManager fm) {
        super(fm);

        EditSceneFragment f1 = new EditSceneFragment();
        EditSceneFragment f2 = new EditSceneFragment();
        frag = new EditSceneFragment[]{f1, f2};
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

    public EditSceneFragment getFragmentIncluded() {
        return frag[0];
    }

    public EditSceneFragment getFragmentAvailable() {
        return frag[1];
    }
}
