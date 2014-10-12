package oly.netpowerctrl.device_ports;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.IconDeferredLoadingThread;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.utils.AnimationController;
import oly.netpowerctrl.utils.controls.onListItemElementClicked;

public class DevicePortsExecuteAdapter extends DevicePortsBaseAdapter implements
        SeekBar.OnSeekBarChangeListener {

    // We block updates while moving the range slider
    private static final String TAG = "PortAdapter";
    public TitleClick titleClick = null;
    private final View.OnClickListener titleOnlyClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (titleClick != null)
                titleClick.onTitleClick((Integer) view.getTag());
        }
    };
    private boolean enableEditing;

    public DevicePortsExecuteAdapter(Context context, onListItemElementClicked mListContextMenu, DevicePortSourceInterface source,
                                     IconDeferredLoadingThread iconCache) {
        super(context, mListContextMenu, source, iconCache, true);
        mShowHidden = SharedPrefs.getInstance().getShowHiddenOutlets();
        setLayoutRes(R.layout.list_item_icon);
        if (source != null)
            source.updateNow();
    }

    @Override
    public boolean isEnabled(int position) {
        DevicePort port = mItems.get(position).port;
        return port == null || port.device.getFirstReachableConnection() != null;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DevicePortAdapterItem item = mItems.get(position);
        DevicePort port = item.port;

        convertView = super.getView(position, convertView, parent);

        // Not our business, if port is null
        if (port == null) {
            if (current_viewHolder.isNew && current_viewHolder.imageEdit != null) {
                current_viewHolder.imageEdit.setVisibility(View.INVISIBLE);
            }
            return convertView;
        }

        // Assign position to title view. Used in the custom OnClickListener
        current_viewHolder.title.setTag(position);

        // We do this only once, if the viewHolder is new
        if (current_viewHolder.isNew) {
            // We use the tools icon for the context menu.
            if (current_viewHolder.imageEdit != null) {
                if (!enableEditing)
                    current_viewHolder.imageEdit.setVisibility(View.GONE);
                else {
                    current_viewHolder.imageEdit.setTag(position);
                    current_viewHolder.imageEdit.setOnClickListener(current_viewHolder);
                    current_viewHolder.imageEdit.setVisibility(View.VISIBLE);
                }
            }

            // If we have a listener for title clicks, we set the click listener now
            if (titleClick != null) {
                current_viewHolder.title.setOnClickListener(titleOnlyClick);
            }

            //current_viewHolder.mainTextView.setTag(position);
            switch (port.getType()) {
                case TypeToggle: {
                    current_viewHolder.seekBar.setVisibility(View.GONE);
                    current_viewHolder.loadIcon(mIconCache, port.uuid,
                            LoadStoreIconData.IconType.DevicePortIcon, LoadStoreIconData.IconState.StateOff,
                            LoadStoreIconData.getResIdForState(LoadStoreIconData.IconState.StateOff), 0);
                    current_viewHolder.loadIcon(mIconCache, port.uuid,
                            LoadStoreIconData.IconType.DevicePortIcon, LoadStoreIconData.IconState.StateOn,
                            LoadStoreIconData.getResIdForState(LoadStoreIconData.IconState.StateOn), 1);
                    break;
                }
                case TypeButton: {
                    current_viewHolder.loadIcon(mIconCache, port.uuid,
                            LoadStoreIconData.IconType.DevicePortIcon, LoadStoreIconData.IconState.StateToggle,
                            R.drawable.netpowerctrl, 0);
                    current_viewHolder.seekBar.setVisibility(View.GONE);
                    current_viewHolder.setCurrentBitmapIndex(0);
                    break;
                }
                case TypeRangedValue:
                    current_viewHolder.loadIcon(mIconCache, port.uuid,
                            LoadStoreIconData.IconType.DevicePortIcon, LoadStoreIconData.IconState.StateOff,
                            LoadStoreIconData.getResIdForState(LoadStoreIconData.IconState.StateOff), 0);
                    current_viewHolder.loadIcon(mIconCache, port.uuid,
                            LoadStoreIconData.IconType.DevicePortIcon, LoadStoreIconData.IconState.StateOn,
                            LoadStoreIconData.getResIdForState(LoadStoreIconData.IconState.StateOn), 1);
                    current_viewHolder.seekBar.setVisibility(View.VISIBLE);
                    current_viewHolder.seekBar.setOnSeekBarChangeListener(this);
                    current_viewHolder.seekBar.setTag(-1);
                    current_viewHolder.seekBar.setMax(port.max_value - port.min_value);
                    break;
            }

        }

        // This has to be done more often
        switch (port.getType()) {
            case TypeButton: {
                break;
            }
            case TypeToggle: {
                current_viewHolder.setCurrentBitmapIndex(port.current_value >= port.max_value ? 1 : 0);
                break;
            }
            case TypeRangedValue:
                current_viewHolder.seekBar.setTag(-1);
                current_viewHolder.seekBar.setProgress(port.current_value - port.min_value);
                current_viewHolder.seekBar.setTag(position);
                current_viewHolder.setCurrentBitmapIndex(port.current_value <= port.min_value ? 0 : 1);
                break;
        }

        return convertView;
    }

    // Called from the listView that uses this adapter
    public void handleClick(int position, long id) {
        DevicePortAdapterItem info = mItems.get(position);
        AppData.getInstance().execute(info.port, DevicePort.TOGGLE, null);
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
        AppData.getInstance().execute(info.port, info.command_value, null);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        getSource().setAutomaticUpdate(false);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        getSource().setAutomaticUpdate(true);
    }

    public boolean isEnableEditing() {
        return enableEditing;
    }

    public void setEnableEditing(boolean enableEditing) {
        this.enableEditing = enableEditing;
    }

    public interface TitleClick {
        void onTitleClick(int position);
    }
}
