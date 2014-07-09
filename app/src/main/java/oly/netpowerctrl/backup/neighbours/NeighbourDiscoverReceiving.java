package oly.netpowerctrl.backup.neighbours;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.network.UDPReceiving;

/**
 * Neighbour discovery
 */
public class NeighbourDiscoverReceiving extends UDPReceiving {
    private final WeakReference<NeighbourFragment> neighbourFragmentWeakReference;

    public NeighbourDiscoverReceiving(NeighbourFragment neighbourFragment) {
        super(3311);
        this.neighbourFragmentWeakReference = new WeakReference<>(neighbourFragment);
    }

    // This is executed in another thread!
    @Override
    public void parsePacket(final byte[] message, int length, int receive_port) {
        if (length < 16) return;
        ByteBuffer bb = ByteBuffer.wrap(message);
        bb.order(ByteOrder.BIG_ENDIAN);

        // Compare signature
        if (bb.getInt() != 0xCAFEBABE) return;

        int packetType = bb.getInt();

        // Get unique id, 4 bytes
        final long uniqueID = bb.getLong();

        // Ignore own broadcast packet
//        if (Utils.getMacAsLong() == uniqueID)
//            return;
        switch (packetType) {
            //noinspection ConstantConditions
            case 0xCCCCAAAA: // Discover packet
                Log.w(NeighbourFragment.TAG, "parseDiscoverPacket");
                parseDiscoverPacket(bb, uniqueID);
                break;
            //noinspection ConstantConditions
            case 0xCCCCAAAB: // Pair init
                Log.w(NeighbourFragment.TAG, "parsePairInitPacket");
                parsePairInitPacket(uniqueID);
                break;
            //noinspection ConstantConditions
            case 0xCCCCAAAC: // Pair (n)ack
                Log.w(NeighbourFragment.TAG, "parsePairResultPacket");
                parsePairResultPacket(bb, uniqueID);
                break;
            default:
                Log.w(NeighbourFragment.TAG, "packet not recognized");
        }
    }

    private void parsePairResultPacket(ByteBuffer bb, final long uniqueID) {
        if (bb.remaining() < 1) return;

        final NeighbourFragment neighbourFragment = neighbourFragmentWeakReference.get();
        if (neighbourFragment == null) {
            interrupt();
            return;
        }

        final NeighbourAdapter.AdapterItem item = neighbourFragment.neighbourAdapter.getItemByID(uniqueID);
        if (item == null || !item.pairingRequest)
            return;

        item.pairingRequest = false;

        final byte c = bb.get();
        if (c > 1) {
            Log.e(NeighbourFragment.TAG, "parsePairResultPacket unexpected code");
            return;
        }

        NetpowerctrlApplication.getMainThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                neighbourFragment.pairResult(item, c == 1);
            }
        });
    }

    private void parsePairInitPacket(final long uniqueID) {
        final NeighbourFragment neighbourFragment = neighbourFragmentWeakReference.get();
        if (neighbourFragment == null) {
            interrupt();
            return;
        }

        final NeighbourAdapter.AdapterItem item = neighbourFragment.neighbourAdapter.getItemByID(uniqueID);
        if (item == null)
            return;

        NetpowerctrlApplication.getMainThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                neighbourFragment.askForPairing(item);
            }
        });
    }

    void parseDiscoverPacket(ByteBuffer bb, final long uniqueID) {
        final NeighbourFragment neighbourFragment = neighbourFragmentWeakReference.get();
        if (neighbourFragment == null) {
            Log.e(NeighbourFragment.TAG, "Stop discovery, did not found fragment!");
            interrupt();
            return;
        }

        if (bb.remaining() < 10) return;

        // Get others and own version
        final int version = bb.getInt();
        final int versionCode;
        Context c = NetpowerctrlApplication.instance;
        try {
            //noinspection ConstantConditions
            versionCode = c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return;
        }
        final short devices = bb.getShort();
        final short scenes = bb.getShort();
        final short groups = bb.getShort();
        final short icons = bb.getShort();

        // Get name length and name bytes
        final short nameLength = bb.getShort();
        if (nameLength > bb.remaining()) {
            Log.e(NeighbourFragment.TAG, "SecurityException. Datagram is smaller than advertised");
            return;
        }

        final String name;
        try {
            name = new String(bb.array(), bb.position(), nameLength, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return;
        }

        // Post to main thread
        NetpowerctrlApplication.getMainThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                if (neighbourFragment.neighbourAdapter == null)
                    return;
                neighbourFragment.neighbourAdapter.add(name, uniqueID, version, versionCode,
                        devices, scenes, groups, icons, receivedDatagram.getAddress());
                neighbourFragment.syncTimer();
            }
        });
    }
}
