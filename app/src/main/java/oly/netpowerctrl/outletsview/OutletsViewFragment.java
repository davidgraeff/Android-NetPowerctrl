package oly.netpowerctrl.outletsview;

import android.annotation.SuppressLint;
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
import oly.netpowerctrl.groups.Group;

/**
 */
public class OutletsViewFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private final OutletsViewContainerFragment container;
    private RecyclerView mRecyclerView;
    private TextView placeHolderText;
    private SwipeRefreshLayout mPullToRefreshLayout;
    private Group group;
    private View placeHolderLayout;
    private int resIdWhileNotAdded = -1;

    public OutletsViewFragment() {
        container = null;
    }

    @SuppressLint("ValidFragment")
    public OutletsViewFragment(OutletsViewContainerFragment container, Group group) {
        this.container = container;
        this.group = group;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (mRecyclerView != null) {
            mRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(mListViewNumColumnsChangeListener);
        }
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

        mRecyclerView.addOnItemTouchListener(container.onItemTouchListener);
        return view;
    }

    public void onRefreshStateChanged(boolean isRefreshing) {
        mPullToRefreshLayout.setRefreshing(isRefreshing);
    }

    @Override
    public void onRefresh() {
        container.refreshNow();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (resIdWhileNotAdded != -1) {
            setEmptyText(resIdWhileNotAdded);
            resIdWhileNotAdded = -1;
        }
    }

    public void setEmptyText(int resId) {
        if (!isAdded()) {
            resIdWhileNotAdded = resId;
            return;
        }

        if (resId != 0) {
            placeHolderText.setText(resId);
            placeHolderLayout.setVisibility(View.VISIBLE);
            mPullToRefreshLayout.setVisibility(View.GONE);
        } else {
            placeHolderLayout.setVisibility(View.GONE);
            mPullToRefreshLayout.setVisibility(View.VISIBLE);
            mRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(mListViewNumColumnsChangeListener);
        }
    }

    public void viewTypeChanged() {
        mRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(mListViewNumColumnsChangeListener);
    }


}
