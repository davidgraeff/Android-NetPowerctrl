package oly.netpowerctrl.outletsview;

import android.app.Fragment;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.IconCacheCleared;
import oly.netpowerctrl.data.LoadStoreIconData;

/**
 */
public class OutletsViewFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, IconCacheCleared {
    private OutletsViewContainerFragment container;
    private RecyclerView mRecyclerView;
    private TextView placeHolderText;
    private SwipeRefreshLayout mPullToRefreshLayout;
    private View placeHolderLayout;
    private int resIdWhileNotAdded = -1;

    public OutletsViewFragment() {
        container = null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (mRecyclerView != null && container != null) {
            mRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(mListViewNumColumnsChangeListener);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup viewGroup,
                             Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_outlets_content, viewGroup, false);
        assert view != null;
        mRecyclerView = (RecyclerView) view.findViewById(android.R.id.list);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        ///// For pull to refresh
        mPullToRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.list_layout);
        mPullToRefreshLayout.setOnRefreshListener(this);
        mPullToRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        ///// END: For pull to refresh

        placeHolderText = (TextView) view.findViewById(R.id.empty_text);
        placeHolderLayout = view.findViewById(R.id.empty_layout);

        // Add a decorator to the list to add additional space at the bottom.
        // This is necessary to not obscure elements at the end of the list with the floating
        // buttons.
        RecyclerView.ItemDecoration lastItemWithBottomMargin = new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                int pos = ((RecyclerView.LayoutParams) view.getLayoutParams()).getViewPosition();
                int max = container.getAdapter().getItemCount() - 1;
                if (max > 4 && pos == max) {
                    outRect.set(0, 0, 0, container.spaceBottomForFloatingButtons);
                    return;
                }
                super.getItemOffsets(outRect, view, parent, state);
            }
        };

        mRecyclerView.addItemDecoration(lastItemWithBottomMargin);
        return view;
    }

    public void onRefreshStateChanged(boolean isRefreshing) {
        if (mPullToRefreshLayout != null)
            mPullToRefreshLayout.setRefreshing(isRefreshing);
    }

    private final ViewTreeObserver.OnGlobalLayoutListener mListViewNumColumnsChangeListener =
            new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    //noinspection deprecation
                    mRecyclerView.getViewTreeObserver().removeGlobalOnLayoutListener(mListViewNumColumnsChangeListener);

                    //getActivity().findViewById(R.id.content_frame).getWidth();
                    //Log.w("width", String.valueOf(mListView.getMeasuredWidth()));
                    int i = mRecyclerView.getWidth() / container.requestedColumnWidth;
                    if (i < 1) i = 1;
                    container.getAdapter().setItemsInRow(i);
//                    SpannableGridLayoutManager spannableGridLayoutManager = new SpannableGridLayoutManager(getActivity());
//                    spannableGridLayoutManager.setNumColumns(i);
//                    spannableGridLayoutManager.setNumRows(1);
                    GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), i);
                    gridLayoutManager.setSpanSizeLookup(container.getAdapter().getSpanSizeLookup());
                    mRecyclerView.setHasFixedSize(false);
                    mRecyclerView.setLayoutManager(gridLayoutManager);
                    mRecyclerView.setAdapter(container.getAdapter());

                }
            };

    @Override
    public void onRefresh() {
        if (container != null)
            container.refreshNow();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (resIdWhileNotAdded != -1) {
            setEmptyText(container, resIdWhileNotAdded);
            resIdWhileNotAdded = -1;
        } else if (container == null)
            setEmptyText(null, R.string.empty_no_outlets);
    }

    public void setEmptyText(OutletsViewContainerFragment container, int resId) {
        this.container = container;
        if (!isAdded() || mRecyclerView == null) {
            resIdWhileNotAdded = resId;
            return;
        }

        if (resId == 0 || resIdWhileNotAdded != -1) {
            mRecyclerView.addOnItemTouchListener(container.onItemTouchListener);
            mRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(mListViewNumColumnsChangeListener);
            LoadStoreIconData.iconCacheClearedObserver.register(this);
        }

        if (resId != 0) {
            placeHolderText.setText(resId);
            placeHolderLayout.setVisibility(View.VISIBLE);
            mPullToRefreshLayout.setVisibility(View.GONE);
        } else {
            placeHolderLayout.setVisibility(View.GONE);
            mPullToRefreshLayout.setVisibility(View.VISIBLE);
        }
    }

    public void viewTypeChanged(OutletsViewContainerFragment container) {
        this.container = container;
        if (container != null && mRecyclerView != null)
            mRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(mListViewNumColumnsChangeListener);
    }

    @Override
    public void onIconCacheCleared() {
        if (mRecyclerView != null && container != null)
            mRecyclerView.setAdapter(container.getAdapter());
    }


}
