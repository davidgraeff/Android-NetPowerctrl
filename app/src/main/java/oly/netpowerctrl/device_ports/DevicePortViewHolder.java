package oly.netpowerctrl.device_ports;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.utils.IconDeferredLoadingThread;
import oly.netpowerctrl.utils.Icons;
import oly.netpowerctrl.utils.ListItemMenu;

/**
 * An item in a devicePort adapter. Used for implementing the ViewHolder pattern.
 */
class DevicePortViewHolder implements View.OnClickListener, IconDeferredLoadingThread.IconLoaded {
    final ImageView imageIcon;
    final ImageView imageEdit;
    //LinearLayout mainTextView;
    final View entry;
    final SeekBar seekBar;
    final TextView title;
    final TextView subtitle;
    final Drawable[] drawables = new Drawable[2];
    public int position;
    boolean isNew = true;
    int layoutChangeId;
    int currentBitmapIndex = -1;
    private ListItemMenu mListContextMenu = null;

    DevicePortViewHolder(View convertView, ListItemMenu listContextMenu, int layoutChangeId, boolean isHeader) {
        this.layoutChangeId = layoutChangeId;
        mListContextMenu = listContextMenu;

        entry = convertView.findViewById(R.id.item_layout);
        title = (TextView) convertView.findViewById(R.id.text1);
        imageEdit = (ImageView) convertView.findViewById(R.id.icon_edit);

        View line = convertView.findViewById(R.id.line);
        if (line != null)
            line.setVisibility(isHeader ? View.INVISIBLE : View.VISIBLE);

        if (isHeader) {
            subtitle = null;
            imageIcon = null;
            seekBar = null;
            return;
        }

        subtitle = (TextView) convertView.findViewById(R.id.subtitle);
        imageIcon = (ImageView) convertView.findViewById(R.id.icon_bitmap);
        seekBar = (SeekBar) convertView.findViewById(R.id.item_seekbar);
    }

    boolean isStillValid(int layoutChangeId) {
        boolean hasChanged = this.layoutChangeId == layoutChangeId;
        this.layoutChangeId = layoutChangeId;
        return hasChanged;
    }

    @SuppressWarnings("SameParameterValue")
    public void loadIcon(IconDeferredLoadingThread iconCache, UUID uuid,
                         Icons.IconType iconType, Icons.IconState state,
                         int default_resource, int bitmapPosition) {
        iconCache.loadIcon(new IconDeferredLoadingThread.IconItem(imageIcon.getContext(),
                uuid, iconType, state, default_resource, this, bitmapPosition));
    }

    public void setCurrentBitmapIndex(int index) {
        if (index == currentBitmapIndex)
            return;
        currentBitmapIndex = index;
        if (drawables[index] != null) {
            imageIcon.setImageDrawable(drawables[index]);
        }
    }

    @Override
    public void onClick(View view) {
        mListContextMenu.onMenuItemClicked(view, position);
    }

    @Override
    public void setDrawable(Drawable bitmap, int position) {
        drawables[position] = bitmap;
        if (currentBitmapIndex == position)
            imageIcon.setImageDrawable(drawables[position]);
    }
}
