package oly.netpowerctrl.backup.neighbours;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.network.UDPSending;

/**
 * Neighbour discovery is activated if this fragment is on screen.
 */
public class NeighbourFragment extends Fragment implements PopupMenu.OnMenuItemClickListener, NeighbourDataSync.NeighbourDataCommunication {
    public static final String TAG = "NeighbourFragment";

    public NeighbourAdapter neighbourAdapter;
    private final Runnable advanceTimeRunnable = new Runnable() {
        @Override
        public void run() {
            if (neighbourAdapter == null)
                return;

            neighbourAdapter.advanceTime();

            NetpowerctrlApplication.getMainThreadHandler().postDelayed(this, 5000);
        }
    };
    int clickedPosition = 0;
    private UDPSending udpSending;
    private NeighbourDiscoverReceiving udpReceiving;
    private UDPSending.SendRawJob broadcastSendJob;
    private boolean discoverIsRunning = false;
    private final Runnable sendDiscoverMessageRunnable = new Runnable() {
        @Override
        public void run() {
            if (broadcastSendJob == null)
                return;

            discoverIsRunning = true;

            Log.w(TAG, "send");
            udpSending.addJob(broadcastSendJob);

            NetpowerctrlApplication.getMainThreadHandler().postDelayed(this, 1500);
        }
    };
    private TextView networkIDText;
    private int lastGateway = 0;
    private BroadcastReceiver wifiChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkNetworkState();
        }
    };

    public NeighbourFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

    }

    @Override
    public void onStart() {
        super.onStart();
        neighbourAdapter = new NeighbourAdapter(getActivity());
    }

    @Override
    public void onStop() {
        neighbourAdapter = null;
        super.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (udpSending != null) {
            udpSending.interrupt();
            udpSending = null;
        }
        if (udpReceiving != null) {
            if (udpReceiving.isAlive())
                udpReceiving.interrupt();
            udpReceiving = null;
        }

        NetpowerctrlApplication.getMainThreadHandler().removeCallbacks(advanceTimeRunnable);
        NetpowerctrlApplication.getMainThreadHandler().removeCallbacks(sendDiscoverMessageRunnable);

        // Stop tcp receiver
        NeighbourDataReceiveService.stop();

        try {
            getActivity().unregisterReceiver(wifiChanged);
        } catch (IllegalArgumentException ignored) {
        }
        wifiChanged = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        udpSending = new UDPSending(true);
        udpSending.start();
        udpReceiving = new NeighbourDiscoverReceiving(this);
        udpReceiving.start();
        advanceTimeRunnable.run();
        // Reset icon size cache, will be set on next sending
        NeighbourDiscoverSending.icon_size_cache = -1;
        checkNetworkState();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        //noinspection ConstantConditions
        getActivity().registerReceiver(wifiChanged, intentFilter);
    }

