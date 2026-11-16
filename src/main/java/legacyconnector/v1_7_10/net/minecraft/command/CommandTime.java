package legacyconnector.v1_7_10.net.minecraft.command;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer;
import legacyconnector.v1_7_10.net.minecraft.world.WorldServer;

/** {@code /time set <value|day|night>} and {@code /time add <value>} (1.7.10 {@code CommandTime}); mods subclass it or register it under another name. */
public class CommandTime extends CommandBase {
    public CommandTime() {
    }

    @Override
    public String getCommandName() {
        return "time";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "commands.time.usage";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length > 1) {
            if (args[0].equals("set")) {
                int time;
                if (args[1].equals("day")) time = 0;
                else if (args[1].equals("night")) time = 12500;
                else time = parseIntWithMin(sender, args[1], 0);
                func_71552_a(sender, time);
                func_152373_a(sender, this, "commands.time.set", time);
                return;
            }
            if (args[0].equals("add")) {
                int time = parseIntWithMin(sender, args[1], 0);
                func_71553_b(sender, time);
                func_152373_a(sender, this, "commands.time.added", time);
                return;
            }
        }
        throw new WrongUsageException("commands.time.usage");
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, "set", "add");
        return args.length == 2 && args[0].equals("set") ? getListOfStringsMatchingLastWord(args, "day", "night") : null;
    }

    /** Sets the time of every world. */
    protected void func_71552_a(ICommandSender sender, int time) {
        WorldServer[] worlds = MinecraftServer.mcc$get$worldServers(MinecraftServer.getServer());
        for (WorldServer world : worlds) world.setWorldTime(time);
    }

    /** Advances the time of every world. */
    protected void func_71553_b(ICommandSender sender, int time) {
        WorldServer[] worlds = MinecraftServer.mcc$get$worldServers(MinecraftServer.getServer());
        for (WorldServer world : worlds) world.setWorldTime(world.getWorldTime() + time);
    }
}
