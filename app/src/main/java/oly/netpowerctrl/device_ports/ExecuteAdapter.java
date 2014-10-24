package oly.netpowerctrl.device_ports;

import android.view.View;
import android.widget.SeekBar;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.Executable;
import oly.netpowerctrl.data.IconDeferredLoadingThread;
import oly.netpowerctrl.data.LoadStoreIconData;

public class ExecuteAdapter extends DevicePortsBaseAdapter implements
        SeekBar.OnSeekBarChangeListener {

    // We block updates while moving the range slider
    private static final String TAG = "PortAdapter";
    private boolean enableEditing;

    public ExecuteAdapter(DevicePortSourceInterface source,
                          IconDeferredLoadingThread iconCache) {
        super(source, iconCache, true);
        setLayoutRes(R.layout.list_item_icon);
        if (source != null)
            source.updateNow();
    }

    @Override
    public void onBindViewHolder(DevicePortViewHolder devicePortViewHolder, int position) {
        boolean isNew = devicePortViewHolder.isNew;
        super.onBindViewHolder(devicePortViewHolder, position);

        ExecutableAdapterItem item = mItems.get(position);
        Executable executable = item.getExecutable();

        // Not our business, if port is null
        if (executable == null) {
            if (isNew && devicePortViewHolder.imageEdit != null) {
                devicePortViewHolder.imageEdit.setVisibility(View.INVISIBLE);
            }
            return;
        }

        // We do this only once, if the viewHolder is new
        if (isNew) {
            // We use the tools icon for the context menu.
            if (devicePortViewHolder.imageEdit != null) {
                devicePortViewHolder.imageEdit.setVisibility(enableEditing ? View.VISIBLE : View.GONE);
            }

            //current_viewHolder.mainTextView.setTag(position);
            switch (executable.getType()) {
                case TypeToggle: {
                    devicePortViewHolder.seekBar.setVisibility(View.GONE);
                    devicePortViewHolder.loadIcon(mIconCache, executable.getUid(),
                            LoadStoreIconData.IconType.DevicePortIcon, LoadStoreIconData.IconState.StateOff, 0);
                    devicePortViewHolder.loadIcon(mIconCache, executable.getUid(),
                            LoadStoreIconData.IconType.DevicePortIcon, LoadStoreIconData.IconState.StateOn, 1);
                    break;
                }
                case TypeButton: {
                    devicePortViewHolder.loadIcon(mIconCache, executable.getUid(),
                            LoadStoreIconData.IconType.DevicePortIcon, LoadStoreIconData.IconState.OnlyOneState, 0);
                    devicePortViewHolder.seekBar.setVisibility(View.GONE);
                    devicePortViewHolder.setBitmapOff();
                    break;
                }
                case TypeRangedValue:
                    DevicePort devicePort = (DevicePort) executable;
                    devicePortViewHolder.loadIcon(mIconCache, executable.getUid(),
                            LoadStoreIconData.IconType.DevicePortIcon, LoadStoreIconData.IconState.StateOff, 0);
                    devicePortViewHolder.loadIcon(mIconCache, executable.getUid(),
                            LoadStoreIconData.IconType.DevicePortIcon, LoadStoreIconData.IconState.StateOn, 1);
                    devicePortViewHolder.seekBar.setVisibility(View.VISIBLE);
                    devicePortViewHolder.seekBar.setOnSeekBarChangeListener(this);
                    devicePortViewHolder.seekBar.setTag(-1);
                    devicePortViewHolder.seekBar.setMax(devicePort.max_value - devicePort.min_value);
                    break;
            }

        }

        // This has to be done more often
        switch (executable.getType()) {
            case TypeButton: {
                break;
            }
            case TypeToggle: {
                DevicePort devicePort = (DevicePort) executable;
                if (devicePort.current_value >= devicePort.max_value)
                    devicePortViewHolder.setBitmapOn();
                else
                    devicePortViewHolder.setBitmapOff();
                break;
            }
            case TypeRangedValue:
                DevicePort devicePort = (DevicePort) executable;
                devicePortViewHolder.seekBar.setTag(-1);
                devicePortViewHolder.seekBar.setProgress(devicePort.current_value - devicePort.min_value);
                devicePortViewHolder.seekBar.setTag(position);
                if (devicePort.current_value <= devicePort.min_value)
                    devicePortViewHolder.setBitmapOff();
                else
                    devicePortViewHolder.setBitmapOn();
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar view, int value, boolean b) {
        int position = (Integer) view.getTag();
        if (position == -1)
            return;
        ExecutableAdapterItem info = mItems.get(position);
        DevicePort devicePort = (DevicePort) info.getExecutable();
        devicePort.current_value = value + devicePort.min_value;
        info.command_value = devicePort.current_value;
        AppData.getInstance().execute(devicePort, info.command_value, null);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        getSource().setAutomaticUpdate(false);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        getSource().setAutomaticUpdate(true);
    }

    public void setEnableEditing(boolean enableEditing) {
        this.enableEditing = enableEditing;
    }
}
