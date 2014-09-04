package oly.netpowerctrl.utils.controls;

/*
 * Copyright (C) 2011 Make Ramen, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.RadioGroup;

import oly.netpowerctrl.R;

public class SegmentedRadioGroup extends RadioGroup {

    public SegmentedRadioGroup(Context context) {
        super(context);
    }

    public SegmentedRadioGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        changeButtonsImages();
    }

    private void changeButtonsImages() {
        int count = super.getChildCount();

        TypedValue typedValue = new TypedValue();
        //noinspection ConstantConditions
        Resources.Theme theme = getContext().getTheme();
        assert theme != null;

        if (count > 1) {
            theme.resolveAttribute(R.attr.segment_radio_left, typedValue, true);
            //noinspection ConstantConditions
            super.getChildAt(0).setBackgroundResource(typedValue.resourceId);
            theme.resolveAttribute(R.attr.segment_radio_middle, typedValue, true);
            for (int i = 1; i < count - 1; i++) {
                //noinspection ConstantConditions
                super.getChildAt(i).setBackgroundResource(typedValue.resourceId);
            }
            theme.resolveAttribute(R.attr.segment_radio_right, typedValue, true);
            //noinspection ConstantConditions
            super.getChildAt(count - 1).setBackgroundResource(typedValue.resourceId);
        } else if (count == 1) {
            theme.resolveAttribute(R.attr.segment_button, typedValue, true);
            //noinspection ConstantConditions
            super.getChildAt(0).setBackgroundResource(typedValue.resourceId);
        }
    }
}