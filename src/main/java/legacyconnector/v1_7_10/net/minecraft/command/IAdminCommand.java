package legacyconnector.v1_7_10.net.minecraft.command;

/** Announces the use of a command to the operators (vanilla's {@code IAdminCommand}, implemented by the server's configuration manager). */
public interface IAdminCommand {
    void func_152372_a(ICommandSender sender, ICommand command, int flags, String message, Object[] args);
}
