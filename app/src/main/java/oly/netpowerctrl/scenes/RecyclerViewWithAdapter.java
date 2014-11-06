package oly.netpowerctrl.scenes;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.AbsListView;
import android.widget.TextView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.ui.RecyclerItemClickListener;

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
        @Override
        public void onChanged() {
            super.onChanged();
            if (mAdapter.getItemCount() != 0) {
                mEmptyView.setVisibility(View.GONE);
                return;
            }

            mEmptyView.setVisibility(View.VISIBLE);
            TextView textView = (TextView) mEmptyView.findViewById(R.id.empty_text);
            textView.setText(mEmptyResText);
        }
    };

    boolean scrolled = false;

    public RecyclerViewWithAdapter(@NonNull Context context, @NonNull View parentView,
                                   @NonNull View view, @NonNull ADAPTER adapter, int emptyResText) {
        mParentView = (ViewParent) parentView;
        mRecyclerView = (RecyclerView) view.findViewById(android.R.id.list);
        mEmptyView = view.findViewById(R.id.empty);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        if (onItemClickListener != null)
            mRecyclerView.addOnItemTouchListener(onItemClickListener);
        mAdapter = adapter;
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.registerAdapterDataObserver(adapterDataObserver);
        this.mEmptyResText = emptyResText;

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
