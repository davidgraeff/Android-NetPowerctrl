package oly.netpowerctrl.device_ports;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.devices.DevicePort;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.Icons;
import oly.netpowerctrl.utils.ListItemMenu;

public class DevicePortsExecuteAdapter extends DevicePortsBaseAdapter implements
        SeekBar.OnSeekBarChangeListener {

    // We block updates while moving the range slider
    private static final String TAG = "PortAdapter";

    /**
     * The constructor will not load the adapter data! Call setGroupFilter to load data!
     *
     * @param context
     * @param mListContextMenu
     */
    public DevicePortsExecuteAdapter(Context context, ListItemMenu mListContextMenu, DevicePortSource source) {
        super(context, mListContextMenu, source);
        showHidden = SharedPrefs.getShowHiddenOutlets();
        setLayoutRes(R.layout.list_icon_item);
    }

    @Override
    public int getViewTypeCount() {
        return DevicePort.DevicePortType.values().length;
    }

    @Override
    public int getItemViewType(int position) {
        return all_outlets.get(position).port.getType().ordinal();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DevicePortListItem item = all_outlets.get(position);
        DevicePort port = item.port;

        convertView = super.getView(position, convertView, parent);

        // We do this only once, if the viewHolder is new
        if (current_devicePortViewHolder.isNew) {
            // We use the tools icon for the context menu.
            if (current_devicePortViewHolder.imageEdit != null) {
                current_devicePortViewHolder.imageEdit.setTag(position);
                current_devicePortViewHolder.imageEdit.setOnClickListener(current_devicePortViewHolder);
            }
            //current_viewHolder.mainTextView.setTag(position);
            switch (port.getType()) {
                case TypeToggle: {
                    current_devicePortViewHolder.seekBar.setVisibility(View.GONE);
                    current_devicePortViewHolder.loadIcon(port.uuid,
                            Icons.IconType.DevicePortIcon, Icons.IconState.StateOff,
                            Icons.getResIdForState(Icons.IconState.StateOff), 0);
                    current_devicePortViewHolder.loadIcon(port.uuid,
                            Icons.IconType.DevicePortIcon, Icons.IconState.StateOn,
                            Icons.getResIdForState(Icons.IconState.StateOn), 1);
                    break;
                }
                case TypeButton: {
                    current_devicePortViewHolder.loadIcon(port.uuid,
                            Icons.IconType.DevicePortIcon, Icons.IconState.StateToggle,
                            R.drawable.netpowerctrl, 0);
                    current_devicePortViewHolder.seekBar.setVisibility(View.GONE);
                    break;
                }
                case TypeRangedValue:
                    current_devicePortViewHolder.loadIcon(port.uuid,
                            Icons.IconType.DevicePortIcon, Icons.IconState.StateOff,
                            Icons.getResIdForState(Icons.IconState.StateOff), 0);
                    current_devicePortViewHolder.loadIcon(port.uuid,
                            Icons.IconType.DevicePortIcon, Icons.IconState.StateOn,
                            Icons.getResIdForState(Icons.IconState.StateOn), 1);
                    current_devicePortViewHolder.seekBar.setVisibility(View.VISIBLE);
                    current_devicePortViewHolder.seekBar.setOnSeekBarChangeListener(this);
                    current_devicePortViewHolder.seekBar.setTag(-1);
                    current_devicePortViewHolder.seekBar.setMax(port.max_value - port.min_value);
                    break;
            }

        }

        // This has to be done more often
        switch (port.getType()) {
            case TypeButton: {
                break;
            }
            case TypeToggle: {
                current_devicePortViewHolder.setCurrentBitmapIndex(port.current_value >= port.max_value ? 1 : 0);
                break;
            }
            case TypeRangedValue:
                current_devicePortViewHolder.seekBar.setTag(-1);
                current_devicePortViewHolder.seekBar.setProgress(port.current_value - port.min_value);
                current_devicePortViewHolder.seekBar.setTag(position);
                current_devicePortViewHolder.setCurrentBitmapIndex(port.current_value <= port.min_value ? 0 : 1);
                break;
        }

        return convertView;
    }

    // Called from the listView that uses this adapter
    public void handleClick(int position, long id) {
        DevicePortListItem info = all_outlets.get(position);
        NetpowerctrlApplication.getDataController().execute(info.port, DevicePort.TOGGLE, null);
        animate_click_id = id;
        notifyDataSetChanged();
    }

    @Override
    public void onProgressChanged(SeekBar view, int value, boolean b) {
        int position = (Integer) view.getTag();
        if (position == -1)
            return;
        DevicePortListItem info = all_outlets.get(position);
        info.port.current_value = value + info.port.min_value;
        info.command_value = info.port.current_value;
        NetpowerctrlApplication.getDataController().execute(info.port, info.command_value, null);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        getSource().setAutomaticUpdate(false);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        getSource().setAutomaticUpdate(true);
    }
}
