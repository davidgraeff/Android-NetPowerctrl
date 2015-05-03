package oly.netpowerctrl.executables;

/**
 * Created by david on 21.04.15.
 */
public class ExecutableAndCommand {
    public final Executable port;
    public final Integer command;

    public ExecutableAndCommand(Executable port, Integer command) {
        this.port = port;
        this.command = command;
    }
}
