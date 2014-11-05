package oly.netpowerctrl.ui;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioButton;

import java.util.ArrayList;

public class SoftRadioGroup implements OnClickListener {

    private ArrayList<RadioButton> buttons = new ArrayList<>();

    public void addView(RadioButton button) {
        buttons.add(button);
    }

    @Override
    public void onClick(View v) {
        for (RadioButton button : buttons) {
            button.setChecked(false);
        }
        RadioButton button = (RadioButton) v;
        button.setChecked(true);
    }

    public RadioButton getCheckedRadioButton() {
        for (RadioButton button : buttons) {
            if (button.isChecked())
                return button;
        }
        return null;
    }

    public int getCheckedRadioButtonIndex() {
        int i = -1;
        for (RadioButton button : buttons) {
            ++i;
            if (button.isChecked())
                return i;
        }
        return -1;
    }

    public int getChildCount() {
        return buttons.size();
    }

    public RadioButton getChildAt(int i) {
        return buttons.get(i);
    }

    public void check(int index) {
        for (RadioButton b : buttons)
            b.setChecked(false);
        buttons.get(index).setChecked(true);
    }

    public void check(SoftRadioButton button) {
        for (RadioButton b : buttons) {
            b.setChecked(b == button);
        }
    }

}