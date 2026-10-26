package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.command.CommandException;
import legacyconnector.v1_7_10.net.minecraft.command.ICommand;
import legacyconnector.v1_7_10.net.minecraft.command.ICommandSender;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.slf4j.LoggerFactory;

/**
 * Legacy {@code ICommand}s become Brigadier literals with one greedy string argument that is split on spaces,
 * which is exactly the {@code String[] args} the legacy command expects. Players who join afterwards receive
 * the updated command tree automatically.
 */
public final class CommandBridge1710 {
    private CommandBridge1710() {}

    public static void register(net.minecraft.server.MinecraftServer server, ICommand command) {
        var dispatcher = server.getCommands().getDispatcher();
        String name = command.getCommandName();
        dispatcher.register(build(name, command));
        List<?> aliases = command.getCommandAliases();
        if (aliases != null) for (Object alias : aliases) dispatcher.register(build(String.valueOf(alias), command));
        for (var p : server.getPlayerList().getPlayers()) server.getCommands().sendCommands(p);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> build(String name, ICommand command) {
        return Commands.literal(name)
                .requires(src -> command.canCommandSenderUseCommand(sender(src)))
                .executes(ctx -> run(ctx, command, new String[0]))
                .then(Commands.argument("args", StringArgumentType.greedyString())
                        .executes(ctx -> run(ctx, command, StringArgumentType.getString(ctx, "args").trim().split(" +"))));
    }

    private static int run(CommandContext<CommandSourceStack> ctx, ICommand command, String[] args) {
        ICommandSender sender = sender(ctx.getSource());
        try {
            // like its world generators' state, a mod's commands ran on the server thread (CustomOreGen reloads its options)
            var lock = org.kylepitcock.legacyconnector.core.LegacyModLocks.of(command);
            lock.lock();
            try {
                command.processCommand(sender, args);
            } finally {
                lock.unlock();
            }
            return 1;
        } catch (CommandException e) {
            ctx.getSource().sendFailure(Component.translatable(e.getMessage(), e.getErrorOjbects()));
            return 0;
        } catch (RuntimeException e) {
            LoggerFactory.getLogger("LegacyConnector/Commands").error("Legacy command /{} failed", command.getCommandName(), e);
            ctx.getSource().sendFailure(Component.literal("An error occurred while executing legacy command: " + e));
            return 0;
        }
    }

    static ICommandSender sender(CommandSourceStack source) {
        if (source.getEntity() != null) {
            var view = EntityBridge.wrap(source.getEntity());
            if (view instanceof ICommandSender s) return s;
        }
        return MinecraftServer.mcc$wrap(source.getServer());
    }

    static boolean isPlayer(ICommandSender sender) {
        return sender instanceof EntityPlayer;
    }
}
