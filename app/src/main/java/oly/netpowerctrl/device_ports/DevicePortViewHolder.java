package oly.netpowerctrl.device_ports;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.IconDeferredLoadingThread;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.utils.controls.ListItemMenu;

/**
 * An item in a devicePort adapter. Used for implementing the ViewHolder pattern.
 */
class DevicePortViewHolder implements View.OnClickListener, IconDeferredLoadingThread.IconLoaded {
    final ImageView imageIcon;
    final ImageView imageEdit;
    //LinearLayout mainTextView;
    final View entry;
    final SeekBar seekBar;
    final ProgressBar progress;
    final TextView title;
    final TextView subtitle;
    final View line;
    final Drawable[] drawables = new Drawable[2];
    public int position;
    public Animation animation = null;
    boolean isNew = true;
    int layoutChangeId;
    int currentBitmapIndex = -1;
    private ListItemMenu mListContextMenu = null;

    DevicePortViewHolder(View convertView, ListItemMenu listContextMenu, int layoutChangeId,
                         DevicePortAdapterItem.groupTypeEnum groupTypeEnum) {
        this.layoutChangeId = layoutChangeId;
        mListContextMenu = listContextMenu;

        entry = convertView.findViewById(R.id.item_layout);
        title = (TextView) convertView.findViewById(R.id.text1);
        imageEdit = (ImageView) convertView.findViewById(R.id.icon_edit);

        if (groupTypeEnum == DevicePortAdapterItem.groupTypeEnum.GROUP_TYPE ||
                groupTypeEnum == DevicePortAdapterItem.groupTypeEnum.GROUP_SPAN_TYPE) {
            subtitle = null;
            imageIcon = null;
            seekBar = null;
            progress = null;
            line = convertView.findViewById(R.id.line);
            return;

        }

        line = null;
        subtitle = (TextView) convertView.findViewById(R.id.subtitle);
        imageIcon = (ImageView) convertView.findViewById(R.id.icon_bitmap);
        seekBar = (SeekBar) convertView.findViewById(R.id.item_seekbar);
        progress = (ProgressBar) convertView.findViewById(R.id.progress);

        if (groupTypeEnum == DevicePortAdapterItem.groupTypeEnum.PRE_GROUP_FILL_ELEMENT_TYPE) {
            entry.setVisibility(View.INVISIBLE);
        }
    }

    boolean isStillValid(int layoutChangeId) {
        boolean hasChanged = this.layoutChangeId == layoutChangeId;
        this.layoutChangeId = layoutChangeId;
        return hasChanged;
    }

    @SuppressWarnings("SameParameterValue")
    public void loadIcon(IconDeferredLoadingThread iconCache, UUID uuid,
                         LoadStoreIconData.IconType iconType, LoadStoreIconData.IconState state,
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
