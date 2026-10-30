package legacyconnector.v1_12_2.net.minecraft.command;

import legacyconnector.v1_12_2.net.minecraft.advancements.Advancement;
import legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** {@code /advancement}: run as the modern command; {@link #findAdvancement} looks one up by id for mods' own commands. */
public class AdvancementCommand extends CommandBase {
    @Override
    public String getName() {
        return "advancement";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.advancement.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        var m = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (m == null) return;
        m.getCommands().performPrefixedCommand(legacyconnector.v1_12_2.net.minecraft.advancements.FunctionManager.mcc$source(sender),
                "advancement " + String.join(" ", args));
    }

    /** The advancement of that id, or a command error naming it. */
    public static Advancement findAdvancement(MinecraftServer server, String id) throws CommandException {
        Advancement advancement = server.getAdvancementManager().getAdvancement(new ResourceLocation(id));
        if (advancement == null) throw new CommandException("commands.advancement.advancementNotFound", id);
        return advancement;
    }
}