//    private void enableSync(boolean enable) {
//        if (enable) {
//            SharedPrefs.setNeighbourAutoSync(true);
//            NeighbourDataReceiveService.startAutoSync();
//        } else {
//            SharedPrefs.setNeighbourAutoSync(false);
//        }
//    }

    private void checkNetworkState() {
        int g = updateNetworkIDText();
        if (lastGateway == g) {
            NeighbourDataReceiveService.stop();
            return;
        }
        lastGateway = g;
        broadcastSendJob = NeighbourDiscoverSending.createDiscoverMessage();
        if (!discoverIsRunning)
            sendDiscoverMessageRunnable.run();

        // Start tcp receiver
        NeighbourDataReceiveService.start(this);
    }

    public void syncTimer() {
        NetpowerctrlApplication.getMainThreadHandler().removeCallbacks(sendDiscoverMessageRunnable);
        // The activity may be hidden at this point, so check if the relevant objects still exist.
        if (!discoverIsRunning && udpSending != null && broadcastSendJob != null)
            sendDiscoverMessageRunnable.run();
    }

    public void pairResult(final NeighbourAdapter.AdapterItem item, boolean accepted) {
        if (accepted) {
            Toast.makeText(getActivity(), R.string.neighbour_pairing_accepted, Toast.LENGTH_SHORT).show();
            neighbourAdapter.setPaired(item, true);
        } else {
            Toast.makeText(getActivity(), R.string.neighbour_pairing_denied, Toast.LENGTH_SHORT).show();
        }
    }

    public void askForPairing(final NeighbourAdapter.AdapterItem item) {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        alert.setTitle(R.string.neighbour_pairing_title);
        alert.setMessage(getString(R.string.neighbour_pairing_message, item.getName()));
        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                NeighbourDiscoverSending.sendPairAckMessage(udpSending, true, item.address);
                neighbourAdapter.setPaired(item, true);
            }
        });
        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                NeighbourDiscoverSending.sendPairAckMessage(udpSending, false, item.address);
            }
        });
        alert.show();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.only_help, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_help: {
                //noinspection ConstantConditions
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.menu_help)
                        .setMessage(R.string.help_neighbours)
                        .setIcon(android.R.drawable.ic_menu_help).show();
                return true;
            }
        }
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_neighbours, container, false);

        //noinspection ConstantConditions
        ListView list = (ListView) view.findViewById(R.id.list);
        networkIDText = (TextView) view.findViewById(R.id.hintText);
        updateNetworkIDText();
        list.setEmptyView(view.findViewById(android.R.id.empty));
        list.setAdapter(neighbourAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                final NeighbourAdapter.AdapterItem item = neighbourAdapter.getNeighbour(position);
                if (item.isPaired) {
                    clickedPosition = position;
                    PopupMenu popup = new PopupMenu(getActivity(), view);
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.neighbour_entry, popup.getMenu());
                    popup.getMenu().findItem(R.id.menu_neighbour_push).setEnabled(item.isOnline && item.isSameVersion);
                    popup.getMenu().findItem(R.id.menu_neighbour_remove).setEnabled(item.isOnline);
                    popup.setOnMenuItemClickListener(NeighbourFragment.this);
                    popup.show();
                    return;
                }

                if (!item.isSameVersion) {
                    //noinspection ConstantConditions
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.error_neighbour_wrong_version_title)
                            .setMessage(R.string.error_neighbour_wrong_version)
                            .setIcon(android.R.drawable.ic_menu_help)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    sendPairingRequest(item);
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            })
                            .show();
                    //noinspection ConstantConditions
                    Toast.makeText(getActivity(), R.string.neighbour_not_same_version, Toast.LENGTH_SHORT).show();
                    return;
                }

                sendPairingRequest(item);

            }
        });

        return view;
    }

    private void sendPairingRequest(NeighbourAdapter.AdapterItem item) {
        item.pairingRequest = true;
        NeighbourDiscoverSending.sendPairRequestMessage(udpSending, item.address);
        //noinspection ConstantConditions
        Toast.makeText(getActivity(), R.string.neighbour_pairing_request, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        final NeighbourAdapter.AdapterItem item = neighbourAdapter.getItem(clickedPosition);

        switch (menuItem.getItemId()) {
            case R.id.menu_neighbour_remove: {
                NeighbourDataSync.sendData(item.address, item.uniqueID, this, true);
                return true;
            }
            case R.id.menu_neighbour_push: {
                NeighbourDataSync.sendData(item.address, item.uniqueID, this, false);
                return true;
            }
        }
        return false;
    }

    private int updateNetworkIDText() {
        //noinspection ConstantConditions
        WifiManager wifi = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
        int gateway = (wifi != null) ? wifi.getDhcpInfo().gateway : 0;
        String networkID = (gateway != 0) ? String.valueOf(gateway) : "n/a";

        //noinspection ConstantConditions
        networkIDText.setText(getActivity().getString(R.string.neighbours_hint_activated, networkID));
        return gateway;
    }

    @Override
    public void pairingRemoved(long uniqueID) {
        neighbourAdapter.removePairing(uniqueID);
    }

    @Override
    public void pairingRemoveDenied(long uniqueID) {
        Toast.makeText(getActivity(), R.string.neighbour_pairing_removal_denied, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void dataReceived(long uniqueID) {
        NeighbourAdapter.AdapterItem item = neighbourAdapter.getItemByID(uniqueID);
        if (item == null) return;
        Toast.makeText(getActivity(), getString(R.string.neighbour_data_received, item.getName()),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void dataProgress(long uniqueID, String progress) {
        if (progress != null)
            networkIDText.setText(progress);
        else
            updateNetworkIDText();
    }
}
