package oly.netpowerctrl.scenes.adapter;

import android.widget.SeekBar;

import oly.netpowerctrl.executables.adapter.ExecutableAdapterItem;

/**
 * Created by david on 10.05.15.
 */
class SeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
    final ExecutableAdapterItem item;

    SeekBarChangeListener(ExecutableAdapterItem item) {
        this.item = item;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        item.setCommandValue(seekBar.getProgress());
    }
}
