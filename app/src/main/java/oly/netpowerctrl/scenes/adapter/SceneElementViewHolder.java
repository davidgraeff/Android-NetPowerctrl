package oly.netpowerctrl.scenes.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.executables.ExecutableType;
import oly.netpowerctrl.ui.widgets.SegmentedRadioGroup;

/**
 * Created by david on 10.05.15.
 */
public class SceneElementViewHolder extends RecyclerView.ViewHolder {
    public final View close;
    final View entry;
    final TextView title;
    final TextView subtitle;
    public int position;

    SegmentedRadioGroup rGroup;
    RadioButton r0;
    RadioButton r1;
    RadioButton r2;
    RadioButton r3;

    SeekBar seekBar;

    SceneElementViewHolder(View convertView, ExecutableType type) {
        super(convertView);

        entry = convertView.findViewById(R.id.item_layout);
        title = (TextView) convertView.findViewById(R.id.title);
        subtitle = (TextView) convertView.findViewById(R.id.subtitle);
        close = convertView.findViewById(R.id.outlet_list_close);

        switch (type) {
            case TypeToggle:
                rGroup = (SegmentedRadioGroup) convertView.findViewById(R.id.radioGroup);
                r0 = (RadioButton) convertView.findViewById(R.id.radioSwitchOff);
                r1 = (RadioButton) convertView.findViewById(R.id.radioSwitchOn);
                r2 = (RadioButton) convertView.findViewById(R.id.radioToggle);
                r3 = (RadioButton) convertView.findViewById(R.id.radioToggleMaster);
                break;
            case TypeRangedValue:
                seekBar = (SeekBar) convertView.findViewById(R.id.item_seekbar);
                break;
        }

        seekBar = (SeekBar) convertView.findViewById(R.id.item_seekbar);

    }
}
