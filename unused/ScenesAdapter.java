package oly.netpowerctrl.scenes;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.UUID;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.IconDeferredLoadingThread;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.data.ObserverUpdateActions;
import oly.netpowerctrl.data.onCollectionUpdated;
import oly.netpowerctrl.utils.AnimationController;
import oly.netpowerctrl.utils.controls.onListItemElementClicked;

public class ScenesAdapter extends BaseAdapter implements onCollectionUpdated<SceneCollection, Scene> {
    private final SceneCollection scenes;
    private final LayoutInflater inflater;
    private final IconDeferredLoadingThread mIconCache;
    private WeakReference<AnimationController> mAnimationWeakReference = new WeakReference<>(null);
    private onListItemElementClicked mListContextMenu = null;
    private int outlet_res_id = R.layout.grid_item_icon;
    private boolean enableEditing;

    public ScenesAdapter(Context context, SceneCollection data, IconDeferredLoadingThread iconCache) {
        inflater = LayoutInflater.from(context);
        scenes = data;
        mIconCache = iconCache;
        scenes.registerObserver(this);
    }

    public void setAnimationController(AnimationController animationController) {
        mAnimationWeakReference = new WeakReference<>(animationController);
    }

    public void handleClick(int position, View view) {
        AnimationController a = mAnimationWeakReference.get();
        if (a != null)
            a.addHighlight(getItemId(position), R.id.text1);
        notifyDataSetChanged();
    }

    public int getLayoutRes() {
        return outlet_res_id;
    }

    public void setLayoutRes(int layout_res) {
        this.outlet_res_id = layout_res;
        notifyDataSetChanged();
    }

    public void setListContextMenu(onListItemElementClicked listItemMenu) {
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
        return scenes.get(position);
    }

    @Override
    public long getItemId(int position) {
        return scenes.get(position).id;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        final Scene data = scenes.get(position);

        ViewHolder current_viewHolder;
        if (convertView == null) {
            convertView = inflater.inflate(outlet_res_id, null);
            current_viewHolder = new ViewHolder(convertView, mListContextMenu);
            assert convertView != null;
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
                if (!enableEditing)
                    current_viewHolder.imageEdit.setVisibility(View.GONE);
                else {
                    current_viewHolder.imageEdit.setTag(position);
                    current_viewHolder.imageEdit.setOnClickListener(current_viewHolder);
                    current_viewHolder.imageEdit.setVisibility(View.VISIBLE);
                }
            } else {
                current_viewHolder.imageIcon.setTag(position);
                current_viewHolder.imageIcon.setOnClickListener(current_viewHolder);
            }

            current_viewHolder.loadIcon(mIconCache, data.uuid, LoadStoreIconData.IconType.SceneIcon,
                    LoadStoreIconData.IconState.StateUnknown, R.drawable.netpowerctrl, 0);
        }

        current_viewHolder.title.setText(data.sceneName);

        if (current_viewHolder.subtitle != null) {
            if (data.isMasterSlave()) {
                current_viewHolder.subtitle.setText("Master/Slave");
            } else
                current_viewHolder.subtitle.setText("");
        }

        return convertView;
    }

    @Override
    public void notifyDataSetChanged() {
        AnimationController a = mAnimationWeakReference.get();
        if (a != null)
            a.animate();
        super.notifyDataSetChanged();
    }

    @Override
    public boolean updated(SceneCollection sceneCollection, Scene scene, ObserverUpdateActions action) {
        notifyDataSetChanged();
        return true;
    }

    public boolean isEnableEditing() {
        return enableEditing;
    }

    public void setEnableEditing(boolean enableEditing) {
        this.enableEditing = enableEditing;
    }

    //ViewHolder pattern
    protected static class ViewHolder implements View.OnClickListener, IconDeferredLoadingThread.IconLoaded {
        final ImageView imageIcon;
        final ImageView imageEdit;
        //LinearLayout mainTextView;
        final View entry;
        final TextView title;
        final TextView subtitle;
        final int currentBitmapIndex = 0;
        final Drawable[] drawables = new Drawable[1];
        public int position;
        boolean isNew = true;
        private onListItemElementClicked mListContextMenu = null;

        ViewHolder(View convertView, onListItemElementClicked listContextMenu) {
            mListContextMenu = listContextMenu;
            imageIcon = (ImageView) convertView.findViewById(R.id.icon_bitmap);
            imageEdit = (ImageView) convertView.findViewById(R.id.icon_edit);
            entry = convertView.findViewById(R.id.item_layout);
            title = (TextView) convertView.findViewById(R.id.text1);
            subtitle = (TextView) convertView.findViewById(R.id.subtitle);
        }

        public void loadIcon(IconDeferredLoadingThread iconCache, UUID uuid, LoadStoreIconData.IconType iconType,
                             LoadStoreIconData.IconState state, int default_resource, int bitmapPosition) {
            iconCache.loadIcon(new IconDeferredLoadingThread.IconItem(imageIcon.getContext(),
                    uuid, iconType, state, default_resource, this, bitmapPosition));
        }

//        public void setCurrentBitmapIndex(int index) {
//            currentBitmapIndex = index;
//            if (drawables[index] != null)
//                imageIcon.setImageDrawable(drawables[index]);
//        }

        @Override
        public void onClick(View view) {
            mListContextMenu.onListItemElementClicked(view, position);
        }

        @Override
        public void setDrawable(Drawable bitmap, int position) {
            drawables[position] = bitmap;
            if (currentBitmapIndex == position)
                imageIcon.setImageDrawable(drawables[position]);
        }
    }
}
