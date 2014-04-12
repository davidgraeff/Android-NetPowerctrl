package oly.netpowerctrl.listadapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.Scene;
import oly.netpowerctrl.datastructure.SceneCollection;
import oly.netpowerctrl.dynamicgid.AbstractDynamicGridAdapter;

public class ScenesAdapter extends AbstractDynamicGridAdapter implements SceneCollection.IScenesUpdated {
    SceneCollection scenes;
    private LayoutInflater inflater;
    private boolean disableEditing;
    private IEditSceneRequest observer = null;
    private int outlet_res_id = R.layout.grid_icon_item;

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
        scenes.registerObserver(this);
    }

    public void setDisableEditing(boolean disableEditing) {
        this.disableEditing = disableEditing;
        notifyDataSetChanged();
    }

    public void setObserver(IEditSceneRequest observer) {
        this.observer = observer;
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

        if (convertView == null) {
            convertView = inflater.inflate(outlet_res_id, null);
            assert convertView != null;
            convertView.findViewById(R.id.subtitle).setVisibility(View.GONE);
        }

        final Scene data = scenes.getScene(position);

        final TextView tvName = (TextView) convertView.findViewById(R.id.title);
        tvName.setText(data.sceneName);

        ImageView image = (ImageView) convertView.findViewById(R.id.icon_bitmap);
        image.setImageBitmap(data.getBitmap());

        // For a grid view with a dedicated edit button (image) we use that for
        // setOnClickListener. In the other case we use the main icon for setOnClickListener.
        ImageView image_edit = (ImageView) convertView.findViewById(R.id.icon_edit);
        if (image_edit != null)
            image_edit.setVisibility(disableEditing ? View.GONE : View.VISIBLE);
        else
            image_edit = image;
        image_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (observer != null)
                    observer.editScene(position, view);
            }
        });
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

    public interface IEditSceneRequest {
        void editScene(int position, View view);
    }
}
