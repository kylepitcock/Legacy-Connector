package legacyconnector.v1_12_2.net.minecraft.command;

import legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer;

/**
 * {@code /replaceitem block|entity ... <slot> <item> [amount] [data]}: translated to the modern {@code /item replace}, with 1.12's slot names
 * ({@code slot.container.3}, {@code slot.weapon}, {@code slot.armor.head}...) mapped to the modern ones.
 */
public class CommandReplaceItem extends CommandBase {
    @Override
    public String getName() {
        return "replaceitem";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.replaceitem.usage";
    }

    /** The modern slot name of a 1.12 one. */
    static String mcc$slot(String slot) {
        String s = slot.startsWith("slot.") ? slot.substring(5) : slot;
        return switch (s) {
            case "weapon", "weapon.mainhand" -> "weapon.mainhand";
            case "weapon.offhand" -> "weapon.offhand";
            case "armor.chest" -> "armor.chest";
            case "armor.head", "armor.legs", "armor.feet" -> s;
            case "horse.saddle", "horse.armor", "horse.chest" -> s;
            default -> s;
        };
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 3) throw new WrongUsageException("commands.replaceitem.usage");
        var m = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (m == null) return;
        String command;
        int itemAt;
        if ("block".equals(args[0])) {
            if (args.length < 6) throw new WrongUsageException("commands.replaceitem.block.usage");
            command = "item replace block " + args[1] + " " + args[2] + " " + args[3] + " " + mcc$slot(args[4]) + " with ";
            itemAt = 5;
        } else if ("entity".equals(args[0])) {
            if (args.length < 4) throw new WrongUsageException("commands.replaceitem.entity.usage");
            command = "item replace entity " + args[1] + " " + mcc$slot(args[2]) + " with ";
            itemAt = 3;
        } else {
            throw new WrongUsageException("commands.replaceitem.usage");
        }
        String item = args[itemAt];
        String count = args.length > itemAt + 1 ? " " + args[itemAt + 1] : "";
        m.getCommands().performPrefixedCommand(m.createCommandSourceStack(), command + item + count);
    }
}
