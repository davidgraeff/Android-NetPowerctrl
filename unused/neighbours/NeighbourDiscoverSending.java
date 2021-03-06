package oly.netpowerctrl.backup.neighbours;

import android.content.Context;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import oly.netpowerctrl.data.RuntimeDataController;
import oly.netpowerctrl.network.UDPSending;
import oly.netpowerctrl.network.Utils;
import oly.netpowerctrl.utils.Icons;

/**
 * Neighbour discover messages
 */
class NeighbourDiscoverSending {
    public static int icon_size_cache = -1;

    public static UDPSending.SendRawJob createDiscoverMessage(Context context) {
        if (icon_size_cache == -1)
            icon_size_cache = Icons.getAllIcons(context).length;

        ByteBuffer bb = ByteBuffer.allocate(200);
        bb.order(ByteOrder.BIG_ENDIAN);
        // Signature 8 Bytes
        bb.putInt(0xCAFEBABE).putInt(0xCCCCAAAA);
        // unique id 8 Bytes
        bb.putLong(Utils.getMacAsLong());
        // Version Code 4 Bytes
        bb.putInt(Utils.getVersionCode(context));
        // Devices, Scenes, Groups, Icons = 8 Bytes
        RuntimeDataController r = RuntimeDataController.getDataController();
        bb.putShort((short) r.deviceCollection.devices.size());
        bb.putShort((short) r.sceneCollection.scenes.size());
        bb.putShort((short) r.groupCollection.groups.size());
        bb.putShort((short) icon_size_cache);

        // We have 28 Bytes now.

        // Get Device name as byte buffer
        byte[] name;
        try {
            name = Utils.getDeviceName().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
        // Length of name, maximum is rest of allocated ByteBuffer
        short nameLength = name.length > bb.remaining() ? (short) bb.remaining() : (short) name.length;
        bb.putShort(nameLength); // add name length
        bb.put(name, 0, nameLength); // add name

        UDPSending.SendRawJob broadcastSendJob = new UDPSending.SendRawJob(bb.array(), 3311);
        if (broadcastSendJob.ip == null)
            broadcastSendJob = null;

        return broadcastSendJob;
    }

    public static void sendPairRequestMessage(UDPSending udpSending, InetAddress address) {
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.order(ByteOrder.BIG_ENDIAN);
        // Signature 8 Bytes
        bb.putInt(0xCAFEBABE).putInt(0xCCCCAAAB);
        // unique id 8 Bytes
        bb.putLong(Utils.getMacAsLong());

        udpSending.addJob(new UDPSending.SendRawJob(bb.array(), address, 3311));
    }

    public static void sendPairAckMessage(UDPSending udpSending, boolean accepted, InetAddress address) {
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
}
