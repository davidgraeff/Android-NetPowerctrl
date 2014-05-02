package oly.netpowerctrl.anel;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.datastructure.DeviceInfo;

/**
 * For displaying DeviceInfo fields in a ListView
 */
class DeviceConfigurationAdapter extends BaseAdapter implements View.OnClickListener {
    public static class ReviewItem {

        public final String mTitle;
        public String mDisplayValueString;
        public int mDisplayValueInt;
        public boolean mDisplayValueBoolean;
        public final int mPageKey;
        public int type = 0; // o string, 1 int, 2 boolean
        public boolean mEnabled;

        public ReviewItem(String title, String displayValue, int pageKey) {
            mTitle = title;
            mDisplayValueString = displayValue;
            mPageKey = pageKey;
            mEnabled = true;
            type = 0;
        }

        public ReviewItem(String title, int displayValue, int pageKey, boolean enabled) {
            mTitle = title;
            mDisplayValueInt = displayValue;
            mPageKey = pageKey;
            mEnabled = enabled;
            type = 1;
        }

        public ReviewItem(String title, boolean displayValue) {
            mTitle = title;
            mDisplayValueBoolean = displayValue;
            mPageKey = DeviceConfigurationAdapter.DefaultPorts;
            mEnabled = true;
            type = 2;
        }
    }

    private final List<ReviewItem> deviceConfigurationOptions = new ArrayList<ReviewItem>();
    private final Context context;
    private final DeviceInfo deviceInfo;
    private final LayoutInflater inflater;

    private static final int DeviceName = 0;
    private static final int HostName = 1;
    private static final int UserName = 2;
    private static final int Password = 3;
    private static final int DefaultPorts = 4;
    private static final int ReceivePort = 5;
    private static final int SendPort = 6;

    public DeviceConfigurationAdapter(Context context, DeviceInfo deviceInfo) {
        this.context = context;
        this.deviceInfo = deviceInfo;
        inflater = LayoutInflater.from(context);
        deviceConfigurationOptions.add(new ReviewItem(context.getString(R.string.device_name), deviceInfo.DeviceName, DeviceName));
        deviceConfigurationOptions.add(new ReviewItem(context.getString(R.string.device_ip), deviceInfo.HostName, HostName));
        deviceConfigurationOptions.add(new ReviewItem(context.getString(R.string.device_username), deviceInfo.UserName, UserName));
        deviceConfigurationOptions.add(new ReviewItem(context.getString(R.string.device_password), deviceInfo.Password, Password));
        deviceConfigurationOptions.add(new ReviewItem(context.getString(R.string.device_default_ports), deviceInfo.DefaultPorts));
        deviceConfigurationOptions.add(new ReviewItem(context.getString(R.string.device_recv_udp), deviceInfo.ReceivePort, ReceivePort, !deviceInfo.DefaultPorts));
        deviceConfigurationOptions.add(new ReviewItem(context.getString(R.string.device_send_udp), deviceInfo.SendPort, SendPort, !deviceInfo.DefaultPorts));
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
        return deviceConfigurationOptions.get(position);
    }

    @Override
    public long getItemId(int position) {
        return deviceConfigurationOptions.get(position).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup container) {
        if (convertView == null)
            convertView = inflater.inflate(R.layout.device_configuration_item, null);

        ReviewItem reviewItem = deviceConfigurationOptions.get(position);
        assert convertView != null;
        ((TextView) convertView.findViewById(R.id.titleText)).setText(reviewItem.mTitle);
        TextView text = (TextView) convertView.findViewById(R.id.textOption);
        CheckBox checkbox = (CheckBox) convertView.findViewById(R.id.checkboxOption);

        if (reviewItem.type == 0) {
            text.setVisibility(View.VISIBLE);
            checkbox.setVisibility(View.GONE);
            text.setEnabled(reviewItem.mEnabled);
            text.setTag(reviewItem.mPageKey);
            text.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            text.setText(reviewItem.mDisplayValueString);
            text.setOnClickListener(this);
        } else if (reviewItem.type == 1) {
            text.setVisibility(View.VISIBLE);
            checkbox.setVisibility(View.GONE);
            text.setEnabled(reviewItem.mEnabled);
            text.setTag(reviewItem.mPageKey);
            text.setInputType(InputType.TYPE_CLASS_NUMBER);
            text.setText(Integer.valueOf(reviewItem.mDisplayValueInt).toString());
            text.setOnClickListener(this);
        } else if (reviewItem.type == 2) {
            text.setVisibility(View.GONE);
            checkbox.setVisibility(View.VISIBLE);
            checkbox.setEnabled(reviewItem.mEnabled);
            checkbox.setTag(reviewItem.mPageKey);
            checkbox.setChecked(reviewItem.mDisplayValueBoolean);
            checkbox.setOnClickListener(this);
        }
        return convertView;
    }

    @Override
    public int getCount() {
        return deviceConfigurationOptions.size();
    }

    @Override
    public void onClick(final View view) {
        final int key = (Integer) view.getTag();
        final ReviewItem item = deviceConfigurationOptions.get(key);

        if (key == DefaultPorts) {
            CheckBox chk = (CheckBox) view;
            deviceInfo.DefaultPorts = chk.isChecked();
            deviceConfigurationOptions.get(ReceivePort).mEnabled = !deviceInfo.DefaultPorts;
            deviceConfigurationOptions.get(SendPort).mEnabled = !deviceInfo.DefaultPorts;
            item.mDisplayValueBoolean = deviceInfo.DefaultPorts;
            notifyDataSetChanged();
            return;
        }

        EditText textView = new EditText(context);
        textView.setText(((TextView) view).getText());
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(textView);

        @SuppressWarnings("ConstantConditions")
        final String text = textView.getText().toString();

        switch (key) {
            case DeviceName:
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        item.mDisplayValueString = text;
                        deviceInfo.DeviceName = item.mDisplayValueString;
                        notifyDataSetChanged();
                    }
                });
                break;
            case HostName:
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        item.mDisplayValueString = text;
                        deviceInfo.HostName = item.mDisplayValueString;
                        notifyDataSetChanged();
                    }
                });
                break;
            case UserName:
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        item.mDisplayValueString = text;
                        deviceInfo.UserName = item.mDisplayValueString;
                        notifyDataSetChanged();
                    }
                });
                break;
            case Password:
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        item.mDisplayValueString = text;
                        deviceInfo.Password = item.mDisplayValueString;
                        notifyDataSetChanged();
                    }
                });
                break;
            case ReceivePort:
                textView.setInputType(InputType.TYPE_CLASS_NUMBER);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        item.mDisplayValueInt = Integer.valueOf(text);
                        deviceInfo.ReceivePort = item.mDisplayValueInt;
                        notifyDataSetChanged();
                    }
                });
                break;
            case SendPort:
                textView.setInputType(InputType.TYPE_CLASS_NUMBER);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        item.mDisplayValueInt = Integer.valueOf(text);
                        deviceInfo.SendPort = item.mDisplayValueInt;
                        notifyDataSetChanged();
                    }
                });
                break;
        }
        builder.create().show();
    }
}
