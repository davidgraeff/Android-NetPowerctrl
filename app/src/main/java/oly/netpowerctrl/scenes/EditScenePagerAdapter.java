package oly.netpowerctrl.scenes;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.v13.app.FragmentPagerAdapter;
import android.view.ViewGroup;

import oly.netpowerctrl.R;

/**
 * This adapter is for the FragmentPager to show two OutletsFragments
 * (Available actions, scene included actions)
 */
class EditScenePagerAdapter extends FragmentPagerAdapter {
    private final Fragment[] frag;
    private final String[] texts;
    private EditSceneBasicFragment f0;
    private EditSceneIncludedFragment f1;
    private EditSceneAvailableFragment f2;

    public EditScenePagerAdapter(Context context, FragmentManager fm) {
        super(fm);

        f0 = (EditSceneBasicFragment) Fragment.instantiate(context, EditSceneBasicFragment.class.getName());
        f1 = (EditSceneIncludedFragment) Fragment.instantiate(context, EditSceneIncludedFragment.class.getName());
        f2 = (EditSceneAvailableFragment) Fragment.instantiate(context, EditSceneAvailableFragment.class.getName());
        frag = new Fragment[]{f0, f1, f2};
        texts = new String[]{
                context.getString(R.string.scene_create_basics),
                context.getString(R.string.scene_create_added),
                context.getString(R.string.scene_create_available)};
    }

    @Override
    public Fragment getItem(int i) {
        return frag[i];
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
    }

    @Override
    public int getCount() {
        return frag.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return texts[position];
    }

    public EditSceneIncludedFragment getFragmentIncluded() {
        return f1;
    }

    public EditSceneAvailableFragment getFragmentAvailable() {
        return f2;
    }

    public EditSceneBasicFragment getFragmentBasic() {
        return f0;
    }
}
