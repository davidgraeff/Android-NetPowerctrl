package oly.netpowerctrl.utils;

import android.app.Fragment;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.TextView;

import oly.netpowerctrl.R;

/**
 * This fragment consists of a ListView/Gridview and a text that is shown
 * if the ListView/GridView adapter is of length 0.
 */
public class GridOrListFragment extends Fragment {
    protected AbsListView mListView;
    protected TextView emptyText;

    private class DataSetObserverMy extends DataSetObserver {
        private GridOrListFragment sf;

        DataSetObserverMy(GridOrListFragment sf) {
            this.sf = sf;
        }

        @Override
        public void onChanged() {
            sf.checkDataAvailable();
        }

        @Override
        public void onInvalidated() {
            sf.checkDataAvailable();
        }
    }

    private DataSetObserver testObserver = new DataSetObserverMy(this);

    private void checkDataAvailable() {
        emptyText.setVisibility((mListView.getAdapter().getCount() == 0) ? View.VISIBLE : View.GONE);
    }

    /**
     * If no data is available in the listview adapter, a text may present
     * additional information to the user. If auto checking of new data
     * is not enabled, the text may not appear or disappear.
     * Call this in onCreateView after you have set the mListView adapter
     *
     * @param enabled Enable or disable auto checking of listview changes
     */
    protected void setAutoCheckDataAvailable(boolean enabled) {
        if (enabled)
            mListView.getAdapter().registerDataSetObserver(testObserver);
        else
            mListView.getAdapter().unregisterDataSetObserver(testObserver);
        checkDataAvailable();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_item, container, false);
        assert view != null;
        emptyText = (TextView) view.findViewById(android.R.id.empty);
        mListView = (AbsListView) view.findViewById(android.R.id.list);
        return view;
    }

    @Override
    public void onDestroyView() {
        mListView.getAdapter().unregisterDataSetObserver(testObserver);
        super.onDestroyView();
    }
}
