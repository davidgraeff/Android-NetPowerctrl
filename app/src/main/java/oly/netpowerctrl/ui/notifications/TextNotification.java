package oly.netpowerctrl.ui.notifications;

import android.app.Activity;

/**
 * Text notification
 */
public class TextNotification extends PermanentNotification {
    private final boolean isClosable;
    private String text;

    public TextNotification(Activity activity, String id, String text, boolean isClosable) {
        super(id, activity);
        this.text = text;
        this.isClosable = isClosable;
    }

    @Override
    public boolean hasCloseButton() {
        return isClosable;
    }

    @Override
    public void onDismiss() {

    }

    @Override
    public String getText() {
        return text;
    }
}
