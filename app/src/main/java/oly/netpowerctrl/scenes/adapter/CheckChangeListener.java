package oly.netpowerctrl.scenes.adapter;

import android.widget.RadioGroup;

import oly.netpowerctrl.R;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.executables.adapter.ExecutableAdapterItem;

/**
 * Created by david on 10.05.15.
 */
class CheckChangeListener implements RadioGroup.OnCheckedChangeListener {
    final ExecutableAdapterItem item;
    final ChangeMasterInterface changeMaster;

    CheckChangeListener(ExecutableAdapterItem item, ChangeMasterInterface changeMaster) {
        this.item = item;
        this.changeMaster = changeMaster;
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int i) {
        int command_value = item.getCommandValue();

        ExecutableAdapterItem master = changeMaster.getMaster();

        switch (i) {
            case R.id.radioSwitchOff:
                if (item == master) master = null;
                command_value = Executable.OFF;
                break;
            case R.id.radioSwitchOn:
                if (item == master) master = null;
                command_value = Executable.ON;
                break;
            case R.id.radioToggleMaster:
                master = item;
                command_value = Executable.TOGGLE;
                break;
            case R.id.radioToggle:
                if (item == master) master = null;
                command_value = Executable.TOGGLE;
                break;
        }

        item.setCommandValue(command_value);

        changeMaster.setMaster(master);
    }
}
