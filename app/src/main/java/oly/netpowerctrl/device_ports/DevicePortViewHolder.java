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
    private final IconDeferredLoadingThread iconCache;
    public int position;
    boolean isNew = true;
    int layoutChangeId;
    int currentBitmapIndex = -1;
    private ListItemMenu mListContextMenu = null;

    DevicePortViewHolder(View convertView, ListItemMenu listContextMenu,
                         IconDeferredLoadingThread iconCache, int layoutChangeId) {
        this.layoutChangeId = layoutChangeId;
        mListContextMenu = listContextMenu;
        this.iconCache = iconCache;
        imageIcon = (ImageView) convertView.findViewById(R.id.icon_bitmap);
        imageEdit = (ImageView) convertView.findViewById(R.id.icon_edit);
        seekBar = (SeekBar) convertView.findViewById(R.id.item_seekbar);
        //mainTextView = (LinearLayout) convertView.findViewById(R.id.outlet_list_text);
        entry = convertView.findViewById(R.id.item_layout);
        title = (TextView) convertView.findViewById(R.id.text1);
        subtitle = (TextView) convertView.findViewById(R.id.subtitle);
    }

    boolean isStillValid(int layoutChangeId) {
        boolean hasChanged = this.layoutChangeId == layoutChangeId;
        this.layoutChangeId = layoutChangeId;
        return hasChanged;
    }

    @SuppressWarnings("SameParameterValue")
    public void loadIcon(UUID uuid, Icons.IconType iconType, Icons.IconState state, int default_resource, int bitmapPosition) {
        iconCache.loadIcon(new IconDeferredLoadingThread.IconItem(imageIcon.getContext(),
                uuid, iconType, state, default_resource, this, bitmapPosition));
    }

    public void setCurrentBitmapIndex(int index) {
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
