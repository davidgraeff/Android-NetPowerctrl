package oly.netpowerctrl.widget;

import android.app.Activity;
import android.app.Fragment;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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

public class WidgetConfigActivity extends Activity {
    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    RecyclerItemClickListener recyclerItemClickListener =
            new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {
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
                            setResult(RESULT_OK, resultValue);
                            finish();
                            WidgetUpdateService.ForceUpdate(WidgetConfigActivity.this, widgetId);
                        }
                    });
                    return true;
                }
            }, null);
    private ExecutablesAdapter adapter;
    private AdapterSource adapterSource;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        // Set theme, call super onCreate and set content view
        if (SharedPrefs.getInstance().isDarkTheme()) {
            setTheme(R.style.Theme_CustomDarkTheme);
        } else {
            setTheme(R.style.Theme_CustomLightTheme);
        }
        setContentView(R.layout.activity_main_one_pane);

        // Disable nav drawer and add fake adapter to list (RecyclerView crashes!)
        ((DrawerLayout) findViewById(R.id.drawer_layout)).setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.left_drawer_list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(new RecyclerView.Adapter() {
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
                return null;
            }

            public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
            }

            public int getItemCount() {
                return 0;
            }
        });
        findViewById(R.id.left_drawer_list).setVisibility(View.GONE);
        findViewById(R.id.toolbar_actionbar).setVisibility(View.GONE);

        adapterSource = new AdapterSource(AdapterSource.AutoStartEnum.AutoStartAfterFirstQuery);
        adapterSource.setShowHeaders(false);
        adapterSource.addInput(new AdapterSourceInputDevicePorts(), new AdapterSourceInputScenes());
        adapter = new ExecutablesAdapter(adapterSource, LoadStoreIconData.iconLoadingThread, R.layout.list_item_available_outlet);

        Fragment fragment = new Fragment() {
            @Nullable
            @Override
            public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
                View rootView = inflater.inflate(R.layout.fragment_with_list, container, false);
                RecyclerViewWithAdapter<ExecutablesAdapter> recyclerViewWithAdapter =
                        new RecyclerViewWithAdapter<>(getActivity(), null, rootView, adapter, 0);
                recyclerViewWithAdapter.setOnItemClickListener(recyclerItemClickListener);
                return rootView;
            }
        };

        getFragmentManager().beginTransaction().replace(R.id.content_frame, fragment).commit();

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            widgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
    }
}
