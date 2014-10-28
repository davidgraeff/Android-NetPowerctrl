package oly.netpowerctrl.outletsview;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentStatePagerAdapter;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.groups.Group;
import oly.netpowerctrl.groups.GroupCollection;
import oly.netpowerctrl.main.App;

/**
 *
 */
public class GroupPagerAdapter extends FragmentStatePagerAdapter implements onCollectionUpdated<GroupCollection, Group> {
    private int count;

    public GroupPagerAdapter(FragmentManager fm) {
        super(fm);

        count = AppData.getInstance().groupCollection.size() + 1;
        AppData.getInstance().groupCollection.registerObserver(this);
    }

    public void onDestroy() {
        AppData.getInstance().groupCollection.unregisterObserver(this);
    }

    @Override
    public boolean updated(GroupCollection groupCollection, Group group, ObserverUpdateActions action, int position) {
        count = AppData.getInstance().groupCollection.size() + 1;
        notifyDataSetChanged();
        return true;
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public Fragment getItem(int position) {
        return new OutletsViewFragment();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return position == 0 ? App.getAppString(R.string.drawer_overview) :
                AppData.getInstance().groupCollection.get(position - 1).name;
    }
}
