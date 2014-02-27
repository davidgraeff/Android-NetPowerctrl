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

public class ScenesListAdapter extends AbstractDynamicGridAdapter implements SceneCollection.IScenesUpdated {
    SceneCollection scenes;
    private LayoutInflater inflater;
    private boolean disableEditing;
    private IEditSceneRequest observer = null;

    public ScenesListAdapter(Context context, SceneCollection data) {
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
            convertView = inflater.inflate(R.layout.scene_list_item, null);
        }

        final Scene data = scenes.getScene(position);

        assert convertView != null;
        final TextView tvName = (TextView) convertView.findViewById(R.id.scene_list_name);
        tvName.setText(data.sceneName);

        ImageView image = (ImageView) convertView.findViewById(R.id.scene_icon_bitmap);
        image.setImageBitmap(data.getBitmap());

        image = (ImageView) convertView.findViewById(R.id.scene_icon_edit);
        image.setVisibility(disableEditing ? View.GONE : View.VISIBLE);
        image.setOnClickListener(new View.OnClickListener() {
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
