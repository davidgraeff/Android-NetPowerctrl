package oly.netpowerctrl.network;

/**
 * Implement this interface if you want to be informed if an execution finished
 */
public interface onExecutionFinished {
    public void onExecutionProgress(int current, int all);
}
