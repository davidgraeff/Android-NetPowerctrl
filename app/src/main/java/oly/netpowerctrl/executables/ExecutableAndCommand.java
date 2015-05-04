package oly.netpowerctrl.executables;

/**
 * Created by david on 21.04.15.
 */
public class ExecutableAndCommand {
    public final Executable executable;
    public final Integer command;

    public ExecutableAndCommand(Executable executable, Integer command) {
        this.executable = executable;
        this.command = command;
    }
}
