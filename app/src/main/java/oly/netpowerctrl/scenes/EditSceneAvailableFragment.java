package oly.netpowerctrl.scenes;

import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.device_ports.DevicePortsListFragment;
import oly.netpowerctrl.main.App;

/**
 */
public class EditSceneAvailableFragment extends DevicePortsListFragment {
    public EditSceneAvailableFragment() {
    }

    public void checkEmpty() {
        final View view = getView();
        assert view != null;

        // We assign the empty view after a short delay time,
        // to reduce visual flicker on activity start
        Handler h = App.getMainThreadHandler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                View v = mListView.getEmptyView();
                if (v != null)
                    v.setVisibility(View.GONE);
                mListView.setEmptyView(view.findViewById(R.id.empty));
                TextView textView = (TextView) view.findViewById(R.id.empty_text);
                textView.setText(R.string.scene_create_helptext_available);
                textView.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_media_rew, 0, 0, 0);
            }
        }, 200);
    }
}
