package oly.netpowerctrl.executables;

import android.widget.SeekBar;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.IconDeferredLoadingThread;
import oly.netpowerctrl.device_base.device.DevicePort;

public class ExecuteAdapter extends ExecutablesBaseAdapter implements
        SeekBar.OnSeekBarChangeListener {

    // We block updates while moving the range slider
    private static final String TAG = "ExecuteAdapter";

    public ExecuteAdapter(ExecutablesSourceBase source,
                          IconDeferredLoadingThread iconCache) {
        super(source, iconCache, true);
        setLayoutRes(R.layout.list_item_executable);
    }

    @Override
    public void onBindViewHolder(ExecutableViewHolder executableViewHolder, int position) {
        super.onBindViewHolder(executableViewHolder, position);
        executableViewHolder.setSeekBarListener(this);
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
}
