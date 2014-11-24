package oly.netpowerctrl.executables;

import android.view.View;
import android.widget.SeekBar;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.IconDeferredLoadingThread;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.device_base.executables.Executable;

public class ExecuteAdapter extends ExecutablesBaseAdapter implements
        SeekBar.OnSeekBarChangeListener {

    // We block updates while moving the range slider
    private static final String TAG = "ExecuteAdapter";
    private boolean enableEditing;

    public ExecuteAdapter(ExecutablesSourceBase source,
                          IconDeferredLoadingThread iconCache) {
        super(source, iconCache, true);
        setLayoutRes(R.layout.list_item_executable);
    }

    @Override
    public void onBindViewHolder(ExecutableViewHolder executableViewHolder, int position) {
        boolean isNew = executableViewHolder.isNew;
        super.onBindViewHolder(executableViewHolder, position);

        ExecutableAdapterItem item = mItems.get(position);
        Executable executable = item.getExecutable();

        // Not our business, if port is null
        if (executable == null) {
            if (isNew && executableViewHolder.imageEdit != null) {
                executableViewHolder.imageEdit.setVisibility(View.INVISIBLE);
            }
            return;
        }

        // We do this only once, if the viewHolder is new
        if (isNew) {
            // We use the tools icon for the context menu.
            if (executableViewHolder.imageEdit != null) {
                executableViewHolder.imageEdit.setVisibility(enableEditing ? View.VISIBLE : View.GONE);
            }

            //current_viewHolder.mainTextView.setTag(position);
            switch (executable.getType()) {
                case TypeToggle: {
                    executableViewHolder.seekBar.setVisibility(View.GONE);
                    executableViewHolder.loadIcon(mIconCache, executable.getUid(),
                            LoadStoreIconData.IconState.StateOff, 0);
                    executableViewHolder.loadIcon(mIconCache, executable.getUid(),
                            LoadStoreIconData.IconState.StateOn, 1);
                    break;
                }
                case TypeStateless: {
                    executableViewHolder.loadIcon(mIconCache, executable.getUid(),
                            LoadStoreIconData.IconState.OnlyOneState, 0);
                    executableViewHolder.seekBar.setVisibility(View.GONE);
                    executableViewHolder.setBitmapOff();
                    break;
                }
                case TypeRangedValue:
                    DevicePort devicePort = (DevicePort) executable;
                    executableViewHolder.loadIcon(mIconCache, executable.getUid(),
                            LoadStoreIconData.IconState.StateOff, 0);
                    executableViewHolder.loadIcon(mIconCache, executable.getUid(),
                            LoadStoreIconData.IconState.StateOn, 1);
                    executableViewHolder.seekBar.setVisibility(View.VISIBLE);
                    executableViewHolder.seekBar.setOnSeekBarChangeListener(this);
                    executableViewHolder.seekBar.setTag(-1);
                    executableViewHolder.seekBar.setMax(devicePort.max_value - devicePort.min_value);
                    break;
            }

        }

        // This has to be done more often
        switch (executable.getType()) {
            case TypeStateless: {
                break;
            }
            case TypeToggle: {
                if (executable.getCurrentValue() >= executable.getMaximumValue())
                    executableViewHolder.setBitmapOn();
                else
                    executableViewHolder.setBitmapOff();
                break;
            }
            case TypeRangedValue:
                DevicePort devicePort = (DevicePort) executable;
                executableViewHolder.seekBar.setTag(-1);
                executableViewHolder.seekBar.setProgress(devicePort.current_value - devicePort.min_value);
                executableViewHolder.seekBar.setTag(position);
                if (devicePort.current_value <= devicePort.min_value)
                    executableViewHolder.setBitmapOff();
                else
                    executableViewHolder.setBitmapOn();
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
