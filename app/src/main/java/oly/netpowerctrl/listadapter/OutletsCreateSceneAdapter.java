package oly.netpowerctrl.listadapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.Iterator;

import oly.netpowerctrl.R;

public class OutletsCreateSceneAdapter extends OutletsBaseAdapter {

    private boolean temporary_ignore_positionRequest;
    private NotReachableUpdate notReachableObserver;

    private View.OnClickListener closeClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            mListItemMenu.onMenuItemClicked(view, (Integer) view.getTag());
        }
    };

    public OutletsCreateSceneAdapter(Context context) {
        super(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        OutletInfoAdditional info = all_outlets.get(position);
        outlet_res_id = R.layout.create_scene_outlet_list_switch;
        convertView = super.getView(position, convertView, parent);

        ImageView btnClose = (ImageView) convertView.findViewById(R.id.outlet_list_close);
        if (mListItemMenu != null) {
            btnClose.setVisibility(View.VISIBLE);
            btnClose.setTag(position);
            btnClose.setOnClickListener(closeClickListener);
        } else {
            btnClose.setVisibility(View.GONE);
        }

        return convertView;
    }

    public void switchAll(boolean state) {
        for (OutletInfoAdditional outlet_info : all_outlets) {
            outlet_info.enabled = true;
            outlet_info.oi.State = state;
        }
        notifyDataSetChanged();
    }

    public void toggleAll() {
        for (OutletInfoAdditional outlet_info : all_outlets) {
            outlet_info.enabled = true;
            //TODO toggle geht noch nicht
            //outlet_info.oi.State;
        }
        notifyDataSetChanged();
    }

    public void removeAll(OutletsCreateSceneAdapter adapter) {
        for (OutletInfoAdditional outlet_info : adapter.all_outlets) {
            Iterator<OutletInfoAdditional> i = all_outlets.iterator();
            while (i.hasNext()) {
                if (outlet_info.oi.equals(i.next().oi)) {
                    i.remove();
                }
            }

        }
    }
}
