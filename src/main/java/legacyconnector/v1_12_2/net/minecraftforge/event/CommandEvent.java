package legacyconnector.v1_12_2.net.minecraftforge.event;

import legacyconnector.v1_12_2.net.minecraft.command.ICommand;
import legacyconnector.v1_12_2.net.minecraft.command.ICommandSender;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Posted before a 1.12.2 mod's command runs (cancel to stop it; set an exception to report one). The layer posts it for the
 * legacy commands it registered; modern commands go through the modern command event.
 */
@Cancelable
public class CommandEvent extends Event {
    private final ICommand command;
    private final ICommandSender sender;
    private String[] parameters;
    private Throwable exception;

    public CommandEvent(ICommand command, ICommandSender sender, String[] parameters) {
        this.command = command;
        this.sender = sender;
        this.setParameters(parameters);
    }

    public ICommand getCommand() {
        return command;
    }

    public ICommandSender getSender() {
        return sender;
    }

    public String[] getParameters() {
        return parameters;
    }

    public void setParameters(String[] parameters) {
        this.parameters = parameters;
    }

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }
}
