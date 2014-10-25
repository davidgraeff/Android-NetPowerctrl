package oly.netpowerctrl.data;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * A simple array adapter that additionally provide an icon per entry.
 */
class ArrayAdapterWithIcons extends ArrayAdapter<ArrayAdapterWithIcons.Item> {
    public final List<Item> items;

    @SuppressWarnings("SameParameterValue")
    public ArrayAdapterWithIcons(Context context, int resource, int textViewResourceId, List<Item> objects) {
        super(context, resource, textViewResourceId, objects);
        items = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //User super class to create the View
        View v = super.getView(position, convertView, parent);
        assert v != null;
        TextView tv = (TextView) v.findViewById(android.R.id.text1);
        //Put the image on the TextView
        tv.setCompoundDrawablesWithIntrinsicBounds(items.get(position).icon, null, null, null);

        if (items.get(position).icon != null) {
            //Add margin between image and text (support various screen densities)
            int dp5 = (int) (5 * getContext().getResources().getDisplayMetrics().density + 0.5f);
            tv.setCompoundDrawablePadding(dp5);
        } else {
            tv.setCompoundDrawablePadding(0);
        }

        return v;
    }

    public static class Item {
        public final String text;
        public final Drawable icon;

        public Item(String text, Drawable icon) {
            this.text = text;
            this.icon = icon;
        }

        @Override
        public String toString() {
            return text;
        }
    }
}