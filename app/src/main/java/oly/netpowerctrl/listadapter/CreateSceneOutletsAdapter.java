package oly.netpowerctrl.listadapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.application_state.NetpowerctrlApplication;
import oly.netpowerctrl.datastructure.DeviceInfo;
import oly.netpowerctrl.datastructure.OutletInfo;
import oly.netpowerctrl.datastructure.SceneOutlet;
import oly.netpowerctrl.utils.ListItemMenu;

public class CreateSceneOutletsAdapter extends BaseAdapter implements ListAdapter, View.OnClickListener {
    private final Context context;
    private List<SceneOutlet> all_outlets;
    private LayoutInflater inflater;
    private ListItemMenu listItemMenu = null;

    private static class AnimationListenerWithRadioGroup implements Animation.AnimationListener {
        private RadioGroup r;
        private boolean showIt;

        public AnimationListenerWithRadioGroup(RadioGroup r, boolean showIt) {
            this.r = r;
            this.showIt = showIt;
        }

        @Override
        public void onAnimationStart(Animation animation) {
            if (showIt)
                r.setVisibility(View.VISIBLE);
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            if (!showIt)
                r.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }

    public static CreateSceneOutletsAdapter createByConfiguredDevices(Context context) {
        CreateSceneOutletsAdapter o = new CreateSceneOutletsAdapter(context);
        // Enumerate all configured devices and outlets and create SceneOutlets
        for (DeviceInfo device : NetpowerctrlApplication.getDataController().configuredDevices) {
            for (OutletInfo oi : device.Outlets) {
                if (oi.Disabled)
                    continue;
                oi.device = device;
                // Create SceneOutlet and set state to Toggle
                SceneOutlet so = SceneOutlet.fromOutletInfo(oi, false);
                so.state = SceneOutlet.ON;
                o.all_outlets.add(so);
            }
        }
        return o;
    }

    public static CreateSceneOutletsAdapter createByOutletCommands(Context context, List<SceneOutlet> commands) {
        CreateSceneOutletsAdapter o = new CreateSceneOutletsAdapter(context);
        for (DeviceInfo device : NetpowerctrlApplication.getDataController().configuredDevices) {
            for (OutletInfo oi : device.Outlets) {
                oi.device = device;
                if (oi.Disabled)
                    continue;
                SceneOutlet c = SceneOutlet.fromOutletInfo(oi, false);
                int i = commands.indexOf(c);
                if (i != -1) {
                    c.enabled = true;
                    c.state = commands.get(i).state;
                }
                o.all_outlets.add(c);
            }
        }
        return o;
    }

    private CreateSceneOutletsAdapter(Context context) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        all_outlets = new ArrayList<SceneOutlet>();
    }

    public int getCount() {
        return all_outlets.size();
    }

    public Object getItem(int position) {
        return all_outlets.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        boolean newValue = false;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.create_scene_outlet_list_item, null);
            newValue = true;
        }
        SceneOutlet command = all_outlets.get(position);
        assert convertView != null;
        TextView tv = (TextView) convertView.findViewById(R.id.outlet_list_text);
        tv.setOnClickListener(this);
        tv.setText(command.description);
        tv.setTag(position);
        tv.setTypeface(null, command.enabled ? Typeface.BOLD : Typeface.NORMAL);

        View v;
        v = convertView.findViewById(R.id.radio0);
        v.setOnClickListener(radioClick);
        v.setTag(position);

        v = convertView.findViewById(R.id.radio1);
        v.setOnClickListener(radioClick);
        v.setTag(position);

        v = convertView.findViewById(R.id.radio2);
        v.setOnClickListener(radioClick);
        v.setTag(position);

        RadioGroup r = (RadioGroup) convertView.findViewById(R.id.radioGroup);
        //r.setVisibility(command.enabled ? View.VISIBLE : View.GONE);
        if (r.getVisibility() == View.VISIBLE && !command.enabled) {
            r.clearAnimation();
            Animation a = AnimationUtils.loadAnimation(context, R.anim.animate_out);
            assert a != null;
            a.setAnimationListener(new AnimationListenerWithRadioGroup(r, false));
            r.startAnimation(a);
        } else if (r.getVisibility() == View.INVISIBLE && command.enabled) {
            if (!newValue) {
                r.clearAnimation();
                Animation a = AnimationUtils.loadAnimation(context, R.anim.animate_in);
                assert a != null;
                a.setAnimationListener(new AnimationListenerWithRadioGroup(r, true));
                r.startAnimation(a);
            } else {
                r.setVisibility(View.VISIBLE);
            }
        }
        r.check((command.state == 0) ? R.id.radio0 : ((command.state == 1) ? R.id.radio1 : R.id.radio2));
        return convertView;
    }

    public ArrayList<SceneOutlet> getCheckedItems() {
        ArrayList<SceneOutlet> output = new ArrayList<SceneOutlet>();
        for (SceneOutlet c : all_outlets) {
            if (c.enabled) {
                output.add(c);
            }
        }
        return output;
    }

    private View.OnClickListener radioClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int position = (Integer) view.getTag();
            SceneOutlet info = all_outlets.get(position);
            int buttonId = view.getId();
            info.state = (buttonId == R.id.radio0) ? 0 : ((buttonId == R.id.radio1) ? 1 : 2); //1:off;2:on;3:toggle
            if (listItemMenu != null)
                listItemMenu.onMenuItemClicked(view, position);
        }
    };

    public void setListItemMenu(ListItemMenu dce) {
        listItemMenu = dce;
    }

    @Override
    public void onClick(View view) {
        int position = (Integer) view.getTag();
        SceneOutlet info = all_outlets.get(position);
        info.enabled = !info.enabled;
        notifyDataSetChanged();
        if (listItemMenu != null)
            listItemMenu.onMenuItemClicked(view, position);
    }

    public void switchAll(int state) {
        for (SceneOutlet so : all_outlets) {
            if (state == -1)
                so.enabled = false;
            else {
                so.enabled = true;
                so.state = state;
            }
        }
        notifyDataSetChanged();
        if (listItemMenu != null)
            listItemMenu.onMenuItemClicked(null, 0);
    }
}
