package oly.netpowerctrl.main;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.groups.GroupAdapter;
import oly.netpowerctrl.groups.GroupUtilities;
import oly.netpowerctrl.pluginservice.PluginService;
import oly.netpowerctrl.pluginservice.onServiceReady;
import oly.netpowerctrl.ui.RecyclerItemClickListener;
import oly.netpowerctrl.ui.widgets.FloatingActionButton;
import oly.netpowerctrl.utils.DividerItemDecoration;

/**
 * Try to setup all found devices, The dialog shows a short log about the actions.
 */
public class GroupListFragment extends Fragment implements onServiceReady {
    private AppData appData = null;
    private GroupAdapter groupAdapter = new GroupAdapter();
    private FloatingActionButton btnAdd;

    public GroupListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PluginService.observersServiceReady.register(this);
    }

    @Override
    public boolean onServiceReady(PluginService service) {
        appData = service.getAppData();
        return false;
    }

    @Override
    public void onServiceFinished(PluginService service) {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.group_list, container, false);

        RecyclerView group_list = (RecyclerView) root.findViewById(R.id.group_list);
        group_list.setItemAnimator(new DefaultItemAnimator());
        group_list.setLayoutManager(new LinearLayoutManager(getActivity()));
        group_list.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public boolean onItemClick(View view, int position, boolean isLongClick) {
                OutletsFragment outletsFragment = (OutletsFragment) getFragmentManager().findFragmentByTag("outlets");
                if (outletsFragment == null) return true;

                if (isLongClick && position > 0) {
                    outletsFragment.showGroupPopupMenu(view, appData.groupCollection.get(position - 1).uuid);
                    return true;
                }
                outletsFragment.setGroup(position > 0 ? appData.groupCollection.get(position - 1).uuid : null, true);
                return true;
            }
        }, null));
        group_list.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST) {
            @Override
            public boolean dividerForPosition(int position) {
                return true;
            }
        });
        group_list.setAdapter(groupAdapter);
        btnAdd = (FloatingActionButton) root.findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final OutletsFragment outletsFragment = (OutletsFragment) getFragmentManager().findFragmentByTag("outlets");
                if (outletsFragment == null) return;
                GroupUtilities.createGroup(getActivity(), appData.groupCollection, new GroupUtilities.GroupCreatedCallback() {
                    @Override
                    public void onGroupCreated(int group_index, UUID group_uid) {
                        outletsFragment.setGroup(group_uid, true);
                    }
                });
            }
        });

        return root;
    }

    public GroupAdapter getAdapter() {
        return groupAdapter;
    }
}
