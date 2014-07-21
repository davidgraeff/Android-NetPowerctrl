package oly.netpowerctrl.device_ports;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.devices.DevicePort;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.IconDeferredLoadingThread;
import oly.netpowerctrl.utils.Icons;
import oly.netpowerctrl.utils.ListItemMenu;
import oly.netpowerctrl.utils.gui.AnimationController;

public class DevicePortsExecuteAdapter extends DevicePortsBaseAdapter implements
        SeekBar.OnSeekBarChangeListener {

    // We block updates while moving the range slider
    private static final String TAG = "PortAdapter";

    public DevicePortsExecuteAdapter(Context context, ListItemMenu mListContextMenu, DevicePortSource source,
                                     IconDeferredLoadingThread iconCache) {
        super(context, mListContextMenu, source, iconCache, true);
        mShowHidden = SharedPrefs.getShowHiddenOutlets();
        setLayoutRes(R.layout.list_icon_item);
        if (source != null)
            source.updateNow();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DevicePortListItem item = mItems.get(position);
        DevicePort port = item.port;

        convertView = super.getView(position, convertView, parent);

        // Not our business, if port is null
        if (port == null) {
            if (mCurrent_devicePortViewHolder.isNew && mCurrent_devicePortViewHolder.imageEdit != null) {
                mCurrent_devicePortViewHolder.imageEdit.setVisibility(View.INVISIBLE);
            }
            return convertView;
        }

        // We do this only once, if the viewHolder is new
        if (mCurrent_devicePortViewHolder.isNew) {
            // We use the tools icon for the context menu.
            if (mCurrent_devicePortViewHolder.imageEdit != null) {
                mCurrent_devicePortViewHolder.imageEdit.setVisibility(View.VISIBLE);
                mCurrent_devicePortViewHolder.imageEdit.setTag(position);
                mCurrent_devicePortViewHolder.imageEdit.setOnClickListener(mCurrent_devicePortViewHolder);
            }
            //current_viewHolder.mainTextView.setTag(position);
            switch (port.getType()) {
                case TypeToggle: {
                    mCurrent_devicePortViewHolder.seekBar.setVisibility(View.GONE);
                    mCurrent_devicePortViewHolder.loadIcon(mIconCache, port.uuid,
                            Icons.IconType.DevicePortIcon, Icons.IconState.StateOff,
                            Icons.getResIdForState(Icons.IconState.StateOff), 0);
                    mCurrent_devicePortViewHolder.loadIcon(mIconCache, port.uuid,
                            Icons.IconType.DevicePortIcon, Icons.IconState.StateOn,
                            Icons.getResIdForState(Icons.IconState.StateOn), 1);
                    break;
                }
                case TypeButton: {
                    mCurrent_devicePortViewHolder.loadIcon(mIconCache, port.uuid,
                            Icons.IconType.DevicePortIcon, Icons.IconState.StateToggle,
                            R.drawable.netpowerctrl, 0);
                    mCurrent_devicePortViewHolder.seekBar.setVisibility(View.GONE);
                    mCurrent_devicePortViewHolder.setCurrentBitmapIndex(0);
                    break;
                }
                case TypeRangedValue:
                    mCurrent_devicePortViewHolder.loadIcon(mIconCache, port.uuid,
                            Icons.IconType.DevicePortIcon, Icons.IconState.StateOff,
                            Icons.getResIdForState(Icons.IconState.StateOff), 0);
                    mCurrent_devicePortViewHolder.loadIcon(mIconCache, port.uuid,
                            Icons.IconType.DevicePortIcon, Icons.IconState.StateOn,
                            Icons.getResIdForState(Icons.IconState.StateOn), 1);
                    mCurrent_devicePortViewHolder.seekBar.setVisibility(View.VISIBLE);
                    mCurrent_devicePortViewHolder.seekBar.setOnSeekBarChangeListener(this);
                    mCurrent_devicePortViewHolder.seekBar.setTag(-1);
                    mCurrent_devicePortViewHolder.seekBar.setMax(port.max_value - port.min_value);
                    break;
            }

        }

        // This has to be done more often
        switch (port.getType()) {
            case TypeButton: {
                break;
            }
            case TypeToggle: {
                mCurrent_devicePortViewHolder.setCurrentBitmapIndex(port.current_value >= port.max_value ? 1 : 0);
                break;
            }
            case TypeRangedValue:
                mCurrent_devicePortViewHolder.seekBar.setTag(-1);
                mCurrent_devicePortViewHolder.seekBar.setProgress(port.current_value - port.min_value);
                mCurrent_devicePortViewHolder.seekBar.setTag(position);
                mCurrent_devicePortViewHolder.setCurrentBitmapIndex(port.current_value <= port.min_value ? 0 : 1);
                break;
        }

        return convertView;
    }

    // Called from the listView that uses this adapter
    public void handleClick(int position, long id) {
        DevicePortListItem info = mItems.get(position);
        NetpowerctrlApplication.getDataController().execute(info.port, DevicePort.TOGGLE, null);
        AnimationController a = mAnimationWeakReference.get();
        if (a != null)
            a.addHighlight(id);
        notifyDataSetChanged();
    }

    @Override
    public void onProgressChanged(SeekBar view, int value, boolean b) {
        int position = (Integer) view.getTag();
        if (position == -1)
            return;
        DevicePortListItem info = mItems.get(position);
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
