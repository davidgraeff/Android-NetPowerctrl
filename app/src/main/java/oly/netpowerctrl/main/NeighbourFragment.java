package oly.netpowerctrl.main;

import android.app.ActionBar;
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
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.application_state.RuntimeDataController;
import oly.netpowerctrl.network.UDPSending;
import oly.netpowerctrl.network.Utils;
import oly.netpowerctrl.transfer.NeighbourAdapter;
import oly.netpowerctrl.transfer.NeighbourDiscoverReceiving;

/**
 * Neighbour discovery is activated if this fragment is on screen.
 */
public class NeighbourFragment extends Fragment {
    public static final String TAG = "NeighbourFragment";

    public NeighbourAdapter neighbourAdapter;
    private Switch sync_switch;
    UDPSending udpSending;
    NeighbourDiscoverReceiving udpReceiving;

    public NeighbourFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        //set the actionbar to use the custom view (can also be done with a style)
        //noinspection ConstantConditions
        ActionBar bar = getActivity().getActionBar();
        assert bar != null;
        bar.setDisplayShowCustomEnabled(true);
        bar.setDisplayShowHomeEnabled(true);
        bar.setDisplayShowTitleEnabled(false);
        bar.setCustomView(R.layout.neighbour_sync_switch);

        sync_switch = (Switch) getActivity().findViewById(R.id.neighbour_sync_switch);
        sync_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                enableSync(b);
            }
        });

        neighbourAdapter = new NeighbourAdapter(getActivity());
    }


    @Override
    public void onDetach() {
        neighbourAdapter = null;

        super.onDetach();
        ActionBar bar = getActivity().getActionBar();
        assert bar != null;
        bar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP |
                ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
    }

    @Override
    public void onStop() {
        super.onStop();
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
        //noinspection ConstantConditions
        getActivity().unregisterReceiver(wifiChanged);
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
        createDiscoverMessage();
        if (!discoverIsRunning)
            sendDiscoverMessageRunnable.run();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        //noinspection ConstantConditions
        getActivity().registerReceiver(wifiChanged, intentFilter);
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
                sendPairAckMessage(item.address, true);
                neighbourAdapter.setPaired(item, true);
            }
        });
        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                sendPairAckMessage(item.address, false);
            }
        });
        alert.show();
    }

    UDPSending.SendRawJob broadcastSendJob;

    void createDiscoverMessage() {
        ByteBuffer bb = ByteBuffer.allocate(200);
        bb.order(ByteOrder.BIG_ENDIAN);
        // Signature 8 Bytes
        bb.putInt(0xCAFEBABE).putInt(0xCCCCAAAA);
        // unique id 8 Bytes
        bb.putLong(Utils.getMacAsLong());
        // Version Code 4 Bytes
        bb.putInt(Utils.getVersionCode());
        // Devices, Scenes, Groups, Icons = 8 Bytes
        RuntimeDataController r = NetpowerctrlApplication.getDataController();
        bb.putShort((short) r.deviceCollection.devices.size());
        bb.putShort((short) r.sceneCollection.scenes.size());
        bb.putShort((short) r.groupCollection.groups.size());
        bb.putShort((short) 0);

        // We have 28 Bytes now.

        // Get Device name as byte buffer
        byte[] name;
        try {
            name = Utils.getDeviceName().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            return;
        }
        // Length of name, maximum is rest of allocated ByteBuffer
        short nameLength = name.length > bb.remaining() ? (short) bb.remaining() : (short) name.length;
        bb.putShort(nameLength); // add name length
        bb.put(name, 0, nameLength); // add name

        broadcastSendJob = new UDPSending.SendRawJob(bb.array(), 3311);
        if (broadcastSendJob.ip == null)
            broadcastSendJob = null;
    }

    void sendPairRequestMessage(InetAddress address) {
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.order(ByteOrder.BIG_ENDIAN);
        // Signature 8 Bytes
        bb.putInt(0xCAFEBABE).putInt(0xCCCCAAAB);
        // unique id 8 Bytes
        bb.putLong(Utils.getMacAsLong());

        udpSending.addJob(new UDPSending.SendRawJob(bb.array(), address, 3311));
    }

    void sendPairAckMessage(InetAddress address, boolean accepted) {
        ByteBuffer bb = ByteBuffer.allocate(17);
        bb.order(ByteOrder.BIG_ENDIAN);
        // Signature 8 Bytes
        bb.putInt(0xCAFEBABE).putInt(0xCCCCAAAC);
        // unique id 8 Bytes
        bb.putLong(Utils.getMacAsLong());
        // accepted byte (ok: A, not accepted: N)
        bb.put((byte) (accepted ? 1 : 0));

        udpSending.addJob(new UDPSending.SendRawJob(bb.array(), address, 3311));
    }

    boolean discoverIsRunning = false;
    Runnable sendDiscoverMessageRunnable = new Runnable() {
        @Override
        public void run() {
            if (broadcastSendJob == null)
                return;

            discoverIsRunning = true;

//            Log.w(TAG, "send");
            udpSending.addJob(broadcastSendJob);

            NetpowerctrlApplication.getMainThreadHandler().postDelayed(this, 2500);
        }
    };

    Runnable advanceTimeRunnable = new Runnable() {
        @Override
        public void run() {
            if (neighbourAdapter == null)
                return;

            neighbourAdapter.advanceTime();

            NetpowerctrlApplication.getMainThreadHandler().postDelayed(this, 5000);
        }
    };

    private void enableSync(boolean enable) {
        if (enable) {
            sync_switch.setChecked(false);
            //TODO auto abgleich
            //noinspection ConstantConditions
            Toast.makeText(getActivity(), "Automatischer Abgleich noch nicht unterstützt", Toast.LENGTH_SHORT).show();
        }
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

    private TextView networkIDText;

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
                NeighbourAdapter.AdapterItem item = neighbourAdapter.getNeighbour(position);
                if (item.isPaired)
                    return;

                if (!item.isSameVersion) {
                    //noinspection ConstantConditions
                    Toast.makeText(getActivity(), R.string.neighbour_not_same_version, Toast.LENGTH_SHORT).show();
                    return;
                }

                item.pairingRequest = true;
                sendPairRequestMessage(item.address);
                //noinspection ConstantConditions
                Toast.makeText(getActivity(), R.string.neighbour_pairing_request, Toast.LENGTH_SHORT).show();

            }
        });

        return view;
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

    private int lastGateway = 0;
    private BroadcastReceiver wifiChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int g = updateNetworkIDText();
            if (lastGateway == g)
                return;
            lastGateway = g;
            createDiscoverMessage();
            Log.w(TAG, "wifi changed");
            if (!discoverIsRunning)
                sendDiscoverMessageRunnable.run();
        }
    };

}
