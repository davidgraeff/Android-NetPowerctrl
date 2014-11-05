package oly.netpowerctrl.ui.notifications;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.ui.ChangeLogUtil;

/**
 * Created by david on 19.09.14.
 */
public class ChangeLogNotification extends PermanentNotification {
    public ChangeLogNotification() {
        super("changelog");
    }

    @Override
    public View getView(final Activity context, ViewGroup parent) {
        View v = context.getLayoutInflater().inflate(R.layout.notification_changelog, parent, false);
        TextView title = (TextView) v.findViewById(R.id.notification_title);
        title.setText(context.getString(R.string.notification_title_changelog, SharedPrefs.getVersionName(context)));

        ImageButton closeBtn = (ImageButton) v.findViewById(R.id.notification_close);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPrefs.getInstance().acceptUpdatedVersion();
                InAppNotifications.closePermanentNotification(context, getID());
            }
        });

        Button btn = (Button) v.findViewById(R.id.btnShowChangelog);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPrefs.getInstance().acceptUpdatedVersion();
                InAppNotifications.closePermanentNotification(context, getID());
                ChangeLogUtil.showChangeLog(context);
            }
        });

        return v;
    }
}
