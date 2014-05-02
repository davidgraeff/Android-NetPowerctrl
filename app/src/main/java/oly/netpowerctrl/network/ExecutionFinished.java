package oly.netpowerctrl.network;

/**
 * Implement this interface if you want to be informed if an execution finished
 */
public interface ExecutionFinished {
    public void onExecutionFinished(int commands);
}
