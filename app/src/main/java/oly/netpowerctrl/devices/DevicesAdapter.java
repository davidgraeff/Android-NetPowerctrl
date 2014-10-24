package oly.netpowerctrl.devices;

import android.graphics.Paint;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.data.onDataLoaded;
import oly.netpowerctrl.main.App;

/**
 * An adapter for showing all configured (and newly discovered) devices. Configured and new devices
 * are separated by headers.
 */
public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.ViewHolder>
        implements onCollectionUpdated<DeviceCollection, Device>, onDataLoaded {
    private final boolean showNewDevices;
    private onCollectionUpdated<UnconfiguredDeviceCollection, Device> newDeviceObserver = new onCollectionUpdated<UnconfiguredDeviceCollection, Device>() {
        @Override
        public boolean updated(UnconfiguredDeviceCollection unconfiguredDeviceCollection, Device device, ObserverUpdateActions action, int position) {
            if (position == -1) {
                notifyDataSetChanged();
                return true;
            }

            AppData d = AppData.getInstance();
            position = d.deviceCollection.size() + position;

            switch (action) {
                case AddAction:
                    notifyItemInserted(position);
                    break;
                case ClearAndNewAction:
                    notifyDataSetChanged();
                    break;
                case RemoveAllAction:
                    notifyItemRangeRemoved(d.deviceCollection.size(), position);
                    break;
                case RemoveAction:
                    notifyItemRemoved(position);
                    break;
                case UpdateAction:
                    notifyItemChanged(position);
                    break;
            }

            return true;
        }
    };

    @SuppressWarnings("SameParameterValue")
    public DevicesAdapter(boolean showNewDevices) {
        this.showNewDevices = showNewDevices;
        onResume();
    }

    public void onPause() {
        AppData d = AppData.getInstance();
        d.deviceCollection.unregisterObserver(this);
        AppData.observersOnDataLoaded.unregister(this);
        AppData.getInstance().unconfiguredDeviceCollection.unregisterObserver(newDeviceObserver);
    }

    public void onResume() {
        AppData.observersOnDataLoaded.register(this);
        onDataLoaded();
        if (showNewDevices) {
            AppData.getInstance().unconfiguredDeviceCollection.registerObserver(newDeviceObserver);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return 1;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_device, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        Device device = getDevice(position);
        boolean reachable = device.getFirstReachableConnection() != null;
        TextView tvName = viewHolder.device_name;
        if (device.configured)
            tvName.setText(device.DeviceName);
        else
            tvName.setText(App.getAppString(R.string.device_new, device.DeviceName));

        if (reachable)
            tvName.setPaintFlags(tvName.getPaintFlags() & ~(Paint.STRIKE_THRU_TEXT_FLAG));
        else
            tvName.setPaintFlags(tvName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        TextView tvIP = viewHolder.device_ip;
        String subtext = "";
        if (!reachable)
            subtext += device.getNotReachableReasons();
        else {
            DeviceConnection deviceConnection = device.getFirstReachableConnection();
            subtext += deviceConnection.getProtocol() + "/" + deviceConnection.getDestinationHost();
        }

        if (device.Version.length() > 0)
            subtext += ", " + device.Version;
        if (device.hasFeatures())
            subtext += ", " + device.getFeatureString();
        tvIP.setText(subtext);
    }

    public Device getDevice(int position) {
        AppData d = AppData.getInstance();
        int cs = d.deviceCollection.size();
        if (position < cs)
            return d.deviceCollection.get(position);
        else // minus deviceCollection size
            return d.unconfiguredDeviceCollection.get(position - cs);
    }

    @Override
    public int getItemCount() {
        AppData d = AppData.getInstance();
        int c = d.deviceCollection.size();
        if (showNewDevices) {
            c += d.unconfiguredDeviceCollection.size();
        }
        return c;
    }

    @Override
    public boolean onDataLoaded() {
        AppData.getInstance().deviceCollection.registerObserver(this);
        return true;
    }

    @Override
    public boolean updated(DeviceCollection deviceCollection, Device device, ObserverUpdateActions action, int position) {
        if (position == -1) {
            notifyDataSetChanged();
            return true;
        }

        switch (action) {
            case AddAction:
                notifyItemInserted(position);
                break;
            case ClearAndNewAction:
                notifyDataSetChanged();
                break;
            case RemoveAllAction:
                notifyItemRangeRemoved(0, position);
                break;
            case RemoveAction:
                notifyItemRemoved(position);
                break;
            case UpdateAction:
                notifyItemChanged(position);
                break;
        }
        return true;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView device_name;
        public TextView device_ip;

        public ViewHolder(View itemView) {
            super(itemView);
            device_name = (TextView) itemView.findViewById(R.id.device_name);
            device_ip = (TextView) itemView.findViewById(R.id.device_ip);
        }
    }
}
