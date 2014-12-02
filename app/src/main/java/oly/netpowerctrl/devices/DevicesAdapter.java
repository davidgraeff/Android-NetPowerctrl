package oly.netpowerctrl.devices;

import android.graphics.Paint;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.data.onDataLoaded;
import oly.netpowerctrl.device_base.device.Device;
import oly.netpowerctrl.device_base.device.DeviceConnection;
import oly.netpowerctrl.main.App;

/**
 * An adapter for showing all configured (and newly discovered) devices. Configured and new devices
 * are separated by headers.
 */
public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.ViewHolder>
        implements onCollectionUpdated<Object, Device>, onDataLoaded {

    private static final int DEVICE_HEADER = 0;
    private static final int DEVICE_CONNECTION = 1;
    private final boolean showNewDevices;
    private final boolean showConnections;
    private List<DeviceAdapterItem> mList = new ArrayList<>();

    @SuppressWarnings("SameParameterValue")
    public DevicesAdapter(boolean showNewDevices, boolean showConnections) {
        this.showNewDevices = showNewDevices;
        this.showConnections = showConnections;
        onResume();
    }

    public void onPause() {
        AppData d = AppData.getInstance();
        d.deviceCollection.unregisterObserver(this);
        d.unconfiguredDeviceCollection.unregisterObserver(this);
        AppData.observersOnDataLoaded.unregister(this);
    }

    public void onResume() {
        AppData.observersOnDataLoaded.register(this);
    }

    @Override
    public int getItemViewType(int position) {
        return mList.get(position).isDeviceHeader ? DEVICE_HEADER : DEVICE_CONNECTION;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        int res;
        if (viewType == DEVICE_HEADER)
            res = R.layout.list_item_device;
        else
            res = R.layout.list_item_device_connection;
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(res, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        DeviceAdapterItem item = mList.get(position);
        TextView title = viewHolder.title;
        title.setText(item.title);

        if (item.reachable) {
            title.setPaintFlags(title.getPaintFlags() & ~(Paint.STRIKE_THRU_TEXT_FLAG));
            viewHolder.image.setImageResource(android.R.drawable.presence_online);
        } else {
            if (item.tested)
                viewHolder.image.setImageResource(android.R.drawable.presence_offline);
            else
                viewHolder.image.setImageResource(android.R.drawable.presence_away);
            title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }

        TextView subtitle = viewHolder.subtitle;
        subtitle.setText(item.subtitle);
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    private void fullUpdate() {
        mList.clear();

        List<Device> deviceList = new ArrayList<>(AppData.getInstance().deviceCollection.getItems());
        if (showNewDevices)
            deviceList.addAll(AppData.getInstance().unconfiguredDeviceCollection.getItems());

        for (Device device : deviceList)
            addDeviceToList(device, mList.size());

        // Initial data
        notifyDataSetChanged();
    }

    @Override
    public boolean onDataLoaded() {
        AppData.getInstance().deviceCollection.registerObserver(this);
        if (showNewDevices) {
            AppData.getInstance().unconfiguredDeviceCollection.registerObserver(this);
        }

        fullUpdate();

        return true;
    }

    @Override
    public boolean updated(Object deviceCollection, Device device, ObserverUpdateActions action, int position) {
        if (device != null && device.getUniqueDeviceID() == null)
            return true;

        switch (action) {
            case AddAction:
            case ConnectionUpdateAction:
            case UpdateAction:
                assert device != null;
                // First remove all device connections and the device
                int start = -1, range = 0;
                for (int i = mList.size() - 1; i >= 0; --i) {
                    DeviceAdapterItem item = mList.get(i);
                    if (item.matches(device)) {
                        mList.remove(i);
                        start = i;
                        ++range;
                    }
                }

                // Add items at position start or at the end of the list if the device didn't exist before
                if (start == -1) start = mList.size();
                int addedItems = addDeviceToList(device, start);

                // All items are newly added, nothing removed
                if (range == 0) { // all added
                    notifyItemRangeInserted(start, addedItems);
                    break;
                }

                // Some items are added again (some have been removed before). Notify about the changed items.
                int updatedItems = Math.min(addedItems, range);
                notifyItemRangeChanged(start, updatedItems);

                // Notify about the additionally added items (compared to the state before removal)
                // or that the list now contains less items than before.
                if (range < addedItems) {
                    notifyItemRangeInserted(start + updatedItems, addedItems - range);
                } else {
                    notifyItemRangeRemoved(start + updatedItems, range - addedItems);
                }
                break;
            case ClearAndNewAction:
            case RemoveAllAction:
                fullUpdate();
                break;
            case RemoveAction:
                for (int i = mList.size() - 1; i >= 0; --i) {
                    DeviceAdapterItem item = mList.get(i);
                    assert device != null;
                    if (item.matches(device)) {
                        mList.remove(i);
                        notifyItemRemoved(position);
                    }
                }
                break;
        }
        return true;
    }

    private int addDeviceToList(Device device, int startIndex) {
        mList.add(startIndex++, new DeviceAdapterItem(device));
        if (showConnections && device.isConfigured()) {
            device.lockDevice();
            int s = device.getDeviceConnections().size();
            for (int i = 0; i < s; ++i) {
                mList.add(startIndex++, new DeviceAdapterItem(device.getConnectionByID(i), i));
            }
            device.releaseDevice();
            return 1 + s; // amount of connections plus the device itself
        } else
            return 1;
    }

    public DeviceAdapterItem getItem(int position) {
        return mList.get(position);
    }

    public static class DeviceAdapterItem {
        public String title;
        public String subtitle;
        public boolean isDeviceHeader;
        public int connectionID;
        public boolean reachable;
        public boolean tested;
        public boolean enabled = true;
        public boolean isConfigured;
        private String uid;

        /**
         * Create an entry for a device
         *
         * @param device The device to get data from.
         */
        public DeviceAdapterItem(Device device) {
            if (!device.isConfigured())
                this.title = App.getAppString(R.string.device_new, device.getDeviceName());
            else
                this.title = device.getDeviceName();

            this.subtitle = device.getFeatureString();
            this.isDeviceHeader = true;
            this.reachable = device.isReachable();
            this.tested = true;
            this.isConfigured = device.isConfigured();
            this.uid = device.getUniqueDeviceID();
        }

        /**
         * Create an entry for a device connection
         *
         * @param deviceConnection The connection to get data from.
         * @param connectionID     The position in the connections list of the parent device.
         */
        public DeviceAdapterItem(DeviceConnection deviceConnection, int connectionID) {

            this.title = "";
            if (deviceConnection.isAssignedByDevice())
                this.title += "Auto.";
            this.title += deviceConnection.getProtocol() + "/" + deviceConnection.getDestinationHost();
            if (deviceConnection.getDestinationPort() != -1)
                this.title += ":" + String.valueOf(deviceConnection.getDestinationPort());

            this.tested = true;
            this.reachable = deviceConnection.isReachable();

            this.subtitle = deviceConnection.getNotReachableReason();
            if (this.subtitle == null) {
                if (!this.reachable) {
                    this.tested = false;
                    this.subtitle = App.getAppString(R.string.device_connection_notTested);
                } else
                    this.subtitle = App.getAppString(R.string.device_reachable);
            }

            this.isDeviceHeader = false;
            this.connectionID = connectionID;
            this.uid = deviceConnection.device.getUniqueDeviceID();
            this.isConfigured = deviceConnection.device.isConfigured();
        }

        public String getUid() {
            return uid;
        }

        public boolean matches(Device device) {
            return isConfigured == device.isConfigured() && uid.equals(device.getUniqueDeviceID());
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView title;
        public TextView subtitle;
        public ImageView image;

        public ViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.title);
            subtitle = (TextView) itemView.findViewById(R.id.subtitle);
            image = (ImageView) itemView.findViewById(R.id.device_connection_reachable);
        }
    }
}
