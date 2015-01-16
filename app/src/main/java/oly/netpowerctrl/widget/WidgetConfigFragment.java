package oly.netpowerctrl.widget;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.executables.AdapterSource;
import oly.netpowerctrl.executables.AdapterSourceInputDevicePorts;
import oly.netpowerctrl.executables.AdapterSourceInputScenes;
import oly.netpowerctrl.executables.ExecutablesAdapter;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.ui.RecyclerItemClickListener;
import oly.netpowerctrl.ui.RecyclerViewWithAdapter;

/**
* Created by david on 15.01.15.
*/
public class WidgetConfigFragment extends Fragment implements RecyclerItemClickListener.OnItemClickListener {
    private ExecutablesAdapter adapter;
    private AdapterSource adapterSource;
    private final int widgetId;

    public WidgetConfigFragment() {widgetId= AppWidgetManager.INVALID_APPWIDGET_ID;}

    @SuppressLint("ValidFragment")
    WidgetConfigFragment(int widgetId) {
        this.widgetId = widgetId;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        adapterSource = new AdapterSource(AdapterSource.AutoStartEnum.AutoStartOnServiceReady);
        adapterSource.setShowHeaders(false);
        adapterSource.addInput(new AdapterSourceInputDevicePorts(), new AdapterSourceInputScenes());
        adapter = new ExecutablesAdapter(adapterSource, LoadStoreIconData.iconLoadingThread, R.layout.list_item_available_outlet);

        View rootView = inflater.inflate(R.layout.fragment_with_list, container, false);
        RecyclerViewWithAdapter<ExecutablesAdapter> recyclerViewWithAdapter =
                new RecyclerViewWithAdapter<>(getActivity(), null, rootView, adapter, 0);
        recyclerViewWithAdapter.setOnItemClickListener(new RecyclerItemClickListener(getActivity(), this, null));
        return rootView;
    }

    @Override
    public boolean onItemClick(View view, int position, boolean isLongClick) {
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID)
            return false;
        String uid = adapterSource.getItem(position).getExecutableUid();
        if (uid == null)
            return false;

        SharedPrefs.getInstance().SaveWidget(widgetId, uid);

        App.getMainThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
                getActivity().setResult(Activity.RESULT_OK, resultValue);
                getActivity().finish();
                WidgetUpdateService.ForceUpdate(getActivity(), widgetId);
            }
        });
        return true;
    }
}
