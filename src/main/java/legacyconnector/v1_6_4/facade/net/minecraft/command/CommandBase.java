package legacyconnector.v1_6_4.facade.net.minecraft.command;

import legacyconnector.v1_7_10.net.minecraft.command.ICommandSender;

/** Facade of the 1.7.10 shim's {@code CommandBase} for 1.6.4 mods. Never instantiated. */
public final class CommandBase {
    private CommandBase() {}

    /** {@code notifyAdmins(sender, key, args)}: the other operators see the command's result, and so does the sender (1.7.10's {@code notifyOperators}). */
    public static void notifyAdmins(ICommandSender sender, String message, Object... args) {
        legacyconnector.v1_7_10.net.minecraft.command.CommandBase.notifyOperators(sender, null, 0, message, args);
    }

    /** 1.6.4 {@code notifyAdmins(sender, flags, key, args)}: the form with the 1.6.4 flag word (bit 0 hid the message from the sender); the flags are not kept. */
    public static void notifyAdmins(ICommandSender sender, int flags, String message, Object... args) {
        legacyconnector.v1_7_10.net.minecraft.command.CommandBase.notifyOperators(sender, null, flags, message, args);
    }

    /** 1.6.4 {@code func_82361_a(sender, args, start, selectors)}: the arguments from {@code start} joined into one string (player selectors are resolved as the 1.7.10 form does). */
    public static String func_82361_a(ICommandSender sender, String[] args, int start, boolean selectors) {
        return legacyconnector.v1_7_10.net.minecraft.command.CommandBase.func_82360_a(sender, args, start);
    }

    /** 1.6.4 {@code func_110663_b(collection)}: the elements joined as "a, b and c". */
    public static String func_110663_b(java.util.Collection<?> collection) {
        return legacyconnector.v1_7_10.net.minecraft.command.CommandBase.joinNiceStringFromCollection(collection);
    }
}
