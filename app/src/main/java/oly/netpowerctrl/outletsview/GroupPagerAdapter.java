package oly.netpowerctrl.outletsview;

import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.View;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.data.onDataLoaded;
import oly.netpowerctrl.groups.Group;
import oly.netpowerctrl.groups.GroupCollection;
import oly.netpowerctrl.main.App;

/**
 *
 */
public class GroupPagerAdapter extends PagerAdapter implements onCollectionUpdated<GroupCollection, Group> {
    private int count;

    public GroupPagerAdapter() {
        count = 0;
        AppData.observersOnDataLoaded.register(new onDataLoaded() {
            @Override
            public boolean onDataLoaded() {
                count = AppData.getInstance().groupCollection.size() + 1;
                notifyDataSetChanged();
                return false;
            }
        });
        AppData.getInstance().groupCollection.registerObserver(this);
    }

    public void onDestroy() {
        AppData.getInstance().groupCollection.unregisterObserver(this);
    }

    @Override
    public boolean updated(@NonNull GroupCollection groupCollection, Group group, @NonNull ObserverUpdateActions action, int position) {
        count = AppData.getInstance().groupCollection.size() + 1;
        notifyDataSetChanged();
        return true;
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public boolean isViewFromObject(View view, Object o) {
        return false;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return position == 0 ? App.getAppString(R.string.drawer_overview) :
                AppData.getInstance().groupCollection.get(position - 1).name;
    }
}
