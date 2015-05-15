package oly.netpowerctrl.data.graphic;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple array adapter that additionally provide an icon per entry.
 */
class ArrayAdapterWithIcons extends RecyclerView.Adapter<ArrayAdapterWithIcons.ViewHolder> {
    final static int resource = android.R.layout.select_dialog_item;
    final static int textViewResourceId = android.R.id.text1;
    public final List<Item> items = new ArrayList<>();
    private int dp5;

    @SuppressWarnings("SameParameterValue")
    public ArrayAdapterWithIcons(Context context) {
        //Add margin between image and text (support various screen densities)
        dp5 = (int) (5 * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(resource, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        //Put the image on the TextView
        Item item = items.get(position);

        if (item.text != null)
            holder.tv.setText(item.text);

        holder.tv.setCompoundDrawablesWithIntrinsicBounds(item.icon, null, null, null);

        if (item.icon != null) {
            holder.tv.setCompoundDrawablePadding(dp5);
        } else {
            holder.tv.setCompoundDrawablePadding(0);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tv;

        public ViewHolder(View itemView) {
            super(itemView);
            tv = (TextView) itemView.findViewById(textViewResourceId);
        }
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
