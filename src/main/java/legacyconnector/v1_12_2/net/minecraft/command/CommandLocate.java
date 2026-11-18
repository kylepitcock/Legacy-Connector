package legacyconnector.v1_12_2.net.minecraft.command;

import legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer;

/** {@code /locate <structure>}: 1.12's structure names are mapped to the structures (or structure tags) of the modern {@code /locate}. */
public class CommandLocate extends CommandBase {
    @Override
    public String getName() {
        return "locate";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.locate.usage";
    }

    /** The modern structure (or {@code #tag}) of a 1.12 structure name, or the name itself for the ones the game already knows. */
    static String mcc$structure(String name) {
        return switch (name.toLowerCase(java.util.Locale.ROOT)) {
            case "village" -> "#minecraft:village";
            case "mineshaft" -> "#minecraft:mineshaft";
            case "stronghold" -> "minecraft:stronghold";
            case "temple" -> "#minecraft:ruined_portal";
            case "monument" -> "minecraft:ocean_monument";
            case "mansion" -> "minecraft:mansion";
            case "endcity" -> "minecraft:end_city";
            case "fortress" -> "minecraft:fortress";
            default -> name.contains(":") || name.startsWith("#") ? name : "minecraft:" + name.toLowerCase(java.util.Locale.ROOT);
        };
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 1) throw new WrongUsageException("commands.locate.usage");
        var m = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (m == null) return;
        m.getCommands().performPrefixedCommand(m.createCommandSourceStack(), "locate structure " + mcc$structure(args[0]));
    }

    @Override
    public java.util.List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos targetPos) {
        return args.length == 1 ? getListOfStringsMatchingLastWord(args, "Stronghold", "Monument", "Village", "Mansion", "EndCity", "Fortress", "Temple", "Mineshaft")
                : java.util.Collections.emptyList();
    }
}
