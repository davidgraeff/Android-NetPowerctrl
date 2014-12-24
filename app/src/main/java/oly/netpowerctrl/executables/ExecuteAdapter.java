package oly.netpowerctrl.executables;

import android.graphics.drawable.Drawable;
import android.widget.SeekBar;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.IconDeferredLoadingThread;
import oly.netpowerctrl.device_base.device.DevicePort;
import oly.netpowerctrl.main.App;

public class ExecuteAdapter extends ExecutablesBaseAdapter implements
        SeekBar.OnSeekBarChangeListener {

    // We block updates while moving the range slider
    private static final String TAG = "ExecuteAdapter";
    private final Drawable editModeDrawable;
    private boolean editMode = false;

    public ExecuteAdapter(AdapterSource source,
                          IconDeferredLoadingThread iconCache) {
        super(source, iconCache, true);
        setLayoutRes(R.layout.list_item_executable);
        editModeDrawable = App.instance.getResources().getDrawable(android.R.drawable.ic_menu_edit);
    }

    @Override
    public void onBindViewHolder(ExecutableViewHolder executableViewHolder, int position) {
        executableViewHolder.setShowImages(!editMode);
        executableViewHolder.title.getPaint().setUnderlineText(editMode);
        if (editMode && executableViewHolder.imageIcon != null)
            executableViewHolder.imageIcon.setImageDrawable(editModeDrawable);

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
        getSource().getAppData().execute(devicePort, info.command_value, null);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        int position = (Integer) seekBar.getTag();
        if (position == -1)
            return;
        ExecutableAdapterItem info = mItems.get(position);
        getSource().ignoreUpdates(info.getExecutable());
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        getSource().ignoreUpdates(null);
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void setEditMode(boolean editMode) {
        if (this.editMode == editMode) return;
        this.editMode = editMode;
        notifyDataSetChanged();
    }
}
