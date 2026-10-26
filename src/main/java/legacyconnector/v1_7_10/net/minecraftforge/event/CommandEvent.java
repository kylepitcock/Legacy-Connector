package legacyconnector.v1_7_10.net.minecraftforge.event;

import legacyconnector.v1_7_10.net.minecraft.command.ICommand;
import legacyconnector.v1_7_10.net.minecraft.command.ICommandSender;

/** A command is about to run; cancel it to stop it (set {@link #exception} to report why). */
@legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable
public class CommandEvent extends legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event {
    public final ICommand command;
    public final ICommandSender sender;
    public String[] parameters;
    public Throwable exception;

    public CommandEvent(ICommand command, ICommandSender sender, String[] parameters) {
        this.command = command;
        this.sender = sender;
        this.parameters = parameters;
    }
}
