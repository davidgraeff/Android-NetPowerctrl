package oly.netpowerctrl.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RadioButton;

import java.util.HashMap;
import java.util.Random;

public class SoftRadioButton extends RadioButton {

    private static HashMap<String, SoftRadioGroup> GROUP_MAPPINGS = new HashMap<>();
    private String mGroupName;

    public SoftRadioButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        addToGroup(attrs);
    }

    public SoftRadioGroup getRadioGroup() {
        return GROUP_MAPPINGS.get(mGroupName);
    }

    private void addToGroup(AttributeSet attrs) {
        for (int i = 0; i < attrs.getAttributeCount(); i++) {
            if (attrs.getAttributeName(i).equals("group")) {
                String groupName = attrs.getAttributeValue(i);
                SoftRadioGroup group;
                if ((group = GROUP_MAPPINGS.get(groupName)) != null) {
                    // RadioGroup already exists
                    group.addView(this);
                    setOnClickListener(group);
                    mGroupName = groupName;

                } else {
                    // this is the first RadioButton in the RadioGroup
                    group = new SoftRadioGroup();
                    group.addView(this);
                    mGroupName = groupName;
                    setOnClickListener(group);

                    GROUP_MAPPINGS.put(groupName, group);
                }
                return;
            }
        }
        // group is not specified in the layout xml. Let's generate a random
        // RadioGroup
        SoftRadioGroup group = new SoftRadioGroup();
        group.addView(this);
        Random rn = new Random();
        String groupName;
        do {
            groupName = Integer.toString(rn.nextInt());
        } while (GROUP_MAPPINGS.containsKey(groupName));
        GROUP_MAPPINGS.put(groupName, group);
        mGroupName = groupName;
        setOnClickListener(group);

    }

}