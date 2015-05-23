package oly.netpowerctrl.executables;

/**
 * Created by david on 21.04.15.
 */
public class ExecutableAndCommand {
    public static final int OFF = 0;
    public static final int ON = 1;
    public static final int TOGGLE = -1;
    public static final int INVALID = -2;

    public final Executable executable;
    public final Integer command;

    public ExecutableAndCommand(Executable executable, Integer command) {
        this.executable = executable;
        this.command = command;
    }
}
