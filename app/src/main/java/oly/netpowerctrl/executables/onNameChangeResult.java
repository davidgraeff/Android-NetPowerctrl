package oly.netpowerctrl.executables;

/**
 * Implement this to get informed if a asynchronous name change action succeded or failed.
 */
public interface onNameChangeResult {
    void onNameChangeResult(boolean success, String error_message);
}
