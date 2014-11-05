package oly.netpowerctrl.scenes;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.executables.AdapterFragment;
import oly.netpowerctrl.executables.ExecutablesListAdapter;

/**
 */
public class EditSceneIncludedFragment extends AdapterFragment<SceneElementsAdapter> {
    private boolean isTwoPaneFragment;
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
            if (isTwoPaneFragment)
                textView.setText(R.string.scene_create_include_twopane);
            else {
                textView.setText(R.string.scene_create_include_onepane);
                textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_media_ff, 0);
            }
        }
    };
    private ExecutablesListAdapter adapter_available;

    public EditSceneIncludedFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAdapter != null)
            mAdapter.unregisterAdapterDataObserver(adapterDataObserver);
    }

    public void setAdapter(SceneElementsAdapter adapter) {
        if (mAdapter != null)
            mAdapter.unregisterAdapterDataObserver(adapterDataObserver);

        super.setAdapter(adapter);
        mAdapter.registerAdapterDataObserver(adapterDataObserver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mAdapter != null && mAdapter.getItemCount() > 0)
            inflater.inflate(R.menu.scene_included_actions, menu);
    }

    public void setTwoPaneFragment(boolean isTwoPaneFragment) {
        this.isTwoPaneFragment = isTwoPaneFragment;
    }

    public void setAdapterAvailable(ExecutablesListAdapter adapter_available) {
        this.adapter_available = adapter_available;
    }

    /*
     * ActionBar icon clicked
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        SceneElementsAdapter adapter_included = mAdapter;
        switch (item.getItemId()) {
            case R.id.menu_switch_all_on:
                adapter_included.switchAllOn();
                return true;
            case R.id.menu_switch_all_off:
                adapter_included.switchAllOff();
                return true;
            case R.id.menu_switch_all_toogle:
                adapter_included.toggleAll();
                return true;
            case R.id.menu_switch_all_ignore:
                adapter_included.clear();
                adapter_available.getSource().updateNow();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
