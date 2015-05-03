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
import android.widget.Button;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.data.onServiceReady;
import oly.netpowerctrl.groups.Group;
import oly.netpowerctrl.groups.GroupAdapter;
import oly.netpowerctrl.groups.GroupUtilities;
import oly.netpowerctrl.ui.LineDividerDecoration;
import oly.netpowerctrl.ui.RecyclerItemClickListener;

;

/**
 * Try to setup all found devices, The dialog shows a short log about the actions.
 */
public class GroupListFragment extends Fragment implements onServiceReady {
    private DataService dataService = null;
    private GroupAdapter groupAdapter = new GroupAdapter();

    public GroupListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DataService.observersServiceReady.register(this);
    }

    @Override
    public boolean onServiceReady(DataService service) {
        dataService = service;
        return false;
    }

    @Override
    public void onServiceFinished(DataService service) {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_group_list, container, false);

        RecyclerView group_list = (RecyclerView) root.findViewById(R.id.group_list);
        group_list.setItemAnimator(new DefaultItemAnimator());
        group_list.setLayoutManager(new LinearLayoutManager(getActivity()));
        group_list.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public boolean onItemClick(View view, int position, boolean isLongClick) {
                OutletsFragment outletsFragment = (OutletsFragment) getFragmentManager().findFragmentByTag("outlets");
                if (outletsFragment == null) return true;

                Group group = groupAdapter.getGroup(position);
                if (isLongClick && position > 0) {
                    outletsFragment.showGroupPopupMenu(view, group.getUid());
                    return true;
                }
                outletsFragment.setGroup(position > 0 ? group.getUid() : null, true);
                ((MainActivity) getActivity()).closeGroupMenu();
                return true;
            }
        }, null));
        group_list.addItemDecoration(new LineDividerDecoration(getActivity(), LineDividerDecoration.VERTICAL_LIST) {
            @Override
            public boolean dividerForPosition(int position) {
                return true;
            }
        });
        group_list.setAdapter(groupAdapter);
        Button btnAdd = (Button) root.findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final OutletsFragment outletsFragment = (OutletsFragment) getFragmentManager().findFragmentByTag("outlets");
                if (outletsFragment == null) return;
                GroupUtilities.createGroup(getActivity(), dataService.groups, new GroupUtilities.GroupCreatedCallback() {
                    @Override
                    public void onGroupCreated(Group group) {
                        outletsFragment.setGroup(group.getUid(), true);
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
