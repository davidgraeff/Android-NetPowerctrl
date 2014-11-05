package oly.netpowerctrl.scenes;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.executables.AdapterFragment;
import oly.netpowerctrl.executables.ExecutablesListAdapter;

/**
 */
public class EditSceneAvailableFragment extends AdapterFragment<ExecutablesListAdapter> {
    private RecyclerView.AdapterDataObserver adapterDataObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            View view = getView();
            if (view == null)
                return;
            view = view.findViewById(R.id.empty);
            if (mAdapter.getItemCount() != 0) {
                view.setVisibility(View.GONE);
                return;
            }

            view.setVisibility(View.VISIBLE);
            TextView textView = (TextView) view.findViewById(R.id.empty_text);
            textView.setText(R.string.scene_create_helptext_available);
            textView.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_media_rew, 0, 0, 0);
        }
    };

    public EditSceneAvailableFragment() {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAdapter != null)
            mAdapter.unregisterAdapterDataObserver(adapterDataObserver);
    }

    public void setAdapter(ExecutablesListAdapter adapter) {
        if (mAdapter != null)
            mAdapter.unregisterAdapterDataObserver(adapterDataObserver);

        super.setAdapter(adapter);
        mAdapter.registerAdapterDataObserver(adapterDataObserver);
    }
}
