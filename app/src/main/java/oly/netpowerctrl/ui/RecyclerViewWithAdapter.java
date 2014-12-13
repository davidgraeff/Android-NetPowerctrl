package oly.netpowerctrl.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.AbsListView;
import android.widget.TextView;

import oly.netpowerctrl.R;

/**
 * A recyclerview that has uses the given adapter
 */
public class RecyclerViewWithAdapter<ADAPTER extends RecyclerView.Adapter> {
    protected ADAPTER mAdapter;
    protected RecyclerView mRecyclerView;
    protected RecyclerItemClickListener onItemClickListener;
    protected View mEmptyView;
    protected ViewParent mParentView;
    protected int mEmptyResText;

    private RecyclerView.AdapterDataObserver adapterDataObserver = new RecyclerView.AdapterDataObserver() {
        private void changed() {
            if (mAdapter.getItemCount() != 0 || mEmptyResText == 0) {
                mEmptyView.setVisibility(View.GONE);
                return;
            }

            mEmptyView.setVisibility(View.VISIBLE);
            TextView textView = (TextView) mEmptyView.findViewById(R.id.empty_text);
            textView.setText(mEmptyResText);
        }

        @Override
        public void onChanged() {
            super.onChanged();
            changed();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            super.onItemRangeChanged(positionStart, itemCount);
            changed();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            super.onItemRangeRemoved(positionStart, itemCount);
            changed();
        }
    };

    boolean scrolled = false;

    /**
     * Create an object of this class in the onCreateView method of your fragment. You can access
     * the recyclerView and adapter by the access methods {@link #getRecyclerView()} and
     * {@link #getAdapter()}. The rootView has to contain a RecyclerView with the id android.R.id.list
     * and an container for the empty text with the id android.R.id.empty. The empty text itself (if used)
     * have to have the id R.id.empty_text.
     *
     * @param context      A context
     * @param parentView   The parent view. Its touch handlers will be disabled while scrolling within the
     *                     recyclerView. May be null.
     * @param rootView     The root view
     * @param adapter      The adapter
     * @param emptyResText The text resource for the empty text that will be shown if the adapter count is 0.
     *                     May be 0.
     */
    public RecyclerViewWithAdapter(@NonNull Context context, @Nullable ViewParent parentView,
                                   @NonNull View rootView, ADAPTER adapter, int emptyResText) {
        mParentView = parentView;
        mRecyclerView = (RecyclerView) rootView.findViewById(android.R.id.list);
        mEmptyView = rootView.findViewById(android.R.id.empty);
        mAdapter = adapter;
        if (mRecyclerView == null || mEmptyView == null)
            throw new RuntimeException("RecyclerViewWithAdapter: rootView does not contain list or empty id!");
        if (mAdapter == null)
            throw new RuntimeException("RecyclerViewWithAdapter: adapter not set!");

        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        if (onItemClickListener != null)
            mRecyclerView.addOnItemTouchListener(onItemClickListener);
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.registerAdapterDataObserver(adapterDataObserver);
        this.mEmptyResText = emptyResText;

        adapterDataObserver.onChanged();

        if (parentView != null) {
            mRecyclerView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    int action = event.getAction();
                    switch (action) {
                        case MotionEvent.ACTION_DOWN:
                            // Disallow ScrollView to intercept touch events.
                            mParentView.requestDisallowInterceptTouchEvent(true);
                            break;

                        case MotionEvent.ACTION_UP:
                            scrolled = false;
                            // Allow ScrollView to intercept touch events.
                            mParentView.requestDisallowInterceptTouchEvent(false);
                            break;
                    }

                    // Handle ListView touch events.
                    v.onTouchEvent(event);
                    return true;
                }
            });
            mRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL && !scrolled) {
                        mParentView.requestDisallowInterceptTouchEvent(false);
//                    MotionEvent cancelEvent = MotionEvent.obtain(last_event);
//                    ((View)recyclerView.getParent()).onTouchEvent(cancelEvent);
//                    cancelEvent.recycle();
                    }
                }

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    scrolled = true;
                }
            });
        }
    }

    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    public ADAPTER getAdapter() {
        return mAdapter;
    }

    public void setOnItemClickListener(@NonNull RecyclerItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
        mRecyclerView.addOnItemTouchListener(onItemClickListener);
    }

    public void onDestroy() {
        if (mAdapter != null)
            mAdapter.unregisterAdapterDataObserver(adapterDataObserver);
    }
}
