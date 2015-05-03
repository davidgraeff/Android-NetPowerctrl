package oly.netpowerctrl.network;

/**
 * Implement this interface if you want to be informed if an execution finished
 */
public abstract class onExecutionFinished {
    protected int success = 0;
    protected int errors = 0;
    protected int expected = 0;

    public onExecutionFinished(int expected) {
        this.expected = expected;
    }

    /**
     * Increase the fail success by one. Also accumulate the all counter by the given argument @see #all_counter
     */
    public void addSuccess() {
        ++success;
        onExecutionProgress();
    }

    /**
     * Increase the fail counter by one. Also accumulate the all counter by the given argument @see #all_counter
     */
    public void addFail() {
        ++errors;
        onExecutionProgress();
    }

    public abstract void onExecutionProgress();

    public void addExpected(int value) {
        expected += value;
    }

    public void accumulateSuccessFail(int success, int failed) {
        this.success += success;
        this.errors += failed;
        onExecutionProgress();
    }
}
