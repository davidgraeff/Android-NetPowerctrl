package oly.netpowerctrl.listadapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.datastructure.SceneCollection;
import oly.netpowerctrl.dynamicgid.AbstractDynamicGridAdapter;
import oly.netpowerctrl.utils.IconDeferredLoadingThread;
import oly.netpowerctrl.utils.Icons;
import oly.netpowerctrl.utils.ListItemMenu;

public class ScenesAdapter extends AbstractDynamicGridAdapter implements SceneCollection.IScenesUpdated {
    SceneCollection scenes;
    private LayoutInflater inflater;
    private boolean disableEditing;
    private ListItemMenu mListContextMenu = null;
    private int outlet_res_id = R.layout.grid_icon_item;
    protected ViewHolder current_viewHolder;
    private IconDeferredLoadingThread iconCache = new IconDeferredLoadingThread();

    //ViewHolder pattern
    protected static class ViewHolder implements View.OnClickListener, IconDeferredLoadingThread.IconLoaded {
        ImageView imageIcon;
        ImageView imageEdit;
        //LinearLayout mainTextView;
        View entry;
        TextView title;
        TextView subtitle;
        boolean isNew = true;

        int currentBitmapIndex = 0;
        Drawable[] drawables = new Drawable[1];
        public int position;
        private ListItemMenu mListContextMenu = null;
        private IconDeferredLoadingThread iconCache;

        ViewHolder(View convertView, ListItemMenu listContextMenu, IconDeferredLoadingThread iconCache) {
            mListContextMenu = listContextMenu;
            this.iconCache = iconCache;
            imageIcon = (ImageView) convertView.findViewById(R.id.icon_bitmap);
            imageEdit = (ImageView) convertView.findViewById(R.id.icon_edit);
            entry = convertView.findViewById(R.id.item_layout);
            title = (TextView) convertView.findViewById(R.id.title);
            subtitle = (TextView) convertView.findViewById(R.id.subtitle);
        }

        public void loadIcon(UUID uuid, Icons.IconType iconType, Icons.IconState state, int default_resource, int bitmapPosition) {
            iconCache.loadIcon(new IconDeferredLoadingThread.IconItem(imageIcon.getContext(),
                    uuid, iconType, state, default_resource, this, bitmapPosition));
        }

        public void setCurrentBitmapIndex(int index) {
            currentBitmapIndex = index;
            if (drawables[index] != null)
                imageIcon.setImageDrawable(drawables[index]);
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

    public int getLayoutRes() {
        return outlet_res_id;
    }

    public void setLayoutRes(int layout_res) {
        this.outlet_res_id = layout_res;
        notifyDataSetChanged();
    }

    public ScenesAdapter(Context context, SceneCollection data) {
        inflater = LayoutInflater.from(context);
        scenes = data;
        iconCache.start();
        scenes.registerObserver(this);
    }

    public void setDisableEditing(boolean disableEditing) {
        this.disableEditing = disableEditing;
        notifyDataSetChanged();
    }

    public void setListContextMenu(ListItemMenu listItemMenu) {
        this.mListContextMenu = listItemMenu;
    }

    @Override
    public int getCount() {
        return scenes.length();
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public Object getItem(int position) {
        return scenes.getScene(position);
    }

    @Override
    public long getItemId(int position) {
        return scenes.getScene(position).id;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        final Scene data = scenes.getScene(position);

        if (convertView == null) {
            convertView = inflater.inflate(outlet_res_id, null);
            current_viewHolder = new ViewHolder(convertView, mListContextMenu, iconCache);
            convertView.setTag(current_viewHolder);
        } else {
            current_viewHolder = (ViewHolder) convertView.getTag();
            current_viewHolder.isNew = false;
        }
        current_viewHolder.position = position;

        if (current_viewHolder.isNew) {
            // For a grid view with a dedicated edit button (image) we use that for
            // setOnClickListener. In the other case we use the main icon for setOnClickListener.
            if (current_viewHolder.imageEdit != null) {
                current_viewHolder.imageEdit.setTag(position);
                current_viewHolder.imageEdit.setOnClickListener(current_viewHolder);
            } else {
                current_viewHolder.imageIcon.setTag(position);
                current_viewHolder.imageIcon.setOnClickListener(current_viewHolder);
            }

            current_viewHolder.loadIcon(data.uuid, Icons.IconType.SceneIcon,
                    Icons.IconState.StateUnknown, R.drawable.netpowerctrl, 0);
        }

        current_viewHolder.title.setText(data.sceneName);
        if (data.isMasterSlave()) {
            current_viewHolder.subtitle.setText("Master/Slave");
        }

        // For a grid view with a dedicated edit button (image) we use that for
        // setOnClickListener. In the other case we use the main icon for setOnClickListener.
        ImageView image_edit = current_viewHolder.imageEdit;
        if (image_edit != null)
            image_edit.setVisibility(disableEditing ? View.GONE : View.VISIBLE);
        return convertView;
    }

    @Override
    public void reorderItems(int originalPosition, int newPosition) {
        scenes.reorderItems(originalPosition, newPosition, false);
    }

    @Override
    public void finishedReordering() {
        scenes.saveScenes();
    }

    @Override
    public void scenesUpdated(boolean addedOrRemoved) {
        notifyDataSetChanged();
    }
//
//    public interface IEditSceneRequest {
//        void editScene(int position, View view);
//    }
}
