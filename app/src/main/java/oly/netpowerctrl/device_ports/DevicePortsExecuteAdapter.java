package oly.netpowerctrl.device_ports;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.RuntimeDataController;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.IconDeferredLoadingThread;
import oly.netpowerctrl.utils.Icons;
import oly.netpowerctrl.utils.ListItemMenu;
import oly.netpowerctrl.utils_gui.AnimationController;

public class DevicePortsExecuteAdapter extends DevicePortsBaseAdapter implements
        SeekBar.OnSeekBarChangeListener {

    // We block updates while moving the range slider
    private static final String TAG = "PortAdapter";

    public DevicePortsExecuteAdapter(Context context, ListItemMenu mListContextMenu, DevicePortSource source,
                                     IconDeferredLoadingThread iconCache) {
        super(context, mListContextMenu, source, iconCache, true);
        mShowHidden = SharedPrefs.getInstance().getShowHiddenOutlets();
        setLayoutRes(R.layout.list_icon_item);
        if (source != null)
            source.updateNow();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DevicePortAdapterItem item = mItems.get(position);
        DevicePort port = item.port;

        convertView = super.getView(position, convertView, parent);

        // Not our business, if port is null
        if (port == null) {
            if (cViewHolder.isNew && cViewHolder.imageEdit != null) {
                cViewHolder.imageEdit.setVisibility(View.INVISIBLE);
            }
            return convertView;
        }

        // We do this only once, if the viewHolder is new
        if (cViewHolder.isNew) {
            // We use the tools icon for the context menu.
            if (cViewHolder.imageEdit != null) {
                cViewHolder.imageEdit.setVisibility(View.VISIBLE);
                cViewHolder.imageEdit.setTag(position);
                cViewHolder.imageEdit.setOnClickListener(cViewHolder);
            }
            //current_viewHolder.mainTextView.setTag(position);
            switch (port.getType()) {
                case TypeToggle: {
                    cViewHolder.seekBar.setVisibility(View.GONE);
                    cViewHolder.loadIcon(mIconCache, port.uuid,
                            Icons.IconType.DevicePortIcon, Icons.IconState.StateOff,
                            Icons.getResIdForState(Icons.IconState.StateOff), 0);
                    cViewHolder.loadIcon(mIconCache, port.uuid,
                            Icons.IconType.DevicePortIcon, Icons.IconState.StateOn,
                            Icons.getResIdForState(Icons.IconState.StateOn), 1);
                    break;
                }
                case TypeButton: {
                    cViewHolder.loadIcon(mIconCache, port.uuid,
                            Icons.IconType.DevicePortIcon, Icons.IconState.StateToggle,
                            R.drawable.netpowerctrl, 0);
                    cViewHolder.seekBar.setVisibility(View.GONE);
                    cViewHolder.setCurrentBitmapIndex(0);
                    break;
                }
                case TypeRangedValue:
                    cViewHolder.loadIcon(mIconCache, port.uuid,
                            Icons.IconType.DevicePortIcon, Icons.IconState.StateOff,
                            Icons.getResIdForState(Icons.IconState.StateOff), 0);
                    cViewHolder.loadIcon(mIconCache, port.uuid,
                            Icons.IconType.DevicePortIcon, Icons.IconState.StateOn,
                            Icons.getResIdForState(Icons.IconState.StateOn), 1);
                    cViewHolder.seekBar.setVisibility(View.VISIBLE);
                    cViewHolder.seekBar.setOnSeekBarChangeListener(this);
                    cViewHolder.seekBar.setTag(-1);
                    cViewHolder.seekBar.setMax(port.max_value - port.min_value);
                    break;
            }

        }

        // This has to be done more often
        switch (port.getType()) {
            case TypeButton: {
                break;
            }
            case TypeToggle: {
                cViewHolder.setCurrentBitmapIndex(port.current_value >= port.max_value ? 1 : 0);
                break;
            }
            case TypeRangedValue:
                cViewHolder.seekBar.setTag(-1);
                cViewHolder.seekBar.setProgress(port.current_value - port.min_value);
                cViewHolder.seekBar.setTag(position);
                cViewHolder.setCurrentBitmapIndex(port.current_value <= port.min_value ? 0 : 1);
                break;
        }

        return convertView;
    }

    // Called from the listView that uses this adapter
    public void handleClick(int position, long id) {
        DevicePortAdapterItem info = mItems.get(position);
        RuntimeDataController.getDataController().execute(info.port, DevicePort.TOGGLE, null);
        AnimationController a = mAnimationWeakReference.get();
        if (a != null)
            a.addHighlight(id, R.id.text1);
        notifyDataSetChanged();
    }

    @Override
    public void onProgressChanged(SeekBar view, int value, boolean b) {
        int position = (Integer) view.getTag();
        if (position == -1)
            return;
        DevicePortAdapterItem info = mItems.get(position);
        info.port.current_value = value + info.port.min_value;
        info.command_value = info.port.current_value;
        RuntimeDataController.getDataController().execute(info.port, info.command_value, null);
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
