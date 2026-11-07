package legacyconnector.v1_6_4.facade.net.minecraft.command;

import legacyconnector.v1_7_10.net.minecraft.command.ICommandSender;

/** Facade of the 1.7.10 shim's {@code CommandBase} for 1.6.4 mods. Never instantiated. */
public final class CommandBase {
    private CommandBase() {}

    /** {@code notifyAdmins(sender, key, args)}: the other operators see the command's result, and so does the sender (1.7.10's {@code notifyOperators}). */
    public static void notifyAdmins(ICommandSender sender, String message, Object... args) {
        legacyconnector.v1_7_10.net.minecraft.command.CommandBase.notifyOperators(sender, null, 0, message, args);
    }
}
