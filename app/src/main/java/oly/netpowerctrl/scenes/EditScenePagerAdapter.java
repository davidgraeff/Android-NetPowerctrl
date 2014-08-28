package oly.netpowerctrl.scenes;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.v13.app.FragmentPagerAdapter;

import oly.netpowerctrl.R;

/**
 * This adapter is for the FragmentPager to show two OutletsFragments
 * (Available actions, scene included actions)
 */
class EditScenePagerAdapter extends FragmentPagerAdapter {
    private EditSceneFragment[] frag;
    private String[] texts;

    public EditScenePagerAdapter(Context context, FragmentManager fm) {
        super(fm);

        EditSceneFragment f1 = new EditSceneFragment();
        EditSceneFragment f2 = new EditSceneFragment();
        frag = new EditSceneFragment[]{f1, f2};
        texts = new String[]{context.getString(R.string.scene_create_added), context.getString(R.string.scene_create_available)};
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
        return texts[position];
    }

    public EditSceneFragment getFragmentIncluded() {
        return frag[0];
    }

    public EditSceneFragment getFragmentAvailable() {
        return frag[1];
    }
}
