package oly.netpowerctrl.ui.notifications;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import oly.netpowerctrl.R;
import oly.netpowerctrl.preferences.SharedPrefs;

/**
 * Created by david on 19.09.14.
 */
public class TextNotification extends PermanentNotification {
    private final boolean isClosable;
    private String text;

    public TextNotification(String id, String text, boolean isClosable) {
        super(id);
        this.text = text;
        this.isClosable = isClosable;
    }

    @Override
    public View getView(final Activity context, ViewGroup parent) {
        View v = context.getLayoutInflater().inflate(R.layout.notification_text, parent, false);
        TextView title = (TextView) v.findViewById(R.id.notification_title);
        title.setText(text);

        ImageButton closeBtn = (ImageButton) v.findViewById(R.id.notification_close);
        closeBtn.setVisibility(isClosable ? View.VISIBLE : View.GONE);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPrefs.getInstance().acceptUpdatedVersion();
                InAppNotifications.closePermanentNotification(context, getID());
            }
        });

        return v;
    }
}
