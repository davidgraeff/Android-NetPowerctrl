package oly.netpowerctrl.plugins;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;

/**
 * For displaying DeviceInfo fields in a ListView
 */
public class PluginValuesAdapter extends BaseAdapter {
    public interface OnValueChanged {
        void onIntValueChanged(int id, int value);

        void onBooleanValueChanged(int id, boolean value);

        void onAction(int id);
    }

    public class RemoteEntry {
        public final int id;
        public final String name;
        public final boolean isHeader;
        public int type = 0;
        public static final int TYPE_HEADER = 1;

        RemoteEntry(int id, String value_name) {
            this.id = id;
            this.name = value_name;
            this.isHeader = false;
        }

        RemoteEntry(int id, String value_name, boolean isHeader) {
            this.id = id;
            this.name = value_name;
            this.isHeader = isHeader;
            this.type = TYPE_HEADER;
        }
    }

    public class RemoteAction extends RemoteEntry implements View.OnClickListener {
        public static final int TYPE_ACTION = 2;

        RemoteAction(int index, String value_name) {
            super(index, value_name);
            this.type = TYPE_ACTION;
        }

        @Override
        public void onClick(View view) {
            if (valueChangedObserver != null)
                valueChangedObserver.onAction(id);
        }
    }

    public class RemoteIntValue extends RemoteEntry implements SeekBar.OnSeekBarChangeListener {
        public static final int TYPE_INT = 3;
        final int min;
        final int max;
        int value;

        RemoteIntValue(int index, String value_name,
                       int int_min_value,
                       int int_max_value,
                       int int_current_value) {
            super(index, value_name);
            this.type = TYPE_INT;
            this.min = int_min_value;
            this.max = int_max_value;
            this.value = int_current_value;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            value = i;
            if (valueChangedObserver != null)
                valueChangedObserver.onIntValueChanged(id, value);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    }

    public class RemoteBooleanValue extends RemoteEntry implements CompoundButton.OnCheckedChangeListener {
        public static final int TYPE_BOOLEAN = 4;
        boolean value;

        RemoteBooleanValue(int index, String value_name,
                           boolean current_value) {
            super(index, value_name);
            this.type = TYPE_BOOLEAN;
            this.value = current_value;
        }

        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            this.value = b;
            if (valueChangedObserver != null)
                valueChangedObserver.onBooleanValueChanged(id, value);
        }
    }

    public List<RemoteEntry> pluginValues = new ArrayList<RemoteEntry>();
    public List<View> adapterData = new ArrayList<View>();
    private LayoutInflater inflater;
    private OnValueChanged valueChangedObserver = null;

    public void setOnValueChanged(OnValueChanged valueChangedObserver) {
        this.valueChangedObserver = valueChangedObserver;
    }

    public PluginValuesAdapter(Context context) {
        inflater = LayoutInflater.from(context);
    }

    public void addRemoteIntValue(int index, String value_name,
                                  int int_min_value,
                                  int int_max_value,
                                  int int_current_value) {
        pluginValues.add(new RemoteIntValue(index, value_name, int_min_value, int_max_value, int_current_value));
    }

    public void addRemoteBooleanValue(int index, String value_name,
                                      boolean current_value) {
        pluginValues.add(new RemoteBooleanValue(index, value_name, current_value));
    }

    public void addRemoteAction(int index, String value_name) {
        pluginValues.add(new RemoteAction(index, value_name));
    }

    public void addHeader(int index, String value_name) {
        pluginValues.add(new RemoteEntry(index, value_name, true));
    }

    public void updateValueBoolean(int index, boolean value) {
        RemoteEntry ra = pluginValues.get(index);
        if (ra instanceof RemoteBooleanValue) {
            ((RemoteBooleanValue) ra).value = value;
            notifyDataSetChanged();
        }
    }

    public void updateValueInt(int index, int value) {
        RemoteEntry ra = pluginValues.get(index);
        if (ra instanceof RemoteIntValue) {
            ((RemoteIntValue) ra).value = value;
            notifyDataSetChanged();
        }
    }

    public void addDataFinished() {
        adapterData.clear();
        boolean last_value_was_action = false;
        for (int i = 0; i < pluginValues.size(); ++i) {
            RemoteEntry entry = pluginValues.get(i);
            View addView = null;

            if (entry.type == RemoteEntry.TYPE_HEADER) {
                View plugin_value_row_view = inflater.inflate(R.layout.plugin_header, null);
                ((TextView) plugin_value_row_view.findViewById(R.id.headerTitle)).setText(entry.name);
                addView = plugin_value_row_view;
                last_value_was_action = false;

            } else if (entry.type == RemoteAction.TYPE_ACTION) {
//                RemoteAction value = (RemoteAction) entry;
//                FlowLayout buttonLayout;
//
//                // If last entry was an action, too, we add a button to the FlowLayout of the last entry.
//                if (last_value_was_action) {
//                    View plugin_value_row_view = adapterData.get(adapterData.size() - 1);
//                    buttonLayout = (FlowLayout) plugin_value_row_view.findViewById(R.id.remoteValueLayout);
//                } else {
//                    View plugin_value_row_view = inflater.inflate(R.layout.plugin_action, null);
//                    addView = plugin_value_row_view;
//                    buttonLayout = (FlowLayout) plugin_value_row_view.findViewById(R.id.remoteValueLayout);
//                }
//
//                Button s = new Button(inflater.getContext());
//                s.setText(entry.name);
//                s.setOnClickListener(value);
//                buttonLayout.addView(s);
//
//                if (last_value_was_action) {
//                    continue;
//                }
//                last_value_was_action = true;

            } else if (entry.type == RemoteBooleanValue.TYPE_BOOLEAN) {
                RemoteBooleanValue value = (RemoteBooleanValue) entry;
                View plugin_value_row_view = inflater.inflate(R.layout.create_scene_outlet_list_button, null);
                ((TextView) plugin_value_row_view.findViewById(R.id.titleText)).setText(entry.name);
                CheckBox s = (CheckBox) plugin_value_row_view.findViewById(R.id.remoteValue);
                s.setChecked(value.value);
                s.setOnCheckedChangeListener(value);
                addView = plugin_value_row_view;
                last_value_was_action = false;

            } else if (entry.type == RemoteIntValue.TYPE_INT) {
                RemoteIntValue value = (RemoteIntValue) entry;
                View plugin_value_row_view = inflater.inflate(R.layout.create_scene_outlet_list_ranged, null);
                ((TextView) plugin_value_row_view.findViewById(R.id.titleText)).setText(entry.name);
                SeekBar s = (SeekBar) plugin_value_row_view.findViewById(R.id.remoteValue);
                s.setMax(value.max);
                s.setProgress(value.value);
                s.setOnSeekBarChangeListener(value);
                addView = plugin_value_row_view;
                last_value_was_action = false;
            } else {
                continue;
            }

            adapterData.add(addView);
        }
        notifyDataSetChanged();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public Object getItem(int position) {
        return adapterData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return adapterData.get(position).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup container) {
        return adapterData.get(position);
    }

    @Override
    public int getCount() {
        return adapterData.size();
    }
}
