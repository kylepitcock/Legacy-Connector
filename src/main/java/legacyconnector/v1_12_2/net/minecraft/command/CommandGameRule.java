package legacyconnector.v1_12_2.net.minecraft.command;

import java.util.Collections;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.GameRules;

/** {@code /gamerule}: run as the modern command; {@link #notifyGameRuleChange} tells clients about rules they display. */
public class CommandGameRule extends CommandBase {
    @Override
    public String getName() {
        return "gamerule";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.gamerule.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        var m = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (m == null) return;
        m.getCommands().performPrefixedCommand(m.createCommandSourceStack(), "gamerule " + String.join(" ", args));
    }

    /** After a rule changed: {@code reducedDebugInfo} is sent to every player (their debug screen shows less or more). */
    public static void notifyGameRuleChange(GameRules rules, String rule, MinecraftServer server) {
        if (!"reducedDebugInfo".equals(rule) || rules == null) return;
        var m = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (m == null) return;
        byte status = (byte) (rules.getBoolean(rule) ? 22 : 23);
        for (var player : m.getPlayerList().getPlayers()) {
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundEntityEventPacket(player, status));
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
        if (args.length == 1) {
            var m = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (m == null) return Collections.emptyList();
            return getListOfStringsMatchingLastWord(args, GameRules.mcc$of(m.getGameRules(), m).getRules());
        }
        if (args.length == 2) return getListOfStringsMatchingLastWord(args, "true", "false");
        return Collections.emptyList();
    }
}
