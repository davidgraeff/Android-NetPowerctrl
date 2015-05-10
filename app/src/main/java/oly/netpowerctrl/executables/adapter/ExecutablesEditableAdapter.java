package oly.netpowerctrl.executables.adapter;

import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.widget.SeekBar;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.graphic.IconDeferredLoadingThread;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.main.App;

public class ExecutablesEditableAdapter extends ExecutablesAdapter implements
        SeekBar.OnSeekBarChangeListener {

    // We block updates while moving the range slider
    @SuppressWarnings("unused")
    private static final String TAG = "ExecuteAdapter";
    private final Drawable editModeDrawable;
    private boolean editMode = false;

    public ExecutablesEditableAdapter(AdapterSource source,
                                      IconDeferredLoadingThread iconCache) {
        super(source, iconCache, R.layout.list_item_executable);
        editModeDrawable = ContextCompat.getDrawable(App.instance, android.R.drawable.ic_menu_edit);
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
        ExecutableAdapterItem info = mSource.mItems.get(position);
        Executable executable = info.getExecutable();
        executable.current_value = value + executable.min_value;
        info.command_value = executable.current_value;
        executable.execute(getSource().getPluginService(), info.command_value, null);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        int position = (Integer) seekBar.getTag();
        if (position == -1)
            return;
        ExecutableAdapterItem info = mSource.mItems.get(position);
        mSource.ignoreUpdates(info.getExecutable());
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
