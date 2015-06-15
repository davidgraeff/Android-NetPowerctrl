package oly.netpowerctrl.groups;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.data.onServiceReady;
import oly.netpowerctrl.executables.ExecutablesFragment;
import oly.netpowerctrl.ioconnection.IOConnectionsFragment;
import oly.netpowerctrl.main.BuyFragment;
import oly.netpowerctrl.main.FeedbackFragment;
import oly.netpowerctrl.main.MainActivity;
import oly.netpowerctrl.preferences.PreferencesFragment;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.ui.EmptyListener;
import oly.netpowerctrl.ui.FragmentUtils;
import oly.netpowerctrl.ui.LineDividerDecoration;
import oly.netpowerctrl.ui.RecyclerItemClickListener;

;

/**
 * Try to setup all found devices, The dialog shows a short log about the actions.
 */
public class GroupListFragment extends Fragment implements onServiceReady, EmptyListener {
    private DataService dataService = null;
    private GroupAdapter groupAdapter = new GroupAdapter(true, this);
    private View group_help;

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

        group_help = root.findViewById(R.id.group_help);

        RecyclerView group_list = (RecyclerView) root.findViewById(R.id.group_list);
        group_list.setItemAnimator(new DefaultItemAnimator());
        group_list.setLayoutManager(new LinearLayoutManager(getActivity()));
        group_list.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public boolean onItemClick(View view, int position, boolean isLongClick) {
                Group group = groupAdapter.getGroup(position);

                ExecutablesFragment executablesFragment = (ExecutablesFragment) getFragmentManager().findFragmentByTag("outlets");
                if (executablesFragment == null) {
                    SharedPrefs.getInstance().setLastGroupUID(position > 0 ? group.getUid() : null);
                    FragmentUtils.changeToFragment(getActivity(), ExecutablesFragment.class.getName(), "outlets", null);
                    return true;
                }
                if (!executablesFragment.isAdded())
                    getFragmentManager().popBackStack("outlets", 0);

                if (isLongClick && position > 0) {
                    executablesFragment.showGroupPopupMenu(view, group.getUid());
                    return true;
                }
                executablesFragment.setGroup(position > 0 ? group.getUid() : null, true);
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

        groupAdapter.start();

        View button = root.findViewById(R.id.btnAdd);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final ExecutablesFragment executablesFragment = (ExecutablesFragment) getFragmentManager().findFragmentByTag("outlets");
                if (executablesFragment == null) return;
                GroupUtilities.createGroup(getActivity(), dataService.groups, new GroupUtilities.GroupCreatedCallback() {
                    @Override
                    public void onGroupCreated(Group group) {
                        executablesFragment.setGroup(group.getUid(), true);
                    }
                });
            }
        });

        button = root.findViewById(R.id.btnDevices);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentUtils.changeToFragment(getActivity(), IOConnectionsFragment.class.getName());
                ((MainActivity) getActivity()).closeGroupMenu();
            }
        });

        button = root.findViewById(R.id.btnPreferences);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentUtils.changeToFragment(getActivity(), PreferencesFragment.class.getName());
                ((MainActivity) getActivity()).closeGroupMenu();
            }
        });

        button = root.findViewById(R.id.btnBuy);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentUtils.changeToFragment(getActivity(), BuyFragment.class.getName());
                ((MainActivity) getActivity()).closeGroupMenu();
            }
        });

        button = root.findViewById(R.id.btnFeedback);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentUtils.changeToFragment(getActivity(), FeedbackFragment.class.getName());
                ((MainActivity) getActivity()).closeGroupMenu();
            }
        });

        return root;
    }

    public GroupAdapter getAdapter() {
        return groupAdapter;
    }

    @Override
    public void onEmptyListener(boolean empty) {
        group_help.setVisibility(groupAdapter.getItemCount() > 0 ? View.GONE : View.VISIBLE);
    }
}
